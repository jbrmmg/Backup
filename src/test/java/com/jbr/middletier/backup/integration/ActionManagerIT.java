package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.ActionConfirmRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dto.ActionConfirmDTO;
import com.jbr.middletier.backup.manager.ActionManager;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.modelmapper.ModelMapper;
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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;

@SpringBootTest(classes = MiddleTier.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@ContextConfiguration(initializers = {ActionManagerIT.Initializer.class})
@ActiveProfiles(value="it")
@Testcontainers
public class ActionManagerIT {
    private static final Logger LOG = LoggerFactory.getLogger(AfdmIT.class);

    @SuppressWarnings("rawtypes")
    @Container
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
    void actionConfirm() {
        LOG.info("Check action confirm");

        FileInfo newFile = new FileInfo();
        newFile.setName("Test File");
        newFile.setSize(10);

        fileRepository.save(newFile);
        int fileId = newFile.getIdAndType().getId();

        ActionConfirmDTO action = actionManager.createFileDeleteAction(newFile);

        AtomicReference<Optional<ActionConfirmDTO>> foundAction = new AtomicReference<>(Optional.empty());
        actionManager.externalFindByConfirmed(false).forEach(unconfirmedAction -> {
            if (unconfirmedAction.getId() == action.getId()) {
                foundAction.set(Optional.of(unconfirmedAction));
            }
        });
        assertTrue(foundAction.get().isPresent());

        assertEquals("DELETE", foundAction.get().get().getAction());
        assertNull(foundAction.get().get().getFlags());
        assertNull(foundAction.get().get().getParameter());
        assertEquals(false, foundAction.get().get().getParameterRequired());
        assertEquals(fileId, foundAction.get().get().getFileId());

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
        assertTrue(foundAction2.get().isPresent());

        assertEquals("HERE", foundAction2.get().get().getParameter());

        actionManager.deleteAllActions();

        AtomicReference<Optional<ActionConfirmDTO>> foundAction3 = new AtomicReference<>(Optional.empty());
        actionManager.externalFindByConfirmed(false).forEach(unconfirmedAction -> {
            if (unconfirmedAction.getId() == action.getId()) {
                foundAction3.set(Optional.of(unconfirmedAction));
            }
        });
        assertFalse(foundAction3.get().isPresent());
    }

    @Test
    void testConfirmAction() {
        ModelMapper modelMapper = mock(ModelMapper.class);
        FileSystem fileSystem = mock(FileSystem.class);
        ApplicationProperties properties = mock(ApplicationProperties.class);
        ActionConfirmRepository actionConfirmRepository = mock(ActionConfirmRepository.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);
        FileInfo fileInfo = mock(FileInfo.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);

        ConfirmActionRequest confirmActionRequest = mock(ConfirmActionRequest.class);
        when(confirmActionRequest.getConfirm()).thenReturn(false);

        when(fileInfo.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        ActionConfirm actionConfirm = mock(ActionConfirm.class);
        when(actionConfirm.getAction()).thenReturn(ActionConfirmType.AC_DELETE_DUPLICATE);
        when(actionConfirm.getPath()).thenReturn(fileInfo);

        Optional<ActionConfirm> optionalAction = Optional.of(actionConfirm);

        ActionManager localActionManager = new ActionManager(properties,
                actionConfirmRepository,
                resourceLoader,
                fileSystemObjectManager, associatedFileDataManager, fileSystem, modelMapper);

        when(confirmActionRequest.getId()).thenReturn(1);

        when(actionConfirmRepository.findById(1)).thenReturn(optionalAction);

        localActionManager.confirmAction(confirmActionRequest);
        verify(actionConfirmRepository, times(1)).deleteById(1);
    }

    @Test
    void testConfirmActionCreate() {
        ActionConfirmDTO actionConfirmDTO = mock(ActionConfirmDTO.class);
        when(actionConfirmDTO.getAction()).thenReturn("DELETE_DUP");

        ModelMapper modelMapper = mock(ModelMapper.class);
        when(modelMapper.map(any(ActionConfirm.class),any())).thenReturn(actionConfirmDTO);
        FileSystem fileSystem = mock(FileSystem.class);
        ApplicationProperties properties = mock(ApplicationProperties.class);
        ActionConfirmRepository actionConfirmRepository = mock(ActionConfirmRepository.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);

        FileInfo file = mock(FileInfo.class);
        when(file.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        ActionConfirm actionConfirm = mock(ActionConfirm.class);
        when(actionConfirm.getId()).thenReturn(1);
        when(actionConfirm.getPath()).thenReturn(file);
        when(actionConfirm.getAction()).thenReturn(ActionConfirmType.AC_DELETE_DUPLICATE);

        when(actionConfirmRepository.save(any(ActionConfirm.class))).thenReturn(actionConfirm);

        ActionManager localActionManager = new ActionManager(properties,
                actionConfirmRepository,
                resourceLoader,
                fileSystemObjectManager, associatedFileDataManager,
                fileSystem, modelMapper);

        ActionConfirmDTO action = localActionManager.createFileDeleteDuplicateAction(file);
        assertEquals("DELETE_DUP", action.getAction());
    }
}
