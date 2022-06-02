package com.jbr.middletier.backup.integration;

/*
 * TODO - check here; also add the methods on the repository.
 * Things that need to be tested: https://jbrmmg.atlassian.net/wiki/spaces/~194851681/pages/9273345/Integration+Testing+Progress
 */

import com.jbr.middletier.MiddleTier;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testcontainers.containers.MySQLContainer;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {DatabaseIT.Initializer.class})
@ActiveProfiles(value="it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseIT {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseIT.class);

    @ClassRule
    public static MySQLContainer mysqlContainer = new MySQLContainer("mysql:8.0.28")
            .withDatabaseName("integration-tests-db")
            .withUsername("sa")
            .withPassword("sa");

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + mysqlContainer.getJdbcUrl(),
                    "spring.datasource.username=" + mysqlContainer.getUsername(),
                    "spring.datasource.password=" + mysqlContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @Test
    @Order(1)
    public void hardware() {
        // TODO - hardware test
    }

    @Test
    @Order(2)
    public void backup() {
        // TODO - hardware test
    }

    @Test
    @Order(3)
    public void location() {
        // TODO - hardware test
    }

    @Test
    @Order(4)
    public void classification() {
        // TODO - hardware test
    }

    @Test
    @Order(5)
    public void synchronise() {
        // TODO - hardware test
    }

    @Test
    @Order(6)
    public void action_confirm() {
        // TODO - hardware test
    }

    @Test
    @Order(7)
    public void synchronize() {
        // TODO - hardware test
    }
}
