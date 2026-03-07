package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.BackupDTO;
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

@SpringBootTest(classes = MiddleTier.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@ContextConfiguration(initializers = {DatabaseIT.Initializer.class})
@ActiveProfiles(value="it")
@Testcontainers
public class DatabaseIT {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseIT.class);

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

    @Autowired
    ModelMapper modelMapper;

    @Test
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
        assertTrue(findHardware.isPresent());

        findHardware.get().setName("Test2");
        hardwareRepository.save(findHardware.get());

        Optional<Hardware> findHardware2 = hardwareRepository.findById(id);
        assertTrue(findHardware2.isPresent());
        assertEquals("123.123.123.123", findHardware2.get().getIP());
        assertEquals("Test2", findHardware2.get().getName());
        assertEquals("Y", findHardware2.get().getReservedIP());

        hardwareRepository.delete(findHardware2.get());
        findHardware2 = hardwareRepository.findById(id);
        assertFalse(findHardware2.isPresent());
    }

    @Test
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

        Backup newBackup = modelMapper.map(newBackupDTO,Backup.class);
        backupRepository.save(newBackup);

        Optional<Backup> findBackup = backupRepository.findById("XXXX");
        assertTrue(findBackup.isPresent());

        findBackup.get().setDirectory("/test/directory2");
        backupRepository.save(findBackup.get());

        Optional<Backup> findBackup2 = backupRepository.findById("XXXX");
        assertTrue(findBackup2.isPresent());
        assertEquals("/test/directory2", findBackup2.get().getDirectory());
        assertEquals("Test", findBackup2.get().getBackupName());
        assertEquals("XXXX", findBackup2.get().getId());
        assertEquals("Dev", findBackup2.get().getFileName());
        assertEquals(100, findBackup2.get().getTime());
        assertEquals("Git", findBackup2.get().getType());

        backupRepository.delete(findBackup.get());
        findBackup2 = backupRepository.findById("XXXX");
        assertFalse(findBackup2.isPresent());
    }
}
