package com.jbr.middletier.backup.schedule;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import com.jbr.middletier.backup.summary.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SummaryCtrl {
    private static final Logger LOG = LoggerFactory.getLogger(SummaryCtrl.class);

    private final ApplicationProperties applicationProperties;
    private final AssociatedFileDataManager associatedFileDataManager;
    private final FileSystemObjectManager fileSystemObjectManager;

    @Autowired
    public SummaryCtrl(ApplicationProperties applicationProperties,
                       AssociatedFileDataManager associatedFileDataManager,
                       FileSystemObjectManager fileSystemObjectManager) {
        this.applicationProperties = applicationProperties;
        this.associatedFileDataManager = associatedFileDataManager;
        this.fileSystemObjectManager = fileSystemObjectManager;
    }

    @Scheduled(
            fixedRateString = "#{@applicationProperties.summaryRefreshHours * 3600000L}",
            initialDelayString = "#{@applicationProperties.summaryRefreshHours * 3600000L}"
    )
    public void refreshSummary() {
        LOG.info("Scheduled summary refresh (every {} hours).", applicationProperties.getSummaryRefreshHours());
        Summary.forceInstance(associatedFileDataManager, fileSystemObjectManager, applicationProperties);
    }
}
