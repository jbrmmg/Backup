package com.jbr.middletier.backup.health;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by jason on 26/04/17.
 */

@Component
public class ServiceHealthIndicator implements HealthIndicator {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceHealthIndicator.class);

    private final ApplicationProperties applicationProperties;

    private final
    BackupRepository backupRepository;

    @Autowired
    public ServiceHealthIndicator(BackupRepository backupRepository,
                                  ApplicationProperties applicationProperties) {
        this.backupRepository = backupRepository;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public Health health() {
        try {
            List<Backup> backupList = (List<Backup>) backupRepository.findAll();
            LOG.debug("Check Database {}.", backupList.size());

            return Health.up().withDetail("service", applicationProperties.getServiceName()).withDetail("Backup Types",backupList.size()).build();
        } catch (Exception e) {
            LOG.error("Failed to check health",e);
        }

        return Health.down().withDetail("service", applicationProperties.getServiceName()).build();
    }
}
