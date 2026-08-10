package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.RunStatus;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Component
public class DatabaseBackup implements PerformBackup {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseBackup.class);

    private final ApplicationProperties applicationProperties;
    private String lastSummary = "";

    @Autowired
    public DatabaseBackup(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getType() {
        return TypeManager.DATABASE_TYPE;
    }

    @Override
    public String getSummary() {
        return lastSummary;
    }

    private String getDBServerName() {
        String[] urlElements = applicationProperties.getDbUrl().split(":");
        if (urlElements.length < 3) {
            throw new IllegalStateException(String.format("Cannot determine DB server name from url - %s (x:x:x)", applicationProperties.getDbUrl()));
        }
        return urlElements[2].replace("//", "");
    }

    private String getBackupCommand(BackupManager backupManager, Backup backup) {
        String result = applicationProperties.getDbBackupCommand();

        if (backup.getDirectory().startsWith("db:")) {
            String[] databaseElements = backup.getDirectory().split(":");
            if (databaseElements.length < 4) {
                throw new IllegalStateException(String.format("Cannot determine DB server name from url - %s (x:x:x:x)", backup.getDirectory()));
            }
            result = result.replace("$$server$$", databaseElements[1]);
            result = result.replace("$$user$$", databaseElements[2]);
            result = result.replace("$$password$$", databaseElements[3]);
        } else {
            result = result.replace("$$server$$", getDBServerName());
            result = result.replace("$$user$$", applicationProperties.getDbUsername() == null ? "" : applicationProperties.getDbUsername());
            result = result.replace("$$password$$", applicationProperties.getDbPassword() == null ? "" : applicationProperties.getDbPassword());
        }

        result = result.replace("$$dbname$$", backup.getArtifact());
        result = result.replace("$$todaydir$$", backupManager.todaysDirectory());
        result = result.replace("$$backupname$$", backup.getBackupName());
        result = result.replace("$$output$$", backup.getArtifact());

        return result;
    }

    @Override
    public RunStatus performBackup(BackupManager backupManager, FileSystem fileSystem, Backup backup) {
        try {
            LOG.info("Database Backup {} {} {} {}", backup.getId(), backup.getBackupName(), backup.getArtifact(), backup.getDirectory());

            File destinationPath = new File(String.format("%s/%s", backupManager.todaysDirectory(), backup.getBackupName()));
            fileSystem.createDirectory(destinationPath.toPath());

            File destinationFile = new File(String.format("%s/%s/%s", backupManager.todaysDirectory(), backup.getBackupName(), backup.getArtifact()));
            if (destinationFile.exists()) {
                LOG.info("File exists - {}/{}/{}", backupManager.todaysDirectory(), backup.getBackupName(), backup.getArtifact());
                if (destinationFile.length() > 100) {
                    LOG.info("Already backed up, exiting");
                    lastSummary = String.format("Database %s backed up", backup.getArtifact());
                    return RunStatus.SUCCESS;
                }
            }

            String backupCommand = getBackupCommand(backupManager, backup);
            LOG.info("Command: {}", backupCommand);

            String[] cmd = new String[]{"sh", "-c", backupCommand};
            final Process backupProcess = new ProcessBuilder(cmd)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            backupProcess.waitFor(applicationProperties.getDbBackupMaxTime(), TimeUnit.SECONDS);
            backupProcess.destroyForcibly();

            LOG.info("Backup completed.");
            lastSummary = String.format("Database %s backed up", backup.getArtifact());
            return RunStatus.SUCCESS;
        } catch (Exception ex) {
            LOG.error("Failed to perform database backup", ex);
            lastSummary = ex.getMessage();
            Thread.currentThread().interrupt();
            return RunStatus.FAILED;
        }
    }
}
