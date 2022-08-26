package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class BackupManager {
    private static final Logger LOG = LoggerFactory.getLogger(BackupManager.class);

    private final ApplicationProperties applicationProperties;
    private final DbLoggingManager dbLoggingManager;

    public BackupManager(ApplicationProperties applicationProperties, DbLoggingManager dbLoggingManager) {
        this.applicationProperties = applicationProperties;
        this.dbLoggingManager = dbLoggingManager;
    }

    public String todaysDirectory() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(this.applicationProperties.getDirectory().getDateFormat());
        LocalDate today = LocalDate.now();

        return String.format("%s/%s/",this.applicationProperties.getDirectory().getName(),formatter.format(today));
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
            dbLoggingManager.info("Created directory + " + todaysDirectoryPath);
        }
    }
}
