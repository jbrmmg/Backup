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
import org.apache.commons.io.FileUtils;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class GitBackup extends FileBackup {
    private static final Logger LOG = LoggerFactory.getLogger(GitBackup.class);

    private static final String PATH_FILE_FORMAT = "%s/%s";

    // TODO - increase testing on this

    private Path ensureBackupDirectoryExists(String todaysDirectory, String name) throws IOException {
        Path destinationPath = Paths.get(String.format(PATH_FILE_FORMAT, todaysDirectory, name));
        if (Files.notExists(destinationPath)) {
            Files.createDirectory(destinationPath);
        }

        return destinationPath;
    }

    private void processFile(File listOfFile, Path destinationPath, BackupManager backupManager, Backup backup) throws IOException {
        if (listOfFile.getName().startsWith(".")) {
            return;
        }

        // Copy file if not already created.
        LOG.info("File {} copy to {}", listOfFile.getName(), destinationPath);
        performFileBackup(backupManager, backup.getDirectory(), destinationPath.toString(), listOfFile.getName(), false);
    }

    private void processDirectory(File listOfFile, Path destinationPath, BackupManager backupManager, Backup backup) throws IOException {
        if (listOfFile.getName().equalsIgnoreCase("target")) {
            return;
        }

        LOG.info("DirectoryInfo {} copy to {}/{}", listOfFile.getName(), destinationPath, listOfFile.getName());

        // If not existing, create the directory.
        Path newDirectoryPath = Paths.get(String.format(PATH_FILE_FORMAT, destinationPath.toString(), listOfFile.getName()));
        if (Files.notExists(newDirectoryPath)) {
            Files.createDirectory(newDirectoryPath);
        }

        // Do the copy.
        File source = new File(String.format(PATH_FILE_FORMAT, backup.getDirectory(), listOfFile.getName()));
        File destination = new File(newDirectoryPath.toString());
        FileUtils.copyDirectory(source, destination, true);

        backupManager.postWebLog(BackupManager.webLogLevel.INFO, String.format("DirectoryInfo %s copy to %s/%s", listOfFile.getName(), destinationPath, listOfFile.getName()));
    }

    @Override
    public void performBackup(BackupManager backupManager, Backup backup) {
        try {
            LOG.info("Git Backup {} {} {}", backup.getId(), backup.getBackupName(), backup.getDirectory());

            // Perform a git backup.

            // Create the backup directory if it doesn't exist.
            Path destinationPath = ensureBackupDirectoryExists(backupManager.todaysDirectory(),backup.getBackupName());

            // Get a list of files that are in the directory.
            File folder = new File(backup.getDirectory());
            if(!folder.exists()) {
                throw new IllegalStateException("DirectoryInfo does not exist.");
            }

            File[] listOfFiles = folder.listFiles();

            // If no files then return.
            if(listOfFiles == null || listOfFiles.length == 0) {
                return;
            }

            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    processFile(listOfFile,destinationPath,backupManager,backup);
                } else if (listOfFile.isDirectory()) {
                    processDirectory(listOfFile,destinationPath,backupManager,backup);
                }
            }

            LOG.info("Backup completed.");
        } catch (Exception ex) {
            LOG.error("Failed to perform git backup", ex);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"git backup " + ex);
        }
    }
}
