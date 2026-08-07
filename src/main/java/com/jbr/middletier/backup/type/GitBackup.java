package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.RunStatus;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Component
public class GitBackup extends FileBackup {
    private static final Logger LOG = LoggerFactory.getLogger(GitBackup.class);

    private static final String PATH_FILE_FORMAT = "%s/%s";
    private String lastSummary = "";

    @Override
    public String getType() {
        return TypeManager.GIT_TYPE;
    }

    @Override
    public String getSummary() {
        return lastSummary;
    }

    private Path ensureBackupDirectoryExists(FileSystem fileSystem, String todaysDirectory, String name) throws IOException {
        File destinationPath = new File(String.format(PATH_FILE_FORMAT, todaysDirectory, name));
        fileSystem.createDirectory(destinationPath.toPath());
        return destinationPath.toPath();
    }

    private void processFile(FileSystem fileSystem, File listOfFile, Path destinationPath, Backup backup) throws IOException {
        if (listOfFile.getName().startsWith(".")) {
            return;
        }
        LOG.info("File {} copy to {}", listOfFile.getName(), destinationPath);
        performFileBackup(fileSystem, backup.getDirectory(), destinationPath.toString(), listOfFile.getName());
    }

    private void processDirectory(FileSystem fileSystem, File listOfFile, Path destinationPath, Backup backup) throws IOException {
        if (listOfFile.getName().equalsIgnoreCase("target")) {
            return;
        }
        LOG.info("DirectoryInfo {} copy to {}/{}", listOfFile.getName(), destinationPath, listOfFile.getName());
        Path destination = ensureBackupDirectoryExists(fileSystem, destinationPath.toString(), listOfFile.getName());
        File source = new File(String.format(PATH_FILE_FORMAT, backup.getDirectory(), listOfFile.getName()));
        FileSystem.TemporaryResultDTO result = new FileSystem.TemporaryResultDTO();
        fileSystem.copyDirectory(source, destination.toFile(), result);
    }

    @Override
    public RunStatus performBackup(BackupManager backupManager, FileSystem fileSystem, Backup backup) {
        try {
            LOG.info("Git Backup {} {} {}", backup.getId(), backup.getBackupName(), backup.getDirectory());

            Path destinationPath = ensureBackupDirectoryExists(fileSystem, backupManager.todaysDirectory(), backup.getBackupName());

            File folder = new File(backup.getDirectory());
            if (!folder.exists()) {
                throw new IllegalStateException("DirectoryInfo does not exist.");
            }

            File[] listOfFiles = folder.listFiles();
            if (listOfFiles == null || listOfFiles.length == 0) {
                lastSummary = String.format("%s backed up", backup.getDirectory());
                return RunStatus.SUCCESS;
            }

            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    processFile(fileSystem, listOfFile, destinationPath, backup);
                } else if (listOfFile.isDirectory()) {
                    processDirectory(fileSystem, listOfFile, destinationPath, backup);
                }
            }

            LOG.info("Backup completed.");
            lastSummary = String.format("%s backed up", backup.getDirectory());
            return RunStatus.SUCCESS;
        } catch (Exception ex) {
            LOG.error("Failed to perform git backup", ex);
            lastSummary = ex.getMessage();
            return RunStatus.FAILED;
        }
    }
}
