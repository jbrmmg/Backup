package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.ActionConfirm;
import com.jbr.middletier.backup.data.ActionConfirmType;
import com.jbr.middletier.backup.data.ConfirmActionRequest;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dto.ActionConfirmDTO;
import com.jbr.middletier.backup.manager.ActionManager;
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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {ActionManagerIT.Initializer.class})
@ActiveProfiles(value="it")
public class ActionManagerIT {
    private static final Logger LOG = LoggerFactory.getLogger(AfdmIT.class);

    @SuppressWarnings("rawtypes")
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
    FileRepository fileRepository;

    @Autowired
    ActionManager actionManager;

    @Test
    public void actionConfirm() {
        FileInfo newFile = new FileInfo();
        newFile.setName("Test File");
        newFile.setSize(10);
        newFile.clearRemoved();

        fileRepository.save(newFile);
        int fileId = newFile.getIdAndType().getId();

        ActionConfirmDTO action = actionManager.createFileDeleteAction(newFile);

        AtomicReference<Optional<ActionConfirmDTO>> foundAction = new AtomicReference<>(Optional.empty());
        actionManager.externalFindByConfirmed(false).forEach(unconfirmedAction -> {
            if (unconfirmedAction.getId() == action.getId()) {
                foundAction.set(Optional.of(unconfirmedAction));
            }
        });
        Assert.assertTrue(foundAction.get().isPresent());

        Assert.assertEquals("DELETE", foundAction.get().get().getAction().getTypeName());
        Assert.assertNull(foundAction.get().get().getFlags());
        Assert.assertNull(foundAction.get().get().getParameter());
        Assert.assertEquals(false, foundAction.get().get().getParameterRequired());
        Assert.assertEquals(fileId, foundAction.get().get().getFileId());

        ConfirmActionRequest request = new ConfirmActionRequest();
        request.setConfirm(true);
        request.setParameter("HERE");
        request.setId(foundAction.get().get().getId());

        actionManager.confirmAction(request);

        AtomicReference<Optional<ActionConfirmDTO>> foundAction2 = new AtomicReference<>(Optional.empty());
        actionManager.externalFindByConfirmed(true).forEach(confirmedAction -> {
            if (confirmedAction.getId() == action.getId()) {
                foundAction2.set(Optional.of(confirmedAction));
            }
        });
        Assert.assertTrue(foundAction2.get().isPresent());

        Assert.assertEquals("HERE", foundAction2.get().get().getParameter());

        actionManager.deleteAllActions();

        AtomicReference<Optional<ActionConfirmDTO>> foundAction3 = new AtomicReference<>(Optional.empty());
        actionManager.externalFindByConfirmed(false).forEach(unconfirmedAction -> {
            if (unconfirmedAction.getId() == action.getId()) {
                foundAction3.set(Optional.of(unconfirmedAction));
            }
        });
        Assert.assertFalse(foundAction3.get().isPresent());
    }
}
