package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.BackupManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by jason on 11/02/17.
 */
@Component
public class DatabaseBackup implements PerformBackup {
    final static private Logger LOG = LoggerFactory.getLogger(DatabaseBackup.class);

    @Value("${middle.tier.backup.db.url}")
    private String dbUrl;

    @Value("${middle.tier.backup.db.username}")
    private String dbUsername;

    @Value("${middle.tier.backup.db.password}")
    private String dbPassword;

    private String getDBServerName() {
        // Get the database server name.
        String[] urlElements = dbUrl.split(":");

        if(urlElements.length < 3) {
            throw new IllegalStateException(String.format("Cannot determine DB server name from url - %s (x:x:x)",dbUrl));
        }

        return urlElements[2].replace("//","");
    }

    private String getBackupCommand(BackupManager backupManager, Backup backup) {
        if(backup.getDirectory().startsWith("db:")) {
            String[] databaseElements = backup.getDirectory().split(":");

            if(databaseElements.length < 4) {
                throw new IllegalStateException(String.format("Cannot determine DB server name from url - %s (x:x:x:x)",backup.getDirectory()));
            }

            return String.format("mysqldump -h %s -u %s -p%s %s > %s/%s/%s.sql",
                    databaseElements[1],
                    databaseElements[2],
                    databaseElements[3],
                    backup.getArtifact(),
                    backupManager.todaysDirectory(),
                    backup.getBackupName(),
                    backup.getArtifact());
        }

        return String.format("mysqldump -h %s -u %s -p%s %s > %s/%s/%s.sql",
                getDBServerName(),
                dbUsername,
                dbPassword,
                backup.getArtifact(),
                backupManager.todaysDirectory(),
                backup.getBackupName(),
                backup.getArtifact());
    }

    @Override
    public void performBackup(BackupManager backupManager, Backup backup) {
        try {
            LOG.info(String.format("Database Backup %s %s %s %s", backup.getId(), backup.getBackupName(), backup.getArtifact(), backup.getDirectory()));

            // Perform a database backup.

            // Create the backup directory if it doesn't exist.
            Path destinationPath = Paths.get(String.format("%s/%s",backupManager.todaysDirectory(), backup.getBackupName()));
            if(Files.notExists(destinationPath)) {
                Files.createDirectory(destinationPath);
            }

            // Does the destination file exist?
            File destinationFile = new File(String.format("%s/%s/%s",backupManager.todaysDirectory(),backup.getBackupName(),backup.getArtifact()));
            if(destinationFile.exists()) {
                LOG.info(String.format("File exists - %s/%s/%s",backupManager.todaysDirectory(),backup.getBackupName(),backup.getArtifact()));

                // Is there a file
                if (destinationFile.length() > 100) {
                    LOG.info("Already backed up, exiting");
                    return;
                }
            }

            // Perform the database backup using mysqldump.
            String backupCommand = getBackupCommand(backupManager, backup);
            LOG.info(String.format("Command: %s", backupCommand));

            String[] cmd = new String[]{"bash","-c",backupCommand};
            final Process backupProcess = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(1000 * 60 * 10);
                    LOG.warn("Killing backup, taken too long.");
                    backupProcess.destroy();
                } catch (InterruptedException ignored) {
                }
            });

            backupProcess.waitFor();
            t.interrupt();

            LOG.info("Backup completed.");
        } catch (Exception ex) {
            LOG.error("Failed to perform database backup",ex);
        }
    }
}