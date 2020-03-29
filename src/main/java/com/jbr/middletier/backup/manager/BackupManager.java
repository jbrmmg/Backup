package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    final static private Logger LOG = LoggerFactory.getLogger(BackupManager.class);

    private final ApplicationProperties applicationProperties;

    public BackupManager(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
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
        }
    }
}
