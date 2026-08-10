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

@Component
public class FileBackup implements PerformBackup {
    private static final Logger LOG = LoggerFactory.getLogger(FileBackup.class);

    private static final String PATH_FILE_FORMAT = "%s/%s";
    private String lastSummary = "";

    @Override
    public String getType() {
        return TypeManager.FILE_TYPE;
    }

    @Override
    public String getSummary() {
        return lastSummary;
    }

    RunStatus performFileBackup(FileSystem fileSystem, String sourceDirectory, String destinationDirectory, String artifactName) throws IOException {
        File sourcePath = new File(sourceDirectory);
        if (!fileSystem.directoryExists(sourcePath.toPath())) {
            throw new IllegalStateException(String.format("Source directory %s does not exist", sourceDirectory));
        }

        File sourceFile = new File(String.format(PATH_FILE_FORMAT, sourceDirectory, artifactName));
        if (!fileSystem.fileExists(sourceFile)) {
            throw new IllegalStateException(String.format("Source file %s/%s does not exist", sourceDirectory, artifactName));
        }
        double fileSize = sourceFile.length();

        File destinationPath = new File(destinationDirectory);
        fileSystem.createDirectory(destinationPath.toPath());

        File destinationFile = new File(String.format(PATH_FILE_FORMAT, destinationDirectory, artifactName));
        if (fileSystem.fileExists(destinationFile)) {
            LOG.info("File exists - {}/{}", destinationDirectory, artifactName);
            if (fileSize == destinationFile.length()) {
                LOG.info("Already backed up, exiting");
                return RunStatus.SUCCESS;
            }
        }

        LOG.info("Copy {}/{} to {}/{}", sourceDirectory, artifactName, destinationDirectory, artifactName);
        FileSystem.TemporaryResultDTO result = new FileSystem.TemporaryResultDTO();
        fileSystem.copyFile(sourceFile, destinationFile, result);
        return RunStatus.SUCCESS;
    }

    @Override
    public RunStatus performBackup(BackupManager backupManager, FileSystem fileSystem, Backup backup) {
        try {
            LOG.info("File Backup {} {} {} {} {}", backup.getId(), backup.getBackupName(), backup.getFileName(), backup.getArtifact(), backup.getDirectory());
            RunStatus status = performFileBackup(fileSystem, backup.getDirectory(),
                    String.format(PATH_FILE_FORMAT, backupManager.todaysDirectory(), backup.getBackupName()),
                    backup.getArtifact());
            lastSummary = String.format("%s backed up", backup.getArtifact());
            return status;
        } catch (Exception ex) {
            LOG.error("Failed to perform file backup", ex);
            lastSummary = ex.getMessage();
            return RunStatus.FAILED;
        }
    }
}
