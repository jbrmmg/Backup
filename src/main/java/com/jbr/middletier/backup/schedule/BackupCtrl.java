package com.jbr.middletier.backup.schedule;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.BackupJobRun;
import com.jbr.middletier.backup.data.RunStatus;
import com.jbr.middletier.backup.dataaccess.BackupJobRunRepository;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.type.PerformBackup;
import com.jbr.middletier.backup.type.TypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BackupCtrl {
    private static final Logger LOG = LoggerFactory.getLogger(BackupCtrl.class);

    private final TypeManager typeManager;
    private final BackupManager backupManager;
    private final BackupRepository backupRepository;
    private final BackupJobRunRepository backupJobRunRepository;
    private final ApplicationProperties applicationProperties;
    private final FileSystem fileSystem;

    @Autowired
    public BackupCtrl(TypeManager typeManager,
                      BackupManager backupManager,
                      BackupRepository backupRepository,
                      BackupJobRunRepository backupJobRunRepository,
                      ApplicationProperties applicationProperties,
                      FileSystem fileSystem) {
        this.typeManager = typeManager;
        this.backupManager = backupManager;
        this.backupRepository = backupRepository;
        this.backupJobRunRepository = backupJobRunRepository;
        this.applicationProperties = applicationProperties;
        this.fileSystem = fileSystem;
    }

    private void pruneOldRuns() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(applicationProperties.getJobRunRetentionDays());
        backupJobRunRepository.deleteByStartedAtBefore(cutoff);
    }

    private void performBackups(List<Backup> backups) {
        try {
            backupManager.initialiseDay(fileSystem);
        } catch (Exception ex) {
            LOG.error("Failed to initialise backup directory", ex);
            for (Backup backup : backups) {
                BackupJobRun run = new BackupJobRun(backup.getId());
                run.complete(RunStatus.FAILED, ex.getMessage());
                backupJobRunRepository.save(run);
            }
            return;
        }

        for (Backup backup : backups) {
            LOG.info("Perform backup {}", backup.getId());
            BackupJobRun run = new BackupJobRun(backup.getId());
            backupJobRunRepository.save(run);

            try {
                PerformBackup performBackup = typeManager.getBackup(backup.getType());
                RunStatus status = performBackup.performBackup(backupManager, fileSystem, backup);
                run.complete(status, performBackup.getSummary());
            } catch (Exception ex) {
                LOG.error("Failed to perform backup {}", backup.getId(), ex);
                run.complete(RunStatus.FAILED, ex.getMessage());
            }

            backupJobRunRepository.save(run);
        }
    }

    public void performBackup(Backup backup) {
        performBackups(List.of(backup));
    }

    @Scheduled(cron = "#{@applicationProperties.schedule}")
    public void scheduleBackup() {
        LOG.info("Backup");
        if (!applicationProperties.getEnabled()) {
            LOG.warn("Disabled!");
            return;
        }

        pruneOldRuns();

        List<Backup> backupList = backupRepository.findAllByOrderByTimeAsc();
        performBackups(backupList);
    }
}
