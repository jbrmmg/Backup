package com.jbr.middletier.backup.health;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by jason on 26/04/17.
 */

@Component
public class ServiceHealthIndicator implements HealthIndicator {
    final static private Logger LOG = LoggerFactory.getLogger(ServiceHealthIndicator.class);

    @Value("${middle.tier.service.name}")
    private String serviceName;

    private final
    BackupRepository backupRepository;

    @Autowired
    public ServiceHealthIndicator(BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }

    @Override
    public Health health() {
        try {
            List<Backup> backupList = (List<Backup>) backupRepository.findAll();
            LOG.info(String.format("Check Database %s.", backupList.size()));

            return Health.up().withDetail("service", serviceName).withDetail("Backup Types",backupList.size()).build();
        } catch (Exception e) {
            LOG.error("Failed to check health",e);
        }

        return Health.down().withDetail("service", serviceName).build();
    }
}
