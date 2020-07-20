package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class BackupManager {
    private static final Logger LOG = LoggerFactory.getLogger(BackupManager.class);

    private final ApplicationProperties applicationProperties;
    private final RestTemplateBuilder restTemplateBuilder;

    @PostConstruct
    public void initialise() {
        postWebLog(webLogLevel.INFO, applicationProperties.getServiceName() + " starting up.");
    }

    public BackupManager(ApplicationProperties applicationProperties,
                         RestTemplateBuilder restTemplateBuilder) {
        this.applicationProperties = applicationProperties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    public String todaysDirectory() {
        DateFormat formatter = new SimpleDateFormat(this.applicationProperties.getDirectory().getDateFormat());
        Calendar calendar = Calendar.getInstance();

        return String.format("%s/%s/",this.applicationProperties.getDirectory().getName(),formatter.format(calendar.getTime()));
    }

    public void initialiseDay() throws IOException {
        LOG.info("Initialise the backup directory.");

        Path directoryPath = Paths.get(this.applicationProperties.getDirectory().getName());

        // Does the directory exist?
        if(Files.notExists(directoryPath)) {
            throw new IllegalStateException(String.format("The defined directory path %s does not exist.", this.applicationProperties.getDirectory().getName()));
        }

        // What should today's directory be called?
        Path todaysDirectoryPath = Paths.get(todaysDirectory());

        // If not exists, create it.
        if(Files.notExists(todaysDirectoryPath)) {
            Files.createDirectory(todaysDirectoryPath);
            postWebLog(webLogLevel.INFO,"Created directory + " + todaysDirectoryPath);
        }
    }

    public enum webLogLevel { DEBUG, INFO, WARN, ERROR }

    public void postWebLog(webLogLevel level, String message) {
        try {
            // Only perform if there is a web log URL.
            if(applicationProperties.getWebLogUrl() == null || applicationProperties.getWebLogUrl().length() == 0) {
                return;
            }

            RestTemplate restTemplate = this.restTemplateBuilder.build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            StringBuilder requestJson = new StringBuilder();

            requestJson.append("{");

            requestJson.append("\"levelString\": \"");
            switch (level) {
                case DEBUG:
                    requestJson.append("DEBUG");
                    break;
                case INFO:
                    requestJson.append("INFO");
                    break;
                case WARN:
                    requestJson.append("WARN");
                    break;
                case ERROR:
                    requestJson.append("ERROR");
                    break;
            }
            requestJson.append("\",");

            requestJson.append("\"formattedMessage\": \"");
            requestJson.append(message);
            requestJson.append("\",");

            requestJson.append("\"callerFilename\": \"BackupManager.java\",");
            requestJson.append("\"callerLine\": \"0\",");
            requestJson.append("\"callerMethod\": \"postWebLog\",");
            requestJson.append("\"loggerName\": \"Backup Logger\",");

            requestJson.append("\"callerClass\": \"com.jbr.middletier.backup.manager\"");
            requestJson.append("}");

            HttpEntity<String> request = new HttpEntity<>(requestJson.toString(), headers);

            restTemplate.postForEntity(applicationProperties.getWebLogUrl(), request,String.class);
        } catch(Exception ex) {
            LOG.warn("Unable to post to web log.",ex);
        }
    }
}
