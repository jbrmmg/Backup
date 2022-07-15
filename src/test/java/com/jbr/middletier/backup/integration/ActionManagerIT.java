package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.ActionConfirmRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dto.ActionConfirmDTO;
import com.jbr.middletier.backup.manager.ActionManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
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
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testcontainers.containers.MySQLContainer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;

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
        LOG.info("Check action confirm");

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

        Assert.assertEquals("DELETE", foundAction.get().get().getAction());
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

    @Test
    public void testConfirmAction() {
        FileSystem fileSystem = mock(FileSystem.class);
        ApplicationProperties properties = mock(ApplicationProperties.class);
        ActionConfirmRepository actionConfirmRepository = mock(ActionConfirmRepository.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);
        FileInfo fileInfo = mock(FileInfo.class);

        ConfirmActionRequest confirmActionRequest = mock(ConfirmActionRequest.class);
        when(confirmActionRequest.getConfirm()).thenReturn(false);

        when(fileInfo.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        ActionConfirm actionConfirm = mock(ActionConfirm.class);
        when(actionConfirm.getAction()).thenReturn(ActionConfirmType.AC_DELETE_DUPLICATE);
        when(actionConfirm.getPath()).thenReturn(fileInfo);

        Optional<ActionConfirm> optionalAction = Optional.of(actionConfirm);

        ActionManager actionManager = new ActionManager(properties,
                actionConfirmRepository,
                resourceLoader,
                fileSystemObjectManager, fileSystem);

        when(confirmActionRequest.getId()).thenReturn(1);

        when(actionConfirmRepository.findById(1)).thenReturn(optionalAction);

        actionManager.confirmAction(confirmActionRequest);
        verify(actionConfirmRepository, times(1)).deleteById(1);
    }

    @Test
    public void testConfirmActionCreate() {
        FileSystem fileSystem = mock(FileSystem.class);
        ApplicationProperties properties = mock(ApplicationProperties.class);
        ActionConfirmRepository actionConfirmRepository = mock(ActionConfirmRepository.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);

        FileInfo file = mock(FileInfo.class);
        when(file.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        ActionConfirm actionConfirm = mock(ActionConfirm.class);
        when(actionConfirm.getId()).thenReturn(1);
        when(actionConfirm.getPath()).thenReturn(file);
        when(actionConfirm.getAction()).thenReturn(ActionConfirmType.AC_DELETE_DUPLICATE);

        when(actionConfirmRepository.save(any(ActionConfirm.class))).thenReturn(actionConfirm);

        ActionManager actionManager = new ActionManager(properties,
                actionConfirmRepository,
                resourceLoader,
                fileSystemObjectManager, fileSystem);

        ActionConfirmDTO action = actionManager.createFileDeleteDuplicateAction(file);
        Assert.assertEquals("DELETE_DUP", action.getAction());
    }
}
