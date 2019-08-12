package com.jbr.middletier.backup.manager;

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

    @Value("${middle.tier.backup.directory}")
    private String directory;

    @Value("${middle.tier.backup.directory.dateformat}")
    private String dateFormat;

    public String todaysDirectory() {
        DateFormat formatter = new SimpleDateFormat(dateFormat);
        Calendar calendar = Calendar.getInstance();

        return String.format("%s/%s/",directory,formatter.format(calendar.getTime()));
    }

    public void initialiseDay() throws IOException {
        LOG.info("Initialise the backup directory.");

        Path directoryPath = Paths.get(directory);

        // Does the directory exist?
        if(Files.notExists(directoryPath)) {
            throw new IllegalStateException(String.format("The defined directory path %s does not exist.", directory));
        }

        // What should today's directory be called?
        Path todaysDirectoryPath = Paths.get(todaysDirectory());

        // If not exists, create it.
        if(Files.notExists(todaysDirectoryPath)) {
            Files.createDirectory(todaysDirectoryPath);
        }
    }
}
