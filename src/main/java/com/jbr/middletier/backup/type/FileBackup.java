package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.DbLoggingManager;
import com.jbr.middletier.backup.manager.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class FileBackup implements PerformBackup {
    private static final Logger LOG = LoggerFactory.getLogger(FileBackup.class);

    private static final String PATH_FILE_FORMAT = "%s/%s";

    void performFileBackup(DbLoggingManager dbLoggingManager, FileSystem fileSystem, String sourceDirectory, String destinationDirectory, String artifactName) throws IOException {
        // Perform a file backup.

        // Check that the source directory exists.
        File sourcePath = new File(sourceDirectory);
        if(!fileSystem.directoryExists(sourcePath.toPath())) {
            throw new IllegalStateException(String.format("Source directory %s does not exist", sourceDirectory));
        }

        File sourceFile = new File(String.format(PATH_FILE_FORMAT,sourceDirectory,artifactName));
        if(!fileSystem.fileExists(sourceFile)) {
            throw new IllegalStateException(String.format("Source file %s/%s does not exist", sourceDirectory, artifactName));
        }
        double fileSize = sourceFile.length();

        // Create the backup directory if it doesn't exist.
        File destinationPath = new File(destinationDirectory);
        fileSystem.createDirectory(destinationPath.toPath());

        // Does the destination file exist?
        File destinationFile = new File(String.format(PATH_FILE_FORMAT,destinationDirectory,artifactName));
        if(fileSystem.fileExists(destinationFile)) {
            LOG.info("File exists - {}/{}",destinationDirectory,artifactName);

            // Is it the same size?
            if (fileSize == destinationFile.length()) {
                LOG.info("Already backed up, exiting");
                return;
            }
        }

        // Perform the file copy.
        LOG.info("Copy {}/{} to {}/{}",sourceDirectory,artifactName,destinationDirectory,artifactName);
        dbLoggingManager.info(String.format("Copy %s/%s to %s/%s", sourceDirectory, artifactName, destinationDirectory, artifactName));
        FileSystem.TemporaryResultDTO result = new FileSystem.TemporaryResultDTO();
        fileSystem.copyFile(sourceFile,destinationFile,result);
    }

    @Override
    public void performBackup(BackupManager backupManager, DbLoggingManager dbLoggingManager, FileSystem fileSystem, Backup backup) {
        try {
            LOG.info("File Backup {} {} {} {} {}", backup.getId(), backup.getBackupName(), backup.getFileName(), backup.getArtifact(), backup.getDirectory());

            // Perform a file backup.
            performFileBackup(dbLoggingManager, fileSystem,backup.getDirectory(),String.format(PATH_FILE_FORMAT,backupManager.todaysDirectory(), backup.getBackupName()),backup.getArtifact());
        } catch (Exception ex) {
            LOG.error("Failed to perform file backup",ex);
            dbLoggingManager.error("file backup " + ex);
        }
    }
}
