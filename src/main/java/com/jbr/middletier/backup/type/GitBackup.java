package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class GitBackup extends FileBackup {
    private static final Logger LOG = LoggerFactory.getLogger(GitBackup.class);

    private static final String PATH_FILE_FORMAT = "%s/%s";

    private Path ensureBackupDirectoryExists(FileSystem fileSystem, String todaysDirectory, String name) throws IOException {
        File destinationPath = new File(String.format(PATH_FILE_FORMAT, todaysDirectory, name));
        fileSystem.createDirectory(destinationPath.toPath());

        return destinationPath.toPath();
    }

    private void processFile(FileSystem fileSystem, File listOfFile, Path destinationPath, BackupManager backupManager, Backup backup) throws IOException {
        if (listOfFile.getName().startsWith(".")) {
            return;
        }

        // Copy file if not already created.
        LOG.info("File {} copy to {}", listOfFile.getName(), destinationPath);
        performFileBackup(backupManager, fileSystem, backup.getDirectory(), destinationPath.toString(), listOfFile.getName(), false);
    }

    private void processDirectory(FileSystem fileSystem, File listOfFile, Path destinationPath, BackupManager backupManager, Backup backup) throws IOException {
        if (listOfFile.getName().equalsIgnoreCase("target")) {
            return;
        }

        LOG.info("DirectoryInfo {} copy to {}/{}", listOfFile.getName(), destinationPath, listOfFile.getName());

        // If not existing, create the directory.
        Path destination = ensureBackupDirectoryExists(fileSystem, destinationPath.toString(), listOfFile.getName());

        // Do the copy.
        File source = new File(String.format(PATH_FILE_FORMAT, backup.getDirectory(), listOfFile.getName()));
        FileSystem.TemporaryResultDTO result = new FileSystem.TemporaryResultDTO();
        fileSystem.copyDirectory(source, destination.toFile(),result);

        backupManager.postWebLog(BackupManager.webLogLevel.INFO, String.format("DirectoryInfo %s copy to %s/%s", listOfFile.getName(), destinationPath, listOfFile.getName()));
    }

    @Override
    public void performBackup(BackupManager backupManager, FileSystem fileSystem, Backup backup) {
        try {
            LOG.info("Git Backup {} {} {}", backup.getId(), backup.getBackupName(), backup.getDirectory());

            // Perform a git backup.

            // Create the backup directory if it doesn't exist.
            Path destinationPath = ensureBackupDirectoryExists(fileSystem,backupManager.todaysDirectory(),backup.getBackupName());

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
                    processFile(fileSystem, listOfFile, destinationPath, backupManager, backup);
                } else if (listOfFile.isDirectory()) {
                    processDirectory(fileSystem,listOfFile,destinationPath,backupManager,backup);
                }
            }

            LOG.info("Backup completed.");
        } catch (Exception ex) {
            LOG.error("Failed to perform git backup", ex);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"git backup " + ex);
        }
    }
}
