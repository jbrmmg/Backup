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
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class BackupManager {
    private static final Logger LOG = LoggerFactory.getLogger(BackupManager.class);

    private final ApplicationProperties applicationProperties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final EnumMap<webLogLevel, List<String>> messageCache;

    @PostConstruct
    public void initialise() {
        postWebLog(webLogLevel.INFO, applicationProperties.getServiceName() + " starting up.");
    }

    public BackupManager(ApplicationProperties applicationProperties,
                         RestTemplateBuilder restTemplateBuilder) {
        this.applicationProperties = applicationProperties;
        this.restTemplateBuilder = restTemplateBuilder;
        this.messageCache = new EnumMap<>(webLogLevel.class);
    }

    public String todaysDirectory() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(this.applicationProperties.getDirectory().getDateFormat());
        LocalDate today = LocalDate.now();

        return String.format("%s/%s/",this.applicationProperties.getDirectory().getName(),formatter.format(today));
    }

    public List<String> getMessageCache(webLogLevel level) {
        if(!this.messageCache.containsKey(level)) {
            return new ArrayList<>();
        }

        return this.messageCache.get(level);
    }

    public void clearMessageCache() {
        for(Map.Entry<webLogLevel, List<String>> nextEntry : this.messageCache.entrySet()) {
            nextEntry.getValue().clear();
        }
    }

    public void initialiseDay(FileSystem fileSystem) throws IOException {
        LOG.info("Initialise the backup directory.");

        File directoryPath = new File(this.applicationProperties.getDirectory().getName());

        // Does the directory exist?
        if(!fileSystem.directoryExists(directoryPath.toPath())) {
            throw new IllegalStateException(String.format("The defined directory path %s does not exist.", this.applicationProperties.getDirectory().getName()));
        }

        // What should today's directory be called?
        File todaysDirectoryPath = new File(todaysDirectory());

        // If not exists, create it.
        if(!fileSystem.directoryExists(todaysDirectoryPath.toPath())) {
            fileSystem.createDirectory(todaysDirectoryPath.toPath());
            postWebLog(webLogLevel.INFO,"Created directory + " + todaysDirectoryPath);
        }
    }

    public enum webLogLevel { DEBUG, INFO, WARN, ERROR }

    public void postWebLog(webLogLevel level, String message) {
        try {
            // Only perform if there is a web log URL.
            if(applicationProperties.getWebLogUrl() == null || applicationProperties.getWebLogUrl().length() == 0) {
                // If there is no web url and required then keep a cache.
                if(applicationProperties.getCacheWebLog()) {
                    List<String> messages;
                    if (!messageCache.containsKey(level)) {
                        messages = new ArrayList<>();
                        messageCache.put(level, messages);
                    } else {
                        messages = messageCache.get(level);
                    }
                    messages.add(message);
                }
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
