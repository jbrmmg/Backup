package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.BackupManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class GitBackup extends FileBackup {
    private static final Logger LOG = LoggerFactory.getLogger(GitBackup.class);

    private static final String PATH_FILE_FORMAT = "%s/%s";

    @Override
    public void performBackup(BackupManager backupManager, Backup backup) {
        try {
            LOG.info("Git Backup {} {} {}", backup.getId(), backup.getBackupName(), backup.getDirectory());

            // Perform a git backup.

            // Create the backup directory if it doesn't exist.
            Path destinationPath = Paths.get(String.format(PATH_FILE_FORMAT, backupManager.todaysDirectory(), backup.getBackupName()));
            if (Files.notExists(destinationPath)) {
                Files.createDirectory(destinationPath);
            }

            // Get a list of files that are in the directory.
            File folder = new File(backup.getDirectory());
            if(!folder.exists()) {
                throw new IllegalStateException("DirectoryInfo does not exist.");
            }

            File[] listOfFiles = folder.listFiles();

            if(listOfFiles != null) {
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        if (listOfFile.getName().startsWith(".")) {
                            continue;
                        }

                        // Copy file if not already created.
                        LOG.info("File {} copy to {}", listOfFile.getName(), destinationPath.toString());
                        performFileBackup(backupManager, backup.getDirectory(), destinationPath.toString(), listOfFile.getName(), false);
                    } else if (listOfFile.isDirectory()) {
                        if (listOfFile.getName().equalsIgnoreCase("target")) {
                            continue;
                        }

                        LOG.info("DirectoryInfo {} copy to {}/{}", listOfFile.getName(), destinationPath.toString(), listOfFile.getName());

                        // If not existing, create the directory.
                        Path newDirectoryPath = Paths.get(String.format(PATH_FILE_FORMAT, destinationPath.toString(), listOfFile.getName()));
                        if (Files.notExists(newDirectoryPath)) {
                            Files.createDirectory(newDirectoryPath);
                        }

                        // Do the copy.
                        File source = new File(String.format(PATH_FILE_FORMAT, backup.getDirectory(), listOfFile.getName()));
                        File destination = new File(newDirectoryPath.toString());
                        FileUtils.copyDirectory(source, destination, true);

                        backupManager.postWebLog(BackupManager.webLogLevel.INFO, String.format("DirectoryInfo %s copy to %s/%s", listOfFile.getName(), destinationPath.toString(), listOfFile.getName()));
                    }
                }
            }

            LOG.info("Backup completed.");
        } catch (Exception ex) {
            LOG.error("Failed to perform git backup", ex);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"git backup " + ex);
        }
    }
}
