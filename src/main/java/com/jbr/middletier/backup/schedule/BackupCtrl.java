package com.jbr.middletier.backup.schedule;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.dataaccess.BackupSpecifications;
import com.jbr.middletier.backup.manager.*;
import com.jbr.middletier.backup.type.PerformBackup;
import com.jbr.middletier.backup.type.TypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class BackupCtrl {
    private static final Logger LOG = LoggerFactory.getLogger(BackupCtrl.class);

    private final TypeManager typeManager;
    private final BackupManager backupManager;
    private final BackupRepository backupRepository;
    private final ApplicationProperties applicationProperties;
    private final FileSystem fileSystem;

    @Autowired
    public BackupCtrl(TypeManager typeManager,
                      BackupManager backupManager,
                      BackupRepository backupRepository,
                      ApplicationProperties applicationProperties,
                      FileSystem fileSystem) {
        this.typeManager = typeManager;
        this.backupManager = backupManager;
        this.backupRepository = backupRepository;
        this.applicationProperties = applicationProperties;
        this.fileSystem = fileSystem;
    }

    private void performBackups(List<Backup> backups) {
        try {
            // Initialise the backup directory.
            backupManager.initialiseDay(fileSystem);

            // Process backups.
            for (Backup backup : backups) {
                LOG.info("Perform backup {}",backup.getId());

                // Get the backup type.
                PerformBackup performBackup = typeManager.getBackup(backup.getType());

                // Perform the backup.
                performBackup.performBackup(backupManager,fileSystem,backup);
            }
        } catch (Exception ex) {
            LOG.error("Failed to perform backup",ex);
        }
    }

    public void performBackup(Backup backup) {
        List<Backup> backupList = new ArrayList<>();
        backupList.add(backup);

        performBackups(backupList);
    }

    @Scheduled(cron = "#{@applicationProperties.schedule}")
    public void scheduleBackup() {
        LOG.info("Backup");
        if(!applicationProperties.getEnabled()) {
            LOG.warn("Disabled!");
            return;
        }

        // Get the current time, and look for any backup that has
        Calendar calendar = Calendar.getInstance();
        int endTime = calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE);
        int startTime = endTime - 120;

        List<Backup> backupList = backupRepository.findAll(Specification.where(BackupSpecifications.backupsBetweenTimes(startTime,endTime)));

        // Sort the list by the backup time.
        backupList.sort(Comparator.comparingLong(Backup::getTime));

        // If any backups return, perform the backup.
        performBackups(backupList);
    }
}
