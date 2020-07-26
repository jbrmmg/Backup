package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.ActionConfirm;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.dataaccess.ActionConfirmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
public class ActionManager {
    private static final Logger LOG = LoggerFactory.getLogger(ActionManager.class);

    private final ApplicationProperties applicationProperties;
    private final ActionConfirmRepository actionConfirmRepository;
    private final ResourceLoader resourceLoader;

    private static final String END_TD = "</td>";

    @Autowired
    public ActionManager(ApplicationProperties applicationProperties,
                         ActionConfirmRepository actionConfirmRepository,
                         ResourceLoader resourceLoader) {
        this.applicationProperties = applicationProperties;
        this.actionConfirmRepository = actionConfirmRepository;
        this.resourceLoader = resourceLoader;
    }

    boolean checkAction(FileInfo fileInfo, String action) {
        List<ActionConfirm> confirmedActions = actionConfirmRepository.findByFileInfoAndAction(fileInfo,action);

        if(!confirmedActions.isEmpty()) {
            boolean confirmed = false;
            for(ActionConfirm nextConfirm: confirmedActions) {
                if(nextConfirm.confirmed()) {
                    confirmed = true;
                }
            }

            if(confirmed) {
                for(ActionConfirm nextConfirm: confirmedActions) {
                    actionConfirmRepository.delete(nextConfirm);
                }

                return true;
            }
        } else {
            // Create an action to be confirmed.
            ActionConfirm actionConfirm = new ActionConfirm();
            actionConfirm.setFileInfo(fileInfo);
            actionConfirm.setAction(action);
            actionConfirm.setConfirmed(false);
            actionConfirm.setParameterRequired(false);

            actionConfirmRepository.save(actionConfirm);
        }

        return false;
    }

    void deleteFileIfConfirmed(FileInfo fileInfo) {
        File file = new File(fileInfo.getFullFilename());

        if(file.exists() && checkAction(fileInfo, "DELETE")) {
            LOG.info("Delete the file - {}", file );
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                LOG.warn("Failed to delete file {}", file);
            }
        }
    }

    public void sendActionEmail() {
        try {
            // Only send the email if its enabled.
            if (Boolean.FALSE.equals(applicationProperties.getEmail().getEnabled())) {
                LOG.warn("Email disabled, not sending.");
                return;
            }

            // Get a list of unconfirmed actions.
            List<ActionConfirm> unconfirmedActions = actionConfirmRepository.findByConfirmed(false);

            if (unconfirmedActions.isEmpty()) {
                LOG.info("No unconfirmed actions.");
                return;
            }

            // Build the list of details.
            StringBuilder emailText = new StringBuilder();
            for (ActionConfirm nextAction : unconfirmedActions) {
                emailText.append("<tr>");
                emailText.append("<td class=\"action\">");
                emailText.append(nextAction.getAction());
                emailText.append(END_TD);
                emailText.append("<td class=\"parameter\">");
                emailText.append(nextAction.getParameter() == null ? "" : nextAction.getParameter());
                emailText.append(END_TD);
                emailText.append("<td class=\"filename\">");
                emailText.append(nextAction.getPath().getFullFilename());
                emailText.append(END_TD);
                emailText.append("</tr>");
            }

            // Get the email template.
            Resource resource = resourceLoader.getResource("classpath:html/email.html");
            InputStream is = resource.getInputStream();

            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(isr);

            String template = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            // Send the email.
            LOG.info("Sending the actions email.");
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", applicationProperties.getEmail().getAuthenticate().toString());
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", applicationProperties.getEmail().getHost());
            properties.put("mail.smtp.port", applicationProperties.getEmail().getPort().toString());

            Session session = Session.getInstance(properties,
                    new javax.mail.Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(applicationProperties.getEmail().getUser(), applicationProperties.getEmail().getPassword());
                        }
                    });

            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(applicationProperties.getEmail().getFrom()));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(applicationProperties.getEmail().getTo()));
            message.setSubject("Backup actions.");

            message.setContent(template.replace("<!-- TABLEROWS -->", emailText.toString()), "text/html");

            Transport.send(message);
        } catch (Exception ex) {
            LOG.error("Failed to send email ", ex);
        }
    }
}
