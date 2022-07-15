package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystem;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Created by jason on 12/02/17.
 */

@Component
public class CleanBackup implements PerformBackup {
    private static final Logger LOG = LoggerFactory.getLogger(CleanBackup.class);

    private final ApplicationProperties applicationProperties;

    public CleanBackup(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    private boolean shouldDirectoryBeDeleted(BackupManager backupManager,String directory) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(applicationProperties.getDirectory().getDateFormat());

            // Convert the directory name to a date.
            LocalDate directoryDate = LocalDate.parse(directory, formatter);
            LocalDate maxDaysAgo = LocalDate.now().minusDays(applicationProperties.getDirectory().getDays());

            if (directoryDate.isBefore(maxDaysAgo)) {
                // Delete this directory.
                LOG.info("Delete directory {}", directory);
                return true;
            }
        } catch ( DateTimeParseException ex ) {
            LOG.warn(String.format("Failed to convert directory name %s to a date",directory));
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to convert directory " + ex);
        }

        return false;
    }

    private void deleleDirectory(BackupManager backupManager, String directory) {
        try {
            File directoryToDelete = new File(directory);
            FileUtils.deleteDirectory(directoryToDelete);
            LOG.info("Deleted {}",directory);
            backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format("Deleted %s",directory));
        } catch ( IOException ex ) {
            LOG.warn(String.format("Failed to deleted %s",directory));
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"delete directory " + ex);
        }
    }

    @Override
    public void performBackup(BackupManager backupManager, FileSystem fileSystem, Backup backup) {
        backupManager.postWebLog(BackupManager.webLogLevel.INFO,"Clean Backup.");

        // Remove any backup directories older than x days
        File folder = new File(applicationProperties.getDirectory().getName());
        if(!folder.exists()) {
            backupManager.postWebLog(BackupManager.webLogLevel.WARN,"Backup directory does not exist.");
            throw new IllegalStateException("Backup directory does not exist.");
        }

        File[] listOfFiles = folder.listFiles();
        if(listOfFiles != null) {
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isDirectory() && shouldDirectoryBeDeleted(backupManager, listOfFile.getName())) {
                    deleleDirectory(backupManager, String.format("%s/%s", applicationProperties.getDirectory().getName(), listOfFile.getName()));
                }
            }
        }

        LOG.info("Clean complete.");
    }
}
