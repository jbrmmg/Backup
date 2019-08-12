package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.BackupManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Created by jason on 12/02/17.
 */

@Component
public class CleanBackup implements PerformBackup {
    final static private Logger LOG = LoggerFactory.getLogger(CleanBackup.class);

    @Value("${middle.tier.backup.directory}")
    private String directory;

    @Value("${middle.tier.backup.directory.dateformat}")
    private String dateFormat;

    @Value("${middle.tier.backup.directory.maxdays}")
    private int maxDays;

    private boolean shouldDirectoryBeDeleted(String directory) {
        try {
            DateFormat formatter = new SimpleDateFormat(dateFormat);

            // Convert the directory name to a date.
            Date directoryDate = formatter.parse(directory);

            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime maxDaysAgo = now.plusDays(-1 * maxDays);

            if (directoryDate.toInstant().isBefore(maxDaysAgo.toInstant())) {
                // Delete this directory.
                LOG.info(String.format("Delete directory %s", directory));
                return true;
            }
        } catch ( ParseException ex ) {
            LOG.warn(String.format("Failed to convert directory name %s to a date",directory));
        }

        return false;
    }

    private void deleleDirectory(String directory) {
        try {
            File directoryToDelete = new File(directory);
            FileUtils.deleteDirectory(directoryToDelete);
            LOG.info(String.format("Deleted %s",directory));
        } catch ( IOException ex ) {
            LOG.warn(String.format("Failed to deleted %s",directory));
        }
    }

    @Override
    public void performBackup(BackupManager backupManager, Backup backup) {
        // Remove any backup directories older than x days
        File folder = new File(directory);
        if(!folder.exists()) {
            throw new IllegalStateException("Backup directory does not exist.");
        }

        File[] listOfFiles = folder.listFiles();
        if(listOfFiles != null) {
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isDirectory()) {
                    if (shouldDirectoryBeDeleted(listOfFile.getName())) {
                        deleleDirectory(String.format("%s/%s", directory, listOfFile.getName()));
                    }
                }
            }
        }

        LOG.info("Clean complete.");
    }
}
