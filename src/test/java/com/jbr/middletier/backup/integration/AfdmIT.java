package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.dto.SynchronizeDTO;
import com.jbr.middletier.backup.exception.*;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = MiddleTier.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@ContextConfiguration(initializers = {AfdmIT.Initializer.class})
@ActiveProfiles(value="it")
@Testcontainers
public class AfdmIT {
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
    AssociatedFileDataManager associatedFileDataManager;

    @Test
    void location() throws LocationAlreadyExistsException, InvalidLocationIdException {
        LOG.info("Testing location new, amend, delete");

        LocationDTO newLocation = new LocationDTO();
        newLocation.setId(1000);
        newLocation.setName("Test");
        newLocation.setSize("1GB");

        associatedFileDataManager.createLocation(associatedFileDataManager.convertToEntity(newLocation));

        Optional<Location> findLocation = associatedFileDataManager.findLocationById(1000);
        assertTrue(findLocation.isPresent());

        LocationDTO updateLocation = associatedFileDataManager.convertToDTO(findLocation.get());
        updateLocation.setName("Test 2");

        associatedFileDataManager.updateLocation(associatedFileDataManager.convertToEntity(updateLocation));

        Optional<Location> findLocation2 = associatedFileDataManager.findLocationById(1000);
        assertTrue(findLocation2.isPresent());

        assertEquals("Test 2", findLocation2.get().getName());
        assertEquals("1GB", findLocation2.get().getSize());

        associatedFileDataManager.deleteLocation(findLocation2.get());

        findLocation2 = associatedFileDataManager.findLocationById(1000);
        assertFalse(findLocation2.isPresent());
    }

    @Test
    void classification() throws ClassificationIdException, InvalidClassificationIdException {
        LOG.info("Testing classification new, amend, delete");

        ClassificationDTO newDTO = new ClassificationDTO();
        newDTO.setAction(ClassificationActionType.CA_BACKUP);
        newDTO.setIsImage(false);
        newDTO.setIcon("Fred");
        newDTO.setOrder(1);
        newDTO.setRegex("x");
        newDTO.setIsVideo(false);

        Classification newClassification = associatedFileDataManager.createClassification(associatedFileDataManager.convertToEntity(newDTO));

        AtomicReference<Optional<Classification>> findClassification = new AtomicReference<>(Optional.empty());
        associatedFileDataManager.findAllClassifications().forEach(classification -> {
            if(classification.getId().equals(newClassification.getId())) {
                findClassification.set(Optional.of(classification));
            }
        });
        assertTrue(findClassification.get().isPresent());
        assertEquals("BACKUP", findClassification.get().get().getAction().getTypeName());
        assertEquals("Fred", findClassification.get().get().getIcon());
        assertEquals("x", findClassification.get().get().getRegex());
        assertEquals(false, findClassification.get().get().getIsImage());
        assertEquals(false, findClassification.get().get().getIsVideo());

        associatedFileDataManager.deleteClassification(findClassification.get().get());

        findClassification.set(Optional.empty());
        associatedFileDataManager.findAllClassifications().forEach(classification -> {
            if(classification.getId().equals(newClassification.getId())) {
                findClassification.set(Optional.of(classification));
            }
        });
        assertFalse(findClassification.get().isPresent());
    }

