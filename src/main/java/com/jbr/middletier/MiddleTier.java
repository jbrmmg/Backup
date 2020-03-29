package com.jbr.middletier;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.config.DefaultProfileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by jason on 08/02/17.
 */

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({LiquibaseProperties.class, ApplicationProperties.class})
public class MiddleTier {
    private static final Logger log = LoggerFactory.getLogger(MiddleTier.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MiddleTier.class);
        DefaultProfileUtil.addDefaultProfile(app);
        app.run(args);
    }

}
