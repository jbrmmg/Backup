package com.jbr.middletier.backup.integration;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.WebTester;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
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

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {EmailApiIT.Initializer.class})
@ActiveProfiles(value="it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmailApiIT extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(EmailApiIT.class);

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
    ActionConfirmRepository actionConfirmRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    DirectoryRepository directoryRepository;

    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    LocationRepository locationRepository;

    private GreenMail testSMTP;

    @Before
    public void setupSMTP() {
        testSMTP = new GreenMail(ServerSetupTest.SMTP);
        testSMTP.start();
    }

    @Test
    public void TestEmail() {
        try {
            Optional<Location> location = locationRepository.findById(1);
            assertTrue(location.isPresent());

            LOG.info("Location {}", location.get());

            Source source = new Source();
            source.setPath("/");
            source.setLocation(location.get());
            sourceRepository.save(source);

            LOG.info("Source {}", source);

            DirectoryInfo directory = new DirectoryInfo();
            directory.setParentId(source);
            directory.setName("");
            directory.clearRemoved();
            directoryRepository.save(directory);

            LOG.info("Directory Info {}", directory);

            FileInfo file = new FileInfo();
            file.setName("Test");
            file.setParentId(directory);
            file.clearRemoved();
            fileRepository.save(file);

            ActionConfirm action = new ActionConfirm();
            action.setAction("Test");
            action.setConfirmed(false);
            action.setFileInfo(file);
            actionConfirmRepository.save(action);

            getMockMvc().perform(post("/jbr/int/backup/actionemail")
                            .contentType(getContentType()))
                    .andExpect(status().isOk());

            actionConfirmRepository.deleteAll();
            fileRepository.deleteAll();
            directoryRepository.deleteAll();
            sourceRepository.deleteAll();
        } catch (Exception ex) {
            fail();
        }
    }
}
