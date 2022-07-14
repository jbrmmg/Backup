package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Created by jason on 11/02/17.
 */
@Component
public class DatabaseBackup implements PerformBackup {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseBackup.class);

    private final ApplicationProperties applicationProperties;

    @Autowired
    public DatabaseBackup(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    private String getDBServerName() {
        // Get the database server name.
        String[] urlElements = applicationProperties.getDbUrl().split(":");

        if(urlElements.length < 3) {
            throw new IllegalStateException(String.format("Cannot determine DB server name from url - %s (x:x:x)",applicationProperties.getDbUrl()));
        }

        return urlElements[2].replace("//","");
    }

    private String getBackupCommand(BackupManager backupManager, Backup backup) {
        String result = applicationProperties.getDbBackupCommand();

        if(backup.getDirectory().startsWith("db:")) {
            String[] databaseElements = backup.getDirectory().split(":");

            if(databaseElements.length < 4) {
                throw new IllegalStateException(String.format("Cannot determine DB server name from url - %s (x:x:x:x)",backup.getDirectory()));
            }

            result = result.replace("$$server$$", databaseElements[1]);
            result = result.replace("$$user$$", databaseElements[2]);
            result = result.replace("$$password$$", databaseElements[3]);
        } else {
            result = result.replace("$$server$$", getDBServerName());
            result = result.replace("$$user$$", applicationProperties.getDbUsername() == null ? "" : applicationProperties.getDbUsername());
            result = result.replace("$$password$$", applicationProperties.getDbPassword() == null ? "" : applicationProperties.getDbPassword());
        }

        result = result.replace("$$dbname$$",backup.getArtifact());
        result = result.replace("$$todaydir$$",backupManager.todaysDirectory());
        result = result.replace("$$backupname$$",backup.getBackupName());
        result = result.replace("$$output$$",backup.getArtifact());

        return result;
    }

    @Override
    public void performBackup(BackupManager backupManager, FileSystem fileSystem, Backup backup) {
        try {
            backupManager.postWebLog(BackupManager.webLogLevel.INFO, String.format("Database Backup %s %s %s %s", backup.getId(), backup.getBackupName(), backup.getArtifact(), backup.getDirectory()));
            LOG.info("Database Backup {} {} {} {}", backup.getId(), backup.getBackupName(), backup.getArtifact(), backup.getDirectory());

            // Perform a database backup.

            // Create the backup directory if it doesn't exist.
            File destinationPath = new File(String.format("%s/%s",backupManager.todaysDirectory(), backup.getBackupName()));
            fileSystem.createDirectory(destinationPath.toPath());

            // Does the destination file exist?
            File destinationFile = new File(String.format("%s/%s/%s",backupManager.todaysDirectory(),backup.getBackupName(),backup.getArtifact()));
            if(destinationFile.exists()) {
                if(LOG.isInfoEnabled()) {
                    LOG.info("File exists - {}/{}/{}", backupManager.todaysDirectory(), backup.getBackupName(), backup.getArtifact());
                }

                // Is there a file
                if (destinationFile.length() > 100) {
                    LOG.info("Already backed up, exiting");
                    return;
                }
            }

            // Perform the database backup using mysqldump.
            String backupCommand = getBackupCommand(backupManager, backup);
            LOG.info("Command: {}", backupCommand);

            String[] cmd = new String[]{"bash","-c",backupCommand};
            final Process backupProcess = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            backupProcess.waitFor(applicationProperties.getDbBackupMaxTime(), TimeUnit.SECONDS);
            backupProcess.destroyForcibly();

            LOG.info("Backup completed.");
        } catch (Exception ex) {
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"db backup " + ex);
            LOG.error("Failed to perform database backup",ex);
            Thread.currentThread().interrupt();
        }
    }
}
