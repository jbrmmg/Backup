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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {AfdmIT.Initializer.class})
@ActiveProfiles(value="it")
public class AfdmIT {
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
    AssociatedFileDataManager associatedFileDataManager;

    @Test
    public void location() throws LocationAlreadyExistsException, InvalidLocationIdException {
        LOG.info("Testing location new, amend, delete");

        LocationDTO newLocation = new LocationDTO();
        newLocation.setId(1000);
        newLocation.setName("Test");
        newLocation.setSize("1GB");

        associatedFileDataManager.createLocation(newLocation);

        Optional<Location> findLocation = associatedFileDataManager.findLocationById(1000);
        Assert.assertTrue(findLocation.isPresent());

        LocationDTO updateLocation = new LocationDTO(findLocation.get());
        updateLocation.setName("Test 2");

        associatedFileDataManager.updateLocation(updateLocation);

        Optional<Location> findLocation2 = associatedFileDataManager.findLocationById(1000);
        Assert.assertTrue(findLocation2.isPresent());

        Assert.assertEquals("Test 2", findLocation2.get().getName());
        Assert.assertEquals("1GB", findLocation2.get().getSize());

        associatedFileDataManager.deleteLocation(updateLocation);

        findLocation2 = associatedFileDataManager.findLocationById(1000);
        Assert.assertFalse(findLocation2.isPresent());
    }

    @Test
    public void classification() throws ClassificationIdException, InvalidClassificationIdException {
        LOG.info("Testing classification new, amend, delete");

        ClassificationDTO newDTO = new ClassificationDTO();
        newDTO.setAction(ClassificationActionType.CA_BACKUP);
        newDTO.setImage(false);
        newDTO.setIcon("Fred");
        newDTO.setOrder(1);
        newDTO.setRegex("x");
        newDTO.setUseMD5(true);
        newDTO.setVideo(false);

        Classification newClassification = associatedFileDataManager.createClassification(newDTO);

        AtomicReference<Optional<Classification>> findClassification = new AtomicReference<>(Optional.empty());
        associatedFileDataManager.internalFindAllClassification().forEach(classification -> {
            if(classification.getId().equals(newClassification.getId())) {
                findClassification.set(Optional.of(classification));
            }
        });
        Assert.assertTrue(findClassification.get().isPresent());
        Assert.assertEquals("BACKUP", findClassification.get().get().getAction().getTypeName());
        Assert.assertEquals("Fred", findClassification.get().get().getIcon());
        Assert.assertEquals("x", findClassification.get().get().getRegex());
        Assert.assertEquals(false, findClassification.get().get().getIsImage());
        Assert.assertEquals(false, findClassification.get().get().getIsVideo());
        Assert.assertEquals(true, findClassification.get().get().getUseMD5());

        ClassificationDTO classificationForDelete = new ClassificationDTO(findClassification.get().get());
        associatedFileDataManager.deleteClassification(classificationForDelete);

        findClassification.set(Optional.empty());
        associatedFileDataManager.internalFindAllClassification().forEach(classification -> {
            if(classification.getId().equals(newClassification.getId())) {
                findClassification.set(Optional.of(classification));
            }
        });
        Assert.assertFalse(findClassification.get().isPresent());
    }

