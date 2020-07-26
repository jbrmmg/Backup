package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.BackupManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.*;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class FileBackup implements PerformBackup {
    private static final Logger LOG = LoggerFactory.getLogger(FileBackup.class);

    private static final String PATH_FILE_FORMAT = "%s/%s";

    void performFileBackup(BackupManager backupManager, String sourceDirectory, String destinationDirectory, String artifactName, boolean weblog) throws IOException {
        // Perform a file backup.

        // Check that the source directory exists.
        Path sourcePath = Paths.get(sourceDirectory);
        if(Files.notExists(sourcePath)) {
            throw new IllegalStateException(String.format("Source directory %s does not exist", sourceDirectory));
        }

        File sourceFile = new File(String.format(PATH_FILE_FORMAT,sourceDirectory,artifactName));
        if(!sourceFile.exists()) {
            throw new IllegalStateException(String.format("Source file %s/%s does not exist", sourceDirectory, artifactName));
        }
        double fileSize = sourceFile.length();

        // Create the backup directory if it doesn't exist.
        Path destinationPath = Paths.get(destinationDirectory);
        if(Files.notExists(destinationPath)) {
            Files.createDirectory(destinationPath);
        }

        // Does the destination file exist?
        File destinationFile = new File(String.format(PATH_FILE_FORMAT,destinationDirectory,artifactName));
        if(destinationFile.exists()) {
            LOG.info("File exists - {}/{}",destinationDirectory,artifactName);

            // Is it the same size?
            if (fileSize == destinationFile.length()) {
                LOG.info("Already backed up, exiting");
                return;
            }
        }

        // Perform the file copy.
        Path sourceFilePath = Paths.get(String.format(PATH_FILE_FORMAT,sourceDirectory,artifactName));
        Path destinationFilePath = Paths.get(String.format(PATH_FILE_FORMAT,destinationDirectory,artifactName));
        LOG.info("Copy {}/{} to {}/{}",sourceDirectory,artifactName,destinationDirectory,artifactName);
        if(weblog) {
            backupManager.postWebLog(BackupManager.webLogLevel.INFO, String.format("Copy %s/%s to %s/%s", sourceDirectory, artifactName, destinationDirectory, artifactName));
        }
        Files.copy(sourceFilePath,destinationFilePath,REPLACE_EXISTING);
    }

    @Override
    public void performBackup(BackupManager backupManager, Backup backup) {
        try {
            LOG.info("File Backup {} {} {} {} {}", backup.getId(), backup.getBackupName(), backup.getFileName(), backup.getArtifact(), backup.getDirectory());

            // Perform a file backup.
            performFileBackup(backupManager,backup.getDirectory(),String.format(PATH_FILE_FORMAT,backupManager.todaysDirectory(), backup.getBackupName()),backup.getArtifact(),true);
        } catch (Exception ex) {
            LOG.error("Failed to perform file backup",ex);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"file backup " + ex);
        }
    }
}