    @Test
    void synchronize() throws LocationAlreadyExistsException, SourceAlreadyExistsException, InvalidSourceIdException, SynchronizeAlreadyExistsException, InvalidSynchronizeIdException, InvalidLocationIdException {
        LOG.info("Testing synchronize new, amend, delete");

        LocationDTO newLocation = new LocationDTO();
        newLocation.setId(1000);
        newLocation.setName("Test");
        newLocation.setName("1GB");
        associatedFileDataManager.createLocation(associatedFileDataManager.convertToEntity(newLocation));

        SourceDTO newSource1 = new SourceDTO();
        newSource1.setLocation(newLocation);
        newSource1.setStatus("OK");
        newSource1.setFilter("*.xml");
        newSource1.setPath("/test/directory");
        Source createdSource1 = associatedFileDataManager.createSource(associatedFileDataManager.convertToEntity(newSource1));
        newSource1 = associatedFileDataManager.convertToDTO(createdSource1);

        SourceDTO newSource2 = new SourceDTO();
        newSource2.setLocation(newLocation);
        newSource2.setStatus("OK");
        newSource2.setFilter("*.xml");
        newSource2.setPath("/test/directory2");
        Source createdSource2 = associatedFileDataManager.createSource(associatedFileDataManager.convertToEntity(newSource2));
        newSource2 = associatedFileDataManager.convertToDTO(createdSource2);

        SynchronizeDTO newSync = new SynchronizeDTO();
        newSync.setId(1000);
        newSync.setDestination(newSource1);
        newSync.setSource(newSource2);

        associatedFileDataManager.createSynchronize(associatedFileDataManager.convertToEntity(newSync));

        AtomicReference<Optional<Synchronize>> findSync = new AtomicReference<>(Optional.empty());
        associatedFileDataManager.findAllSynchronize().forEach(synchronize -> {
            if(synchronize.getId().equals(newSync.getId())) {
                findSync.set(Optional.of(synchronize));
            }
        });
        assertTrue(findSync.get().isPresent());

        assertEquals("/test/directory", findSync.get().get().getDestination().getPath());
        assertEquals("/test/directory2", findSync.get().get().getSource().getPath());

        findSync.get().get().setDestination(createdSource2);
        findSync.get().get().setSource(createdSource1);
        associatedFileDataManager.updateSynchronize(findSync.get().get());

        AtomicReference<Optional<Synchronize>> findSync2 = new AtomicReference<>(Optional.empty());
        associatedFileDataManager.findAllSynchronize().forEach(synchronize -> {
            if(synchronize.getId().equals(newSync.getId())) {
                findSync2.set(Optional.of(synchronize));
            }
        });
        assertTrue(findSync2.get().isPresent());

        assertEquals("/test/directory2", findSync2.get().get().getDestination().getPath());
        assertEquals("/test/directory", findSync2.get().get().getSource().getPath());

        associatedFileDataManager.deleteSynchronize(findSync2.get().get());
        findSync2.set(Optional.empty());
        for(Synchronize nextSync : associatedFileDataManager.findAllSynchronize() ) {
            if(nextSync.getId().equals(newSync.getId())) {
                findSync2.set(Optional.of(nextSync));
            }
        }
        assertFalse(findSync2.get().isPresent());

        associatedFileDataManager.deleteSource(createdSource1);
        associatedFileDataManager.deleteSource(createdSource2);
        associatedFileDataManager.deleteLocation(associatedFileDataManager.convertToEntity(newLocation));
    }

    @Test
    void updateSynchronizeTest() {
        Synchronize synchronize = mock(Synchronize.class);
        when(synchronize.getId()).thenReturn(1);

        ModelMapper modelMapper = mock(ModelMapper.class);
        SourceRepository sourceRepository = mock(SourceRepository.class);
        PreImportSourceRepository preImportSourceRepository = mock(PreImportSourceRepository.class);
        PostImportSourceRepository postImportSourceRepository = mock(PostImportSourceRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ClassificationRepository classificationRepository = mock(ClassificationRepository.class);
        SynchronizeRepository synchronizeRepository = mock(SynchronizeRepository.class);
        ImportSourceRepository importSourceRepository = mock(ImportSourceRepository.class);
        when(synchronizeRepository.findById(1)).thenReturn(Optional.empty());

        AssociatedFileDataManager testAFDM = new AssociatedFileDataManager(sourceRepository,
                locationRepository,
                classificationRepository,
                synchronizeRepository,
                importSourceRepository,
                preImportSourceRepository,
                postImportSourceRepository,
                modelMapper);

        try {
            testAFDM.updateSynchronize(synchronize);
            fail();
        } catch (InvalidSynchronizeIdException e) {
            assertEquals("Synchronize with id (1) not found.", e.getMessage());
        }
    }

    @Test
    void sourceStatusTest() {
        Source testSource = new Source();

        ModelMapper modelMapper = mock(ModelMapper.class);
        SourceRepository sourceRepository = mock(SourceRepository.class);
        PreImportSourceRepository preImportSourceRepository = mock(PreImportSourceRepository.class);
        PostImportSourceRepository postImportSourceRepository = mock(PostImportSourceRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ClassificationRepository classificationRepository = mock(ClassificationRepository.class);
        SynchronizeRepository synchronizeRepository = mock(SynchronizeRepository.class);
        ImportSourceRepository importSourceRepository = mock(ImportSourceRepository.class);
        when(sourceRepository.save(testSource)).thenThrow(NullPointerException.class);

        AssociatedFileDataManager testAFDM = new AssociatedFileDataManager(sourceRepository,
                locationRepository,
                classificationRepository,
                synchronizeRepository,
                importSourceRepository, preImportSourceRepository, postImportSourceRepository, modelMapper);

        try {
            testAFDM.updateSourceStatus(testSource, SourceStatusType.SST_OK);
        } catch (Exception e) {
            // Should not propagate the exception.
            fail();
        }
    }
}