    @Test
    public void synchronize() throws LocationAlreadyExistsException, SourceAlreadyExistsException, InvalidSourceIdException, SynchronizeAlreadyExistsException, InvalidSynchronizeIdException, InvalidLocationIdException {
        LOG.info("Testing synchronize new, amend, delete");

        LocationDTO newLocation = new LocationDTO();
        newLocation.setId(1000);
        newLocation.setName("Test");
        newLocation.setName("1GB");
        associatedFileDataManager.createLocation(newLocation);

        SourceDTO newSource1 = new SourceDTO();
        newSource1.setLocation(newLocation);
        newSource1.setStatus(SourceStatusType.SST_OK);
        newSource1.setFilter("*.xml");
        newSource1.setPath("/test/directory");
        Source createdSource1 = associatedFileDataManager.createSource(newSource1);

        SourceDTO newSource2 = new SourceDTO();
        newSource2.setLocation(newLocation);
        newSource2.setStatus(SourceStatusType.SST_OK);
        newSource2.setFilter("*.xml");
        newSource2.setPath("/test/directory2");
        Source createdSource2 = associatedFileDataManager.createSource(newSource2);

        SynchronizeDTO newSync = new SynchronizeDTO();
        newSync.setId(1000);
        newSync.setDestination(new SourceDTO(createdSource1));
        newSync.setSource(new SourceDTO(createdSource2));

        associatedFileDataManager.createSynchronize(newSync);

        AtomicReference<Optional<Synchronize>> findSync = new AtomicReference<>(Optional.empty());
        associatedFileDataManager.internalFindAllSynchronize().forEach(synchronize -> {
            if(synchronize.getId().equals(newSync.getId())) {
                findSync.set(Optional.of(synchronize));
            }
        });
        Assert.assertTrue(findSync.get().isPresent());

        Assert.assertEquals("/test/directory", findSync.get().get().getDestination().getPath());
        Assert.assertEquals("/test/directory2", findSync.get().get().getSource().getPath());

        findSync.get().get().setDestination(createdSource2);
        findSync.get().get().setSource(createdSource1);
        associatedFileDataManager.updateSynchronize(new SynchronizeDTO(findSync.get().get()));

        AtomicReference<Optional<Synchronize>> findSync2 = new AtomicReference<>(Optional.empty());
        associatedFileDataManager.internalFindAllSynchronize().forEach(synchronize -> {
            if(synchronize.getId().equals(newSync.getId())) {
                findSync2.set(Optional.of(synchronize));
            }
        });
        Assert.assertTrue(findSync2.get().isPresent());

        Assert.assertEquals("/test/directory2", findSync2.get().get().getDestination().getPath());
        Assert.assertEquals("/test/directory", findSync2.get().get().getSource().getPath());

        associatedFileDataManager.deleteSynchronize(new SynchronizeDTO(findSync2.get().get()));
        findSync2.set(Optional.empty());
        for(Synchronize nextSync : associatedFileDataManager.internalFindAllSynchronize() ) {
            if(nextSync.getId().equals(newSync.getId())) {
                findSync2.set(Optional.of(nextSync));
            }
        }
        Assert.assertFalse(findSync2.get().isPresent());

        associatedFileDataManager.deleteSource(new SourceDTO(createdSource1));
        associatedFileDataManager.deleteSource(new SourceDTO(createdSource2));
        associatedFileDataManager.deleteLocation(newLocation);
    }

    @Test
    public void updateSynchronizeTest() {
        SynchronizeDTO synchronizeDTO = mock(SynchronizeDTO.class);
        when(synchronizeDTO.getId()).thenReturn(1);

        SourceRepository sourceRepository = mock(SourceRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ClassificationRepository classificationRepository = mock(ClassificationRepository.class);
        SynchronizeRepository synchronizeRepository = mock(SynchronizeRepository.class);
        ImportSourceRepository importSourceRepository = mock(ImportSourceRepository.class);
        when(synchronizeRepository.findById(1)).thenReturn(Optional.empty());

        AssociatedFileDataManager testAFDM = new AssociatedFileDataManager(sourceRepository,
                locationRepository,
                classificationRepository,
                synchronizeRepository,
                importSourceRepository );

        try {
            testAFDM.updateSynchronize(synchronizeDTO);
            Assert.fail();
        } catch (InvalidSynchronizeIdException e) {
            Assert.assertEquals("Synchronize with id (1) not found.", e.getMessage());
        } catch (InvalidSourceIdException e2) {
            Assert.fail();
        }
    }

    @Test
    public void sourceStatusTest() {
        Source testSource = new Source();

        SourceRepository sourceRepository = mock(SourceRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ClassificationRepository classificationRepository = mock(ClassificationRepository.class);
        SynchronizeRepository synchronizeRepository = mock(SynchronizeRepository.class);
        ImportSourceRepository importSourceRepository = mock(ImportSourceRepository.class);
        when(sourceRepository.save(testSource)).thenThrow(NullPointerException.class);

        AssociatedFileDataManager testAFDM = new AssociatedFileDataManager(sourceRepository,
                locationRepository,
                classificationRepository,
                synchronizeRepository,
                importSourceRepository );

        try {
            testAFDM.updateSourceStatus(testSource, SourceStatusType.SST_OK);
        } catch (Exception e) {
            // Should not propagate the exception.
            Assert.fail();
        }
    }
}
