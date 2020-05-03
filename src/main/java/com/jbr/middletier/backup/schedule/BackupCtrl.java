package com.jbr.middletier.backup.schedule;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.dataaccess.BackupSpecifications;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.type.PerformBackup;
import com.jbr.middletier.backup.type.TypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class BackupCtrl {
    final static private Logger LOG = LoggerFactory.getLogger(BackupCtrl.class);

    private final
    TypeManager typeManager;

    private final
    BackupManager backupManager;

    private final
    BackupRepository backupRepository;

    @Autowired
    public BackupCtrl(TypeManager typeManager,
                      BackupManager backupManager,
                      BackupRepository backupRepository) {
        this.typeManager = typeManager;
        this.backupManager = backupManager;
        this.backupRepository = backupRepository;
    }

    private void performBackups(List<Backup> backups) {
        try {
            // Initialise the backup directory.
            backupManager.initialiseDay();

            // Process backups.
            for (Backup backup : backups) {
                LOG.info(String.format("Perform backup %s.",backup.getId()));

                // Get the backup type.
                PerformBackup performBackup = typeManager.getBackup(backup.getType());

                // Perform the backup.
                performBackup.performBackup(backupManager,backup);
            }
        } catch (Exception ex) {
            LOG.error("Failed to perform backup",ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Scheduled(cron = "${backup.schedule}")
    public void scheduleBackup() {
        LOG.info("Running backups.");

        backupManager.postWebLog(BackupManager.webLogLevel.INFO,"Running Backups.");

        // Get the current time, and look for any backup that has
        Calendar calendar = Calendar.getInstance();
        int endTime = calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE);
        int startTime = endTime - 120;

        List<Backup> backupList = (List<Backup>) backupRepository.findAll(Specification.where(BackupSpecifications.backupsBetweenTimes(startTime,endTime)));

        // Sort the list by the backup time.
        Collections.sort(backupList);

        // If any backups return, perform the backup.
        performBackups(backupList);

        backupManager.postWebLog(BackupManager.webLogLevel.INFO,"Running Backups complete.");
    }
}
