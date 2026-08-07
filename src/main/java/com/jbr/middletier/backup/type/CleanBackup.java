package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.RunStatus;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystem;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CleanBackup implements PerformBackup {
    private static final Logger LOG = LoggerFactory.getLogger(CleanBackup.class);

    private final ApplicationProperties applicationProperties;
    private String lastSummary = "";

    @Autowired
    public CleanBackup(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getType() {
        return TypeManager.CLEAN_TYPE;
    }

    @Override
    public String getSummary() {
        return lastSummary;
    }

    private boolean shouldDirectoryBeDeleted(String directory) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(applicationProperties.getDirectory().getDateFormat());
            LocalDate directoryDate = LocalDate.parse(directory, formatter);
            LocalDate maxDaysAgo = LocalDate.now().minusDays(applicationProperties.getDirectory().getDays());
            if (directoryDate.isBefore(maxDaysAgo)) {
                LOG.info("Delete directory {}", directory);
                return true;
            }
        } catch (DateTimeParseException ex) {
            LOG.warn("Failed to convert directory name {} to a date", directory);
        }
        return false;
    }

    private void deleteDirectory(String directory) throws IOException {
        File directoryToDelete = new File(directory);
        FileUtils.deleteDirectory(directoryToDelete);
        LOG.info("Deleted {}", directory);
    }

    @Override
    public RunStatus performBackup(BackupManager backupManager, FileSystem fileSystem, Backup backup) {
        LOG.info("Clean Backup.");
        List<String> deleted = new ArrayList<>();

        File folder = new File(applicationProperties.getDirectory().getName());
        if (!folder.exists()) {
            lastSummary = "Backup directory does not exist.";
            throw new IllegalStateException("Backup directory does not exist.");
        }

        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isDirectory() && shouldDirectoryBeDeleted(listOfFile.getName())) {
                    String path = String.format("%s/%s", applicationProperties.getDirectory().getName(), listOfFile.getName());
                    try {
                        deleteDirectory(path);
                        deleted.add(path);
                    } catch (IOException ex) {
                        LOG.warn("Failed to delete {}", path);
                    }
                }
            }
        }

        lastSummary = deleted.isEmpty() ? "No directories deleted" : "Deleted: " + String.join(", ", deleted);
        LOG.info("Clean complete.");
        return RunStatus.SUCCESS;
    }
}
