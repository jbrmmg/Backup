package com.jbr.middletier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by jason on 08/02/17.
 */

@SpringBootApplication
@EnableScheduling
public class MiddleTier {
    public static void main(String[] args) {
        SpringApplication.run(BackupConfig.class, args);
    }

}
