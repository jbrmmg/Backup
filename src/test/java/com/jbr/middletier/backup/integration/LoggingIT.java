package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.WebTester;
import com.jbr.middletier.backup.data.DbLogType;
import com.jbr.middletier.backup.manager.DbLoggingManager;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testcontainers.containers.MySQLContainer;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("rawtypes")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {LoggingIT.Initializer.class})
@ActiveProfiles(value="it")
public class LoggingIT extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingIT.class);

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

    @Autowired
    DbLoggingManager dbLoggingManager;

    @Test
    public void basicTest() throws Exception {
        LOG.info("Basic logging test.");

        dbLoggingManager.debug("Debug message");
        dbLoggingManager.info("Information message");
        dbLoggingManager.warn("Warning message");
        dbLoggingManager.error("Error message");

        getMockMvc().perform(get("/jbr/int/backup/log")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].type", is("Info")))
                .andExpect(jsonPath("$[0].message", is("Starting up")))
                .andExpect(jsonPath("$[1].type", is("Debug")))
                .andExpect(jsonPath("$[1].message", is("Debug message")))
                .andExpect(jsonPath("$[2].type", is("Info")))
                .andExpect(jsonPath("$[2].message", is("Information message")))
                .andExpect(jsonPath("$[3].type", is("Warning")))
                .andExpect(jsonPath("$[3].message", is("Warning message")))
                .andExpect(jsonPath("$[4].type", is("Error")))
                .andExpect(jsonPath("$[4].message", is("Error message")));
    }

    @Test
    public void testCache() {
        LOG.info("Cache test");

        dbLoggingManager.clearMessageCache();

        dbLoggingManager.removeOldLogs();

        dbLoggingManager.error("Error Message");

        Assert.assertEquals(1, dbLoggingManager.getMessageCache(DbLogType.DLT_ERROR).size());
    }
}
