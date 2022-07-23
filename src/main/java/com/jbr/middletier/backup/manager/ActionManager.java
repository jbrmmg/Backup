package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.ActionConfirmRepository;
import com.jbr.middletier.backup.dto.ActionConfirmDTO;
import com.jbr.middletier.backup.dto.ProcessResultDTO;
import com.jbr.middletier.backup.exception.ActionNotFoundException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
public class ActionManager {
    private static final Logger LOG = LoggerFactory.getLogger(ActionManager.class);

    private final ApplicationProperties applicationProperties;
    private final ActionConfirmRepository actionConfirmRepository;
    private final ResourceLoader resourceLoader;
    private final FileSystemObjectManager fileSystemObjectManager;
    private final AssociatedFileDataManager associatedFileDataManager;
    private final FileSystem fileSystem;

    private static final String END_TD = "</td>";

    @Autowired
    public ActionManager(ApplicationProperties applicationProperties,
                         ActionConfirmRepository actionConfirmRepository,
                         ResourceLoader resourceLoader,
                         FileSystemObjectManager fileSystemObjectManager,
                         AssociatedFileDataManager associatedFileDataManager,
                         FileSystem fileSystem) {
        this.applicationProperties = applicationProperties;
        this.actionConfirmRepository = actionConfirmRepository;
        this.resourceLoader = resourceLoader;
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.associatedFileDataManager = associatedFileDataManager;
        this.fileSystem = fileSystem;
    }

    public List<ActionConfirmDTO> externalFindByConfirmed(boolean confirmed) {
        List<ActionConfirmDTO> result = new ArrayList<>();

        this.actionConfirmRepository.findByConfirmed(confirmed).forEach(action -> result.add(new ActionConfirmDTO(action)));

        return result;
    }

    public ActionConfirmDTO confirmAction(ConfirmActionRequest request) {
        // Is this a valid action?
        Optional<ActionConfirm> existingAction = actionConfirmRepository.findById(request.getId());

        if(!existingAction.isPresent()) {
            throw new ActionNotFoundException(request.getId());
        }

        // What type is this?
        if(ActionConfirmType.AC_IMPORT.equals(existingAction.get().getAction()) || Boolean.TRUE.equals(request.getConfirm())) {
            // For import, always confirm the action.
            existingAction.get().setConfirmed(true);
            existingAction.get().setParameter(request.getParameter());

            actionConfirmRepository.save(existingAction.get());
        } else {
            actionConfirmRepository.deleteById(request.getId());
        }

        return new ActionConfirmDTO(existingAction.get());
    }

    private ActionConfirmDTO createAction(ActionConfirmType type, FileInfo file, String flags) {
        ActionConfirm actionConfirm = new ActionConfirm();
        actionConfirm.setFileInfo(file);
        actionConfirm.setAction(type);
        actionConfirm.setConfirmed(false);
        switch(type) {
            case AC_DELETE_DUPLICATE:
            case AC_DELETE:
                actionConfirm.setParameterRequired(false);
                break;

            case AC_IMPORT:
                actionConfirm.setParameterRequired(true);
                actionConfirm.setFlags(flags);
                break;
        }

        return new ActionConfirmDTO(actionConfirmRepository.save(actionConfirm));
    }

    public ActionConfirmDTO createFileDeleteAction(FileInfo file) {
        return createAction(ActionConfirmType.AC_DELETE, file, null);
    }

    @SuppressWarnings("UnusedReturnValue")
    public ActionConfirmDTO createFileImportAction(FileInfo file, String flags) {
        return createAction(ActionConfirmType.AC_IMPORT, file, flags);
    }

    public ActionConfirmDTO createFileDeleteDuplicateAction(FileInfo file) {
        return createAction(ActionConfirmType.AC_DELETE_DUPLICATE, file, null);
    }

    public void actionPerformed(ActionConfirm action) {
        actionConfirmRepository.delete(action);
    }

    public List<ActionConfirm> findConfirmedDeletes() {
        return actionConfirmRepository.findByConfirmedAndAction(true,ActionConfirmType.AC_DELETE.getTypeName());
    }

    public void clearDuplicateActions() {
        actionConfirmRepository.clearActions(ActionConfirmType.AC_DELETE_DUPLICATE.getTypeName(), false);
    }

    public void clearImportActions() {
        actionConfirmRepository.clearActions(ActionConfirmType.AC_IMPORT.getTypeName(), false);
    }

    public void deleteActions(List<ActionConfirm> actions) {
        actionConfirmRepository.deleteAll(actions);
    }

    public List<ActionConfirm> getActionsForFile(FileInfo file) {
        return actionConfirmRepository.findByFileInfoAndAction(file,ActionConfirmType.AC_IMPORT.getTypeName());
    }

    boolean checkAction(FileInfo fileInfo, ActionConfirmType action) {
        List<ActionConfirm> confirmedActions = actionConfirmRepository.findByFileInfoAndAction(fileInfo,action.getTypeName());

        if(!confirmedActions.isEmpty()) {
            boolean confirmed = false;
            for(ActionConfirm nextConfirm: confirmedActions) {
                if(nextConfirm.confirmed()) {
                    confirmed = true;
                }
            }

            if(confirmed) {
                actionConfirmRepository.deleteAll(confirmedActions);
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

    void deleteFileIfConfirmed(FileInfo fileInfo, ProcessResultDTO processResult) {
        if(checkAction(fileInfo, ActionConfirmType.AC_DELETE)) {
            fileSystem.deleteFile(fileSystemObjectManager.getFile(fileInfo), processResult);
        }
    }

    public void deleteAllActions() {
        actionConfirmRepository.deleteAll();
    }

    private boolean sourcesMounted() {
        for(Source nextSource : associatedFileDataManager.findAllSource()) {
            if((nextSource.getIdAndType().getType() == FileSystemObjectType.FSO_SOURCE)
                    && (!fileSystem.validateMountCheck(nextSource.getMountCheck()))) {
                return false;
            }
        }

        return true;
    }

    public void sendActionEmail() {
        try {
            // Only send the email if its enabled.
            if (Boolean.FALSE.equals(applicationProperties.getEmail().getEnabled())) {
                LOG.warn("Email disabled, not sending.");
                return;
            }

            // Check that all the sources are mounted OK.
            String subject = "Backup actions.";
            if(!sourcesMounted()) {
                subject += " (Check Sources Mounted)";
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
                emailText.append(fileSystemObjectManager.getFile(nextAction.getPath()).toString());
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
            properties.put("mail.smtp.starttls.required", "true");
            properties.put("mail.smtp.ssl.protocols", "TLSv1.2");

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
            message.setSubject(subject);

            message.setContent(template.replace("<!-- TABLEROWS -->", emailText.toString()), "text/html");

            Transport.send(message);
        } catch (Exception ex) {
            LOG.error("Failed to send email ", ex);
        }
    }
}
