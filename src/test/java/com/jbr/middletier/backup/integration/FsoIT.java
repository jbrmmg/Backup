package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("rawtypes")
@SpringBootTest(classes = MiddleTier.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@ContextConfiguration(initializers = {FsoIT.Initializer.class})
@ActiveProfiles(value="it")
@Testcontainers
public class FsoIT {
    private static final Logger LOG = LoggerFactory.getLogger(FsoIT.class);

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
    SourceRepository sourceRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    DirectoryRepository directoryRepository;

    @Autowired
    ClassificationRepository classificationRepository;

    @Autowired
    IgnoreFileRepository ignoreFileRepository;

    @Autowired
    ImportFileRepository importFileRepository;

    @Autowired
    ImportSourceRepository importSourceRepository;

    @Autowired
    FileSystemObjectManager fileSystemObjectManager;


    @Test
    void source() {
        LOG.info("Source Testing");

        Optional<Location> location = locationRepository.findById(1);
        assertTrue(location.isPresent());

        Source newSource = new Source();
        newSource.setPath("/test/source/path");
        newSource.setFilter("*.FRD");
        newSource.setStatus(SourceStatusType.SST_OK);
        newSource.setLocation(location.get());

        sourceRepository.save(newSource);
        int newId = newSource.getIdAndType().getId();

        Optional<Source> foundSource = sourceRepository.findById(newId);
        assertTrue(foundSource.isPresent());
        assertFalse(foundSource.get().getParentId().isPresent());

        assertEquals("/test/source/path", foundSource.get().getPath());
        assertEquals("*.FRD", foundSource.get().getFilter());
        assertEquals("OK", foundSource.get().getStatus().getTypeName());
        assertEquals(1, foundSource.get().getLocation().getId());

        foundSource.get().setPath("/test/source2/path");
        foundSource.get().setStatus(SourceStatusType.SST_ERROR);
        foundSource.get().setFilter("FRD.*");
        sourceRepository.save(foundSource.get());

        Optional<Source> foundSource2 = sourceRepository.findById(newId);
        assertTrue(foundSource2.isPresent());

        assertEquals("ERROR", foundSource2.get().getStatus().getTypeName());
        assertEquals("/test/source2/path", foundSource2.get().getPath());
        assertEquals("FRD.*", foundSource2.get().getFilter());

        sourceRepository.delete(foundSource2.get());

        foundSource2 = sourceRepository.findById(newId);
        assertFalse(foundSource2.isPresent());
    }

    @Test
    void file() {
        LOG.info("Test the basic file object");

        Optional<Location> testLocation = locationRepository.findById(1);
        assertTrue(testLocation.isPresent());

        Source testSource = new Source();
        testSource.setPath("/test/source/path");
        testSource.setFilter("*.FRD");
        testSource.setStatus(SourceStatusType.SST_OK);
        testSource.setLocation(testLocation.get());

        sourceRepository.save(testSource);

        DirectoryInfo directoryInfo = new DirectoryInfo();
        directoryInfo.setParent(testSource);
        directoryInfo.setName("test directory");

        directoryRepository.save(directoryInfo);

        Iterable<Classification> classifications = classificationRepository.findAll();
        List<Classification> classificationList = new ArrayList<>();
        classifications.forEach(classificationList::add);
        assertTrue(classificationList.size() > 2);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        LocalDateTime aDate =  LocalDateTime.parse("2022-06-02 10:02:03", formatter);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setParent(directoryInfo);
        fileInfo.setName("Blah");
        fileInfo.setClassification(classificationList.get(0));
        fileInfo.setDate(aDate);
        fileInfo.setMd5(new MD5("C714A0B2E792EB102F706DC2424B0083"));
        fileInfo.setSize(291L);

        fileRepository.save(fileInfo);
        int theId = fileInfo.getIdAndType().getId();

        Optional<FileInfo> theFile = fileRepository.findById(theId);
        assertTrue(theFile.isPresent());

        assertTrue(theFile.get().getParentId().isPresent());
        assertEquals(directoryInfo.getIdAndType().getId(), theFile.get().getParentId().get().getId());
        assertEquals("Blah", theFile.get().getName());
        assertEquals(classificationList.get(0).getId(), theFile.get().getClassification().getId());
        assertEquals(aDate, theFile.get().getDate());
        assertTrue(theFile.get().getMd5().isPresent());
        assertTrue(theFile.get().getMd5().isPresent());
        assertEquals("C714A0B2E792EB102F706DC2424B0083", theFile.get().getMd5().get().toString());
        assertEquals(Long.valueOf(291L), theFile.get().getSize());

        aDate = LocalDateTime.parse("2022-06-02 11:03:10", formatter);
        theFile.get().setName("not Blah");
        theFile.get().setClassification(classificationList.get(1));
        theFile.get().setDate(aDate);
        theFile.get().setMd5(new MD5("12345678901234567890123456789012"));
        theFile.get().setSize(293L);
        fileRepository.save(theFile.get());

        Optional<FileInfo> theFile2 = fileRepository.findById(theId);
        assertTrue(theFile2.isPresent());

        assertTrue(theFile2.get().getParentId().isPresent());
        assertEquals(directoryInfo.getIdAndType().getId(), theFile2.get().getParentId().get().getId());
        assertEquals("not Blah", theFile2.get().getName());
        assertEquals(classificationList.get(1).getId(), theFile2.get().getClassification().getId());
        assertEquals(aDate, theFile2.get().getDate());
        assertTrue(theFile2.get().getMd5().isPresent());
        assertEquals("12345678901234567890123456789012", theFile2.get().getMd5().get().toString());
        assertEquals(Long.valueOf(293L), theFile2.get().getSize());

        fileRepository.delete(theFile2.get());

        theFile2 = fileRepository.findById(theId);
        assertFalse(theFile2.isPresent());
    }

    @Test
    void directory() {
        LOG.info("Test the basic directory object");

        Optional<Location> testLocation = locationRepository.findById(1);
        assertTrue(testLocation.isPresent());

        Source testSource = new Source();
        testSource.setPath("/test/source/path");
        testSource.setFilter("*.FRD");
        testSource.setStatus(SourceStatusType.SST_OK);
        testSource.setLocation(testLocation.get());

        sourceRepository.save(testSource);

        DirectoryInfo directoryInfo = new DirectoryInfo();
        directoryInfo.setParent(testSource);
        directoryInfo.setName("test directory");

        directoryRepository.save(directoryInfo);

        DirectoryInfo directoryInfo1 = new DirectoryInfo();
        directoryInfo1.setParent(directoryInfo);
        directoryInfo1.setName("test 2");

        directoryRepository.save(directoryInfo1);

        List<DirectoryInfo> directoryInfoList = directoryRepository.findAllByOrderByIdAsc();

        assertEquals(2, directoryInfoList.size());
        assertEquals("test directory", directoryInfoList.get(0).getName());
        assertEquals("test 2", directoryInfoList.get(1).getName());

        Optional<FileSystemObjectId> parentId = directoryInfoList.get(1).getParentId();
        assertTrue(parentId.isPresent());
        Optional<FileSystemObject> parent = fileSystemObjectManager.findFileSystemObject(parentId.get());
        assertTrue(parent.isPresent());
        assertInstanceOf(DirectoryInfo.class, parent.get());

        parentId = directoryInfoList.get(0).getParentId();
        assertTrue(parentId.isPresent());
        parent = fileSystemObjectManager.findFileSystemObject(parentId.get());
        assertTrue(parent.isPresent());
        assertInstanceOf(Source.class, parent.get());

        try {
            directoryRepository.delete(directoryInfo);
            fail();
        } catch(DataIntegrityViolationException ex) {
            assertTrue(true);
        } catch(Exception ex) {
            fail();
        }
        directoryRepository.delete(directoryInfo1);
        directoryRepository.delete(directoryInfo);
        sourceRepository.delete(testSource);
    }

    @Test
    void ignoreFile() {
        LOG.info("Test the basic ignore file object");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        LocalDateTime aDate =  LocalDateTime.parse("2022-04-21 14:12:07", formatter);

        IgnoreFile testIgnoreFile = new IgnoreFile();
        testIgnoreFile.setName("Ignore file");
        testIgnoreFile.setDate(aDate);
        testIgnoreFile.setMd5(new MD5("12345678901234567890123456789012"));
        testIgnoreFile.setSize(8310L);
        testIgnoreFile.setParent(null);

        ignoreFileRepository.save(testIgnoreFile);
        assertEquals(FileSystemObjectType.FSO_IGNORE_FILE, testIgnoreFile.getIdAndType().getType());
        int id = testIgnoreFile.getIdAndType().getId();

        Optional<IgnoreFile> findIgnoreFile = ignoreFileRepository.findById(id);
        assertTrue(findIgnoreFile.isPresent());

        assertEquals("Ignore file", findIgnoreFile.get().getName());
        assertEquals(aDate, findIgnoreFile.get().getDate());
        assertTrue(findIgnoreFile.get().getMd5().isPresent());
        assertEquals("12345678901234567890123456789012", findIgnoreFile.get().getMd5().get().toString());
        assertEquals(Long.valueOf(8310L), findIgnoreFile.get().getSize());
        assertEquals(FileSystemObjectType.FSO_IGNORE_FILE, findIgnoreFile.get().getIdAndType().getType());

        findIgnoreFile.get().setMd5(new MD5("12345678901234567890123456789012"));
        ignoreFileRepository.save(findIgnoreFile.get());

        Optional<IgnoreFile> findIgnoreFile2 = ignoreFileRepository.findById(id);
        assertTrue(findIgnoreFile2.isPresent());

        assertTrue(findIgnoreFile2.get().getMd5().isPresent());
        assertEquals("12345678901234567890123456789012", findIgnoreFile2.get().getMd5().get().toString());

        ignoreFileRepository.delete(findIgnoreFile2.get());

        findIgnoreFile2 = ignoreFileRepository.findById(id);
        assertFalse(findIgnoreFile2.isPresent());
    }

    @Test
    void importFile() {
        LOG.info("Test the basic import file object");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        LocalDateTime aDate =  LocalDateTime.parse("2022-04-21 14:12:07", formatter);

        ImportFile testImportFile = new ImportFile();
        testImportFile.setName("Ignore file");
        testImportFile.setDate(aDate);
        testImportFile.setMd5(new MD5("12345678901234567890123456789012"));
        testImportFile.setSize(8310L);

        importFileRepository.save(testImportFile);
        assertEquals(FileSystemObjectType.FSO_IMPORT_FILE, testImportFile.getIdAndType().getType());
        int id = testImportFile.getIdAndType().getId();

        Optional<ImportFile> findImportFile = importFileRepository.findById(id);
        assertTrue(findImportFile.isPresent());

        assertEquals("Ignore file", findImportFile.get().getName());
        assertEquals(aDate, findImportFile.get().getDate());
        assertTrue(findImportFile.get().getMd5().isPresent());
        assertEquals("12345678901234567890123456789012", findImportFile.get().getMd5().get().toString());
        assertEquals(Long.valueOf(8310L), findImportFile.get().getSize());
        assertEquals(FileSystemObjectType.FSO_IMPORT_FILE, findImportFile.get().getIdAndType().getType());

        importFileRepository.save(findImportFile.get());

        Optional<ImportFile> findImportFile2 = importFileRepository.findById(id);
        assertTrue(findImportFile2.isPresent());

        importFileRepository.delete(findImportFile2.get());

        findImportFile2 = importFileRepository.findById(id);
        assertFalse(findImportFile2.isPresent());
    }

    @Test
    void importSource() {
        LOG.info("Test the basic import source object");

        Optional<Location> location = locationRepository.findById(1);
        assertTrue(location.isPresent());

        Source newSource = new Source();
        newSource.setPath("/test/source/path");
        newSource.setFilter("*.FRD");
        newSource.setStatus(SourceStatusType.SST_OK);
        newSource.setLocation(location.get());

        sourceRepository.save(newSource);
        Integer destinationId = newSource.getIdAndType().getId();

        ImportSource newImportSource = new ImportSource();
        newImportSource.setPath("/test/source/import");
        newImportSource.setFilter("*.FRD");
        newImportSource.setStatus(SourceStatusType.SST_OK);
        newImportSource.setLocation(location.get());
        newImportSource.setDestination(newSource);

        importSourceRepository.save(newImportSource);
        Integer newId = newImportSource.getIdAndType().getId();

        Optional<ImportSource> foundSource = importSourceRepository.findById(newId);
        assertTrue(foundSource.isPresent());

        assertEquals("/test/source/import", foundSource.get().getPath());
        assertEquals("*.FRD", foundSource.get().getFilter());
        assertEquals(SourceStatusType.SST_OK, foundSource.get().getStatus());
        assertEquals(1, foundSource.get().getLocation().getId());
        assertEquals( destinationId, foundSource.get().getDestination().getIdAndType().getId());

        foundSource.get().setPath("/test/source2/import");
        foundSource.get().setStatus(SourceStatusType.SST_ERROR);
        foundSource.get().setFilter("FRD.*");
        sourceRepository.save(foundSource.get());

        Optional<ImportSource> foundSource2 = importSourceRepository.findById(newId);
        assertTrue(foundSource2.isPresent());

        assertEquals(SourceStatusType.SST_ERROR, foundSource2.get().getStatus());
        assertEquals("/test/source2/import", foundSource2.get().getPath());
        assertEquals("FRD.*", foundSource2.get().getFilter());
        assertEquals( destinationId, foundSource2.get().getDestination().getIdAndType().getId());

        sourceRepository.delete(foundSource2.get());

        foundSource2 = importSourceRepository.findById(newId);
        assertFalse(foundSource2.isPresent());
    }
}
