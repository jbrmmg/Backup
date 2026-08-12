package com.jbr.middletier.backup.schedule;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.manager.ActionManager;
import com.jbr.middletier.backup.manager.DriveManager;
import com.jbr.middletier.backup.manager.DuplicateManager;
import com.jbr.middletier.backup.manager.SynchronizeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GatherSynchronizeCtrl {
    private static final Logger LOG = LoggerFactory.getLogger(GatherSynchronizeCtrl.class);

    private final ApplicationProperties applicationProperties;
    private final ActionManager emailManager;
    private final DriveManager driveManager;
    private final DuplicateManager duplicateManager;
    private final SynchronizeManager synchronizeManager;
    private final SummaryCtrl summaryCtrl;

    @Autowired
    public GatherSynchronizeCtrl(ApplicationProperties applicationProperties,
                                 ActionManager emailManager,
                                 DriveManager driveManager,
                                 DuplicateManager duplicateManager,
                                 SynchronizeManager synchronizeManager,
                                 SummaryCtrl summaryCtrl) {
        this.applicationProperties = applicationProperties;
        this.emailManager = emailManager;
        this.driveManager = driveManager;
        this.duplicateManager = duplicateManager;
        this.synchronizeManager = synchronizeManager;
        this.summaryCtrl = summaryCtrl;
    }

    @Scheduled(cron = "#{@applicationProperties.gatherSchedule}")
    public void gatherCron() {
        if(applicationProperties.getGatherEnabled()) {
            try {
                emailManager.sendActionEmail();

                driveManager.gather(null);

                duplicateManager.duplicateCheck();

                synchronizeManager.synchronize(null);

                summaryCtrl.refreshSummary();
            } catch (Exception ex) {
                LOG.error("Failed to gather / synchronize",ex);
            }
        }
    }
}
