package com.jbr.middletier.backup.integration;

/*
 * TODO - check here; also add the methods on the repository.
 * Things that need to be tested: https://jbrmmg.atlassian.net/wiki/spaces/~194851681/pages/9273345/Integration+Testing+Progress
 */

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.BackupDTO;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import org.junit.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
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

    @Autowired
    HardwareRepository hardwareRepository;

    @Autowired
    BackupRepository backupRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    ClassificationRepository classificationRepository;

    @Autowired
    ActionConfirmRepository actionConfirmRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    SynchronizeRepository synchronizeRepository;

    @Autowired
    SourceRepository sourceRepository;

    @Test
    @Order(1)
    public void hardware(){
        LOG.info("Basic test of the Hardware object");
        String id = "EF:C9:99:92:93:93";

        Hardware newHardware = new Hardware();
        newHardware.setMacAddress(id);
        newHardware.setIP("123.123.123.123");
        newHardware.setName("Test");
        newHardware.setReservedIP("Y");

        hardwareRepository.save(newHardware);

        Optional<Hardware> findHardware = hardwareRepository.findById(id);
        Assert.assertTrue(findHardware.isPresent());

        findHardware.get().setName("Test2");
        hardwareRepository.save(findHardware.get());

        Optional<Hardware> findHardware2 = hardwareRepository.findById(id);
        Assert.assertTrue(findHardware2.isPresent());
        Assert.assertEquals("123.123.123.123", findHardware2.get().getIP());
        Assert.assertEquals("Test2", findHardware2.get().getName());
        Assert.assertEquals("Y", findHardware2.get().getReservedIP());

        hardwareRepository.delete(findHardware2.get());
        findHardware2 = hardwareRepository.findById(id);
        Assert.assertFalse(findHardware2.isPresent());
    }

    @Test
    @Order(2)
    public void backup() {
        LOG.info("Basic test of the backup object");

        BackupDTO newBackupDTO = new BackupDTO();
        newBackupDTO.setId("XXXX");
        newBackupDTO.setBackupName("Test");
        newBackupDTO.setDirectory("/test/directory");
        newBackupDTO.setType("Git");
        newBackupDTO.setArtifact("");
        newBackupDTO.setFileName("Dev");
        newBackupDTO.setTime(100);

        Backup newBackup = new Backup(newBackupDTO);
        backupRepository.save(newBackup);

        Optional<Backup> findBackup = backupRepository.findById("XXXX");
        Assert.assertTrue(findBackup.isPresent());

        findBackup.get().setDirectory("/test/directory2");
        backupRepository.save(findBackup.get());

        Optional<Backup> findBackup2 = backupRepository.findById("XXXX");
        Assert.assertTrue(findBackup2.isPresent());
        Assert.assertEquals("/test/directory2", findBackup2.get().getDirectory());
        Assert.assertEquals("Test", findBackup2.get().getBackupName());
        Assert.assertEquals("XXXX", findBackup2.get().getId());
        Assert.assertEquals("Dev", findBackup2.get().getFileName());
        Assert.assertEquals(100, findBackup2.get().getTime());
        Assert.assertEquals("Git", findBackup2.get().getType());

        backupRepository.delete(findBackup.get());
        findBackup2 = backupRepository.findById("XXXX");
        Assert.assertFalse(findBackup2.isPresent());
    }

    @Test
    @Order(3)
    public void location() {
        Location newLocation = new Location();
        newLocation.setId(1000);
        newLocation.setName("Test");
        newLocation.setSize("1GB");

        locationRepository.save(newLocation);

        Optional<Location> findLocation = locationRepository.findById(1000);
        Assert.assertTrue(findLocation.isPresent());

        findLocation.get().setName("Test 2");
        locationRepository.save(findLocation.get());

        Optional<Location> findLocation2 = locationRepository.findById(1000);
        Assert.assertTrue(findLocation2.isPresent());

        Assert.assertEquals("Test 2", findLocation2.get().getName());
        Assert.assertEquals("1GB", findLocation2.get().getSize());

        locationRepository.delete(findLocation2.get());
        findLocation2 = locationRepository.findById(1000);
        Assert.assertFalse(findLocation2.isPresent());
    }

    @Test
    @Order(4)
    public void classification() {
        ClassificationDTO newDTO = new ClassificationDTO();
        newDTO.setAction("Test");
        newDTO.setType("File");
        newDTO.setImage(false);
        newDTO.setIcon("Fred");
        newDTO.setOrder(1);
        newDTO.setRegex("x");
        newDTO.setUseMD5(true);
        newDTO.setVideo(false);

        Classification newClassification = new Classification(newDTO);
        classificationRepository.save(newClassification);

        int id = newClassification.getId();

        Optional<Classification> findClassification = classificationRepository.findById(id);
        Assert.assertTrue(findClassification.isPresent());
        Assert.assertEquals("Test", findClassification.get().getAction());
        Assert.assertEquals("Fred", findClassification.get().getIcon());
        Assert.assertEquals("x", findClassification.get().getRegex());
        Assert.assertEquals(false, findClassification.get().getIsImage());
        Assert.assertEquals(false, findClassification.get().getIsVideo());
        Assert.assertEquals(true, findClassification.get().getUseMD5());

        classificationRepository.delete(findClassification.get());

        findClassification = classificationRepository.findById(id);
        Assert.assertFalse(findClassification.isPresent());
    }

    @Test
    @Order(6)
    @Ignore
    public void action_confirm() {
        FileInfo newFile = new FileInfo();
        newFile.setName("Test File");
        newFile.setSize(10);

        fileRepository.save(newFile);
        int fileId = newFile.getIdAndType().getId();

        ActionConfirm actionConfirm = new ActionConfirm();
        actionConfirm.setAction("Test");
        actionConfirm.setConfirmed(true);
        actionConfirm.setFileInfo(newFile);
        actionConfirm.setFlags("x");
        actionConfirm.setParameter("x1");
        actionConfirm.setParameterRequired(true);
        actionConfirmRepository.save(actionConfirm);

        int id = actionConfirm.getId();

        Optional<ActionConfirm> findActionConfirm = actionConfirmRepository.findById(id);
        Assert.assertTrue(findActionConfirm.isPresent());

        Assert.assertEquals("Test", findActionConfirm.get().getAction());
        Assert.assertEquals("x", findActionConfirm.get().getFlags());
        Assert.assertEquals("x1", findActionConfirm.get().getParameter());
        Assert.assertEquals(true, findActionConfirm.get().getParameterRequired());
        Assert.assertEquals(fileId, findActionConfirm.get().getPath().getParentId().getId());

        findActionConfirm.get().setParameter("x2");
        actionConfirmRepository.save(findActionConfirm.get());

        Optional<ActionConfirm> findActionConfirm2 = actionConfirmRepository.findById(id);
        Assert.assertTrue(findActionConfirm2.isPresent());

        Assert.assertEquals("x2", findActionConfirm.get().getParameter());

        actionConfirmRepository.delete(findActionConfirm2.get());

        findActionConfirm2 = actionConfirmRepository.findById(id);
        Assert.assertFalse(findActionConfirm2.isPresent());

        fileRepository.delete(newFile);
    }

    @Test
    @Order(7)
    public void synchronize() {
        Location newLocation = new Location();
        newLocation.setId(1000);
        newLocation.setName("Test");
        newLocation.setName("1GB");
        locationRepository.save(newLocation);

        Source newSource1 = new Source();
        newSource1.setLocation(newLocation);
        newSource1.setStatus("OK");
        newSource1.setFilter("*.xml");
        newSource1.setPath("/test/directory");
        sourceRepository.save(newSource1);

        Source newSource2 = new Source();
        newSource2.setLocation(newLocation);
        newSource2.setStatus("OK");
        newSource2.setFilter("*.xml");
        newSource2.setPath("/test/directory2");
        sourceRepository.save(newSource2);

        Synchronize newSync = new Synchronize();
        newSync.setId(1000);
        newSync.setDestination(newSource1);
        newSync.setSource(newSource2);

        synchronizeRepository.save(newSync);

        Optional<Synchronize> findSync = synchronizeRepository.findById(1000);
        Assert.assertTrue(findSync.isPresent());

        Assert.assertEquals("/test/directory", findSync.get().getDestination().getPath());
        Assert.assertEquals("/test/directory2", findSync.get().getSource().getPath());

        findSync.get().setDestination(newSource2);
        findSync.get().setSource(newSource1);
        synchronizeRepository.save(findSync.get());

        Optional<Synchronize> findSync2 = synchronizeRepository.findById(1000);
        Assert.assertTrue(findSync2.isPresent());

        Assert.assertEquals("/test/directory2", findSync2.get().getDestination().getPath());
        Assert.assertEquals("/test/directory", findSync2.get().getSource().getPath());

        synchronizeRepository.delete(findSync2.get());
        findSync2 = synchronizeRepository.findById(1000);
        Assert.assertFalse(findSync2.isPresent());

        sourceRepository.delete(newSource1);
        sourceRepository.delete(newSource2);
        locationRepository.delete(newLocation);
    }
}
