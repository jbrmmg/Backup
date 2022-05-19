package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {IntegrationTestIT.Initializer.class})
@ActiveProfiles(value="it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntegrationTestIT extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestIT.class);

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
    public void source() throws Exception {
        LOG.info("Source Testing");

        // Use the default location (main drive).
        LocationDTO location1 = new LocationDTO();
        location1.setId(1);

        SourceDTO source = new SourceDTO();
        source.setPath("/target/testfiles/gather1");
        source.setLocation(location1);
        source.setFilter("filter");
        source.setStatus("OK");

        LOG.info("Create a source.");
        getMockMvc().perform(post("/jbr/ext/backup/source")
                        .content(this.json(source))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        LOG.info("Expect that the id is 1000000 - as that is the first.");
        getMockMvc().perform(get("/jbr/ext/backup/source")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id",is(1000000)))
                .andExpect(jsonPath("$[0].path",is("/target/testfiles/gather1")))
                .andExpect(jsonPath("$[0].filter",is("filter")))
                .andExpect(jsonPath("$[0].status",is("OK")))
                .andExpect(jsonPath("$[0].location.id",is(1)));

        // Perform an update.
        LocationDTO location2 = new LocationDTO();
        location2.setId(2);

        source.setId(1000000);
        source.setPath("/target/testfiles/gather2");
        source.setLocation(location2);
        source.setStatus("ERROR");
        source.setFilter("filter2");

        LOG.info("Update the source.");
        getMockMvc().perform(put("/jbr/ext/backup/source")
                        .content(this.json(source))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Check the update.
        getMockMvc().perform(get("/jbr/ext/backup/source")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id",is(1000000)))
                .andExpect(jsonPath("$[0].path",is("/target/testfiles/gather2")))
                .andExpect(jsonPath("$[0].filter",is("filter2")))
                .andExpect(jsonPath("$[0].status",is("ERROR")))
                .andExpect(jsonPath("$[0].location.id",is(2)));

        // Create a second source.
        LOG.info("Create another source.");
        source = new SourceDTO();
        source.setPath("/target/testfiles/gather3");
        source.setLocation(location1);
        source.setFilter("");
        source.setStatus("OK");

        getMockMvc().perform(post("/jbr/ext/backup/source")
                        .content(this.json(source))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/ext/backup/source")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Delete the original.
        LOG.info("Delete the original source.");
        source = new SourceDTO();
        source.setId(1000000);

        getMockMvc().perform(delete("/jbr/ext/backup/source")
                        .content(this.json(source))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/ext/backup/source")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id",is(1000001)));

    }
}
