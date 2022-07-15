package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testcontainers.containers.MySQLContainer;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("rawtypes")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {FsoIT.Initializer.class})
@ActiveProfiles(value="it")
public class FsoIT   {
    private static final Logger LOG = LoggerFactory.getLogger(FsoIT.class);

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
    public void source() {
        LOG.info("Source Testing");

        Optional<Location> location = locationRepository.findById(1);
        Assert.assertTrue(location.isPresent());

        Source newSource = new Source();
        newSource.setPath("/test/source/path");
        newSource.setFilter("*.FRD");
        newSource.setStatus(SourceStatusType.SST_OK);
        newSource.setLocation(location.get());

        sourceRepository.save(newSource);
        int newId = newSource.getIdAndType().getId();

        Optional<Source> foundSource = sourceRepository.findById(newId);
        Assert.assertTrue(foundSource.isPresent());
        Assert.assertNull(foundSource.get().getParentId());

        Assert.assertEquals("/test/source/path", foundSource.get().getPath());
        Assert.assertEquals("*.FRD", foundSource.get().getFilter());
        Assert.assertEquals("OK", foundSource.get().getStatus().getTypeName());
        Assert.assertEquals(1, foundSource.get().getLocation().getId());

        foundSource.get().setPath("/test/source2/path");
        foundSource.get().setStatus(SourceStatusType.SST_ERROR);
        foundSource.get().setFilter("FRD.*");
        sourceRepository.save(foundSource.get());

        Optional<Source> foundSource2 = sourceRepository.findById(newId);
        Assert.assertTrue(foundSource2.isPresent());

        Assert.assertEquals("ERROR", foundSource2.get().getStatus().getTypeName());
        Assert.assertEquals("/test/source2/path", foundSource2.get().getPath());
        Assert.assertEquals("FRD.*", foundSource2.get().getFilter());

        sourceRepository.delete(foundSource2.get());

        foundSource2 = sourceRepository.findById(newId);
        Assert.assertFalse(foundSource2.isPresent());
    }

    @Test
    public void file() throws ParseException {
        LOG.info("Test the basic file object");

        Optional<Location> testLocation = locationRepository.findById(1);
        Assert.assertTrue(testLocation.isPresent());

        Source testSource = new Source();
        testSource.setPath("/test/source/path");
        testSource.setFilter("*.FRD");
        testSource.setStatus(SourceStatusType.SST_OK);
        testSource.setLocation(testLocation.get());

        sourceRepository.save(testSource);

        DirectoryInfo directoryInfo = new DirectoryInfo();
        directoryInfo.setParent(testSource);
        directoryInfo.setName("test directory");
        directoryInfo.clearRemoved();

        directoryRepository.save(directoryInfo);

        Iterable<Classification> classifications = classificationRepository.findAll();
        List<Classification> classificationList = new ArrayList<>();
        classifications.forEach(classificationList::add);
        Assert.assertTrue(classificationList.size() > 2);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        LocalDateTime aDate =  LocalDateTime.parse("2022-06-02 10:02:03", formatter);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setParent(directoryInfo);
        fileInfo.setName("Blah");
        fileInfo.setClassification(classificationList.get(0));
        fileInfo.setDate(aDate);
        fileInfo.setMD5(new MD5("XYZ"));
        fileInfo.setSize(291L);
        fileInfo.clearRemoved();

        fileRepository.save(fileInfo);
        int theId = fileInfo.getIdAndType().getId();

        Optional<FileInfo> theFile = fileRepository.findById(theId);
        Assert.assertTrue(theFile.isPresent());

        Assert.assertEquals(directoryInfo.getIdAndType().getId(), theFile.get().getParentId().getId());
        Assert.assertEquals("Blah", theFile.get().getName());
        Assert.assertEquals(classificationList.get(0).getId(), theFile.get().getClassification().getId());
        Assert.assertEquals(aDate, theFile.get().getDate());
        Assert.assertEquals("XYZ", theFile.get().getMD5().toString());
        Assert.assertEquals(Long.valueOf(291L), theFile.get().getSize());
        Assert.assertEquals(false, theFile.get().getRemoved());

        aDate = LocalDateTime.parse("2022-06-02 11:03:10", formatter);
        theFile.get().setName("not Blah");
        theFile.get().setClassification(classificationList.get(1));
        theFile.get().setDate(aDate);
        theFile.get().setMD5(new MD5("BHS"));
        theFile.get().setSize(293L);
        fileRepository.save(theFile.get());

        Optional<FileInfo> theFile2 = fileRepository.findById(theId);
        Assert.assertTrue(theFile2.isPresent());

        Assert.assertEquals(directoryInfo.getIdAndType().getId(), theFile2.get().getParentId().getId());
        Assert.assertEquals("not Blah", theFile2.get().getName());
        Assert.assertEquals(classificationList.get(1).getId(), theFile2.get().getClassification().getId());
        Assert.assertEquals(aDate, theFile2.get().getDate());
        Assert.assertEquals("BHS", theFile2.get().getMD5().toString());
        Assert.assertEquals(Long.valueOf(293L), theFile2.get().getSize());

        fileRepository.delete(theFile2.get());

        theFile2 = fileRepository.findById(theId);
        Assert.assertFalse(theFile2.isPresent());
    }

    @Test
    public void directory() {
        LOG.info("Test the basic directory object");

        Optional<Location> testLocation = locationRepository.findById(1);
        Assert.assertTrue(testLocation.isPresent());

        Source testSource = new Source();
        testSource.setPath("/test/source/path");
        testSource.setFilter("*.FRD");
        testSource.setStatus(SourceStatusType.SST_OK);
        testSource.setLocation(testLocation.get());

        sourceRepository.save(testSource);

        DirectoryInfo directoryInfo = new DirectoryInfo();
        directoryInfo.setParent(testSource);
        directoryInfo.setName("test directory");
        directoryInfo.clearRemoved();

        directoryRepository.save(directoryInfo);

        DirectoryInfo directoryInfo1 = new DirectoryInfo();
        directoryInfo1.setParent(directoryInfo);
        directoryInfo1.setName("test 2");
        directoryInfo1.clearRemoved();

        directoryRepository.save(directoryInfo1);

        directoryInfo1.setRemoved();
        directoryRepository.save(directoryInfo1);

        List<DirectoryInfo> directoryInfoList = directoryRepository.findAllByOrderByIdAsc();

        Assert.assertEquals(2, directoryInfoList.size());
        Assert.assertEquals("test directory", directoryInfoList.get(0).getName());
        Assert.assertEquals("test 2", directoryInfoList.get(1).getName());

        Optional<FileSystemObject> parent = fileSystemObjectManager.findFileSystemObject(directoryInfoList.get(1).getParentId());
        Assert.assertTrue(parent.isPresent());
        Assert.assertTrue(parent.get() instanceof DirectoryInfo);

        parent = fileSystemObjectManager.findFileSystemObject(directoryInfoList.get(0).getParentId());
        Assert.assertTrue(parent.isPresent());
        Assert.assertTrue(parent.get() instanceof Source);

        try {
            directoryRepository.delete(directoryInfo);
            Assert.fail();
        } catch(DataIntegrityViolationException ex) {
            Assert.assertTrue(true);
        } catch(Exception ex) {
            Assert.fail();
        }
        directoryRepository.delete(directoryInfo1);
        directoryRepository.delete(directoryInfo);
        sourceRepository.delete(testSource);
    }

    @Test
    public void ignoreFile() {
        LOG.info("Test the basic ignore file object");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        LocalDateTime aDate =  LocalDateTime.parse("2022-04-21 14:12:07", formatter);

        IgnoreFile testIgnoreFile = new IgnoreFile();
        testIgnoreFile.setName("Ignore file");
        testIgnoreFile.setDate(aDate);
        testIgnoreFile.setMD5(new MD5("YTWVS"));
        testIgnoreFile.setSize(8310L);
        testIgnoreFile.clearRemoved();
        testIgnoreFile.setParent(null);

        ignoreFileRepository.save(testIgnoreFile);
        Assert.assertEquals(FileSystemObjectType.FSO_IGNORE_FILE, testIgnoreFile.getIdAndType().getType());
        int id = testIgnoreFile.getIdAndType().getId();

        Optional<IgnoreFile> findIgnoreFile = ignoreFileRepository.findById(id);
        Assert.assertTrue(findIgnoreFile.isPresent());

        Assert.assertEquals("Ignore file", findIgnoreFile.get().getName());
        Assert.assertEquals(aDate, findIgnoreFile.get().getDate());
        Assert.assertEquals("YTWVS", findIgnoreFile.get().getMD5().toString());
        Assert.assertEquals(Long.valueOf(8310L), findIgnoreFile.get().getSize());
        Assert.assertEquals(FileSystemObjectType.FSO_IGNORE_FILE, findIgnoreFile.get().getIdAndType().getType());

        findIgnoreFile.get().setMD5(new MD5("HYOSV"));
        ignoreFileRepository.save(findIgnoreFile.get());

        Optional<IgnoreFile> findIgnoreFile2 = ignoreFileRepository.findById(id);
        Assert.assertTrue(findIgnoreFile2.isPresent());

        Assert.assertEquals("HYOSV", findIgnoreFile2.get().getMD5().toString());

        ignoreFileRepository.delete(findIgnoreFile2.get());

        findIgnoreFile2 = ignoreFileRepository.findById(id);
        Assert.assertFalse(findIgnoreFile2.isPresent());
    }

    @Test
    public void importFile() {
        LOG.info("Test the basic import file object");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        LocalDateTime aDate =  LocalDateTime.parse("2022-04-21 14:12:07", formatter);

        ImportFile testImportFile = new ImportFile();
        testImportFile.setName("Ignore file");
        testImportFile.setDate(aDate);
        testImportFile.setMD5(new MD5("YTWVS"));
        testImportFile.setSize(8310L);
        testImportFile.clearRemoved();
        testImportFile.setStatus(ImportFileStatusType.IFS_READ);

        importFileRepository.save(testImportFile);
        Assert.assertEquals(FileSystemObjectType.FSO_IMPORT_FILE, testImportFile.getIdAndType().getType());
        int id = testImportFile.getIdAndType().getId();

        Optional<ImportFile> findImportFile = importFileRepository.findById(id);
        Assert.assertTrue(findImportFile.isPresent());

        Assert.assertEquals("Ignore file", findImportFile.get().getName());
        Assert.assertEquals(aDate, findImportFile.get().getDate());
        Assert.assertEquals("YTWVS", findImportFile.get().getMD5().toString());
        Assert.assertEquals(Long.valueOf(8310L), findImportFile.get().getSize());
        Assert.assertEquals(FileSystemObjectType.FSO_IMPORT_FILE, findImportFile.get().getIdAndType().getType());

        findImportFile.get().setStatus(ImportFileStatusType.IFS_COMPLETE);
        importFileRepository.save(findImportFile.get());

        Optional<ImportFile> findImportFile2 = importFileRepository.findById(id);
        Assert.assertTrue(findImportFile2.isPresent());

        Assert.assertEquals(ImportFileStatusType.IFS_COMPLETE, findImportFile2.get().getStatus());

        importFileRepository.delete(findImportFile2.get());

        findImportFile2 = importFileRepository.findById(id);
        Assert.assertFalse(findImportFile2.isPresent());
    }

    @Test
    public void importSource() {
        LOG.info("Test the basic import source object");

        Optional<Location> location = locationRepository.findById(1);
        Assert.assertTrue(location.isPresent());

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
        Assert.assertTrue(foundSource.isPresent());

        Assert.assertEquals("/test/source/import", foundSource.get().getPath());
        Assert.assertEquals("*.FRD", foundSource.get().getFilter());
        Assert.assertEquals(SourceStatusType.SST_OK, foundSource.get().getStatus());
        Assert.assertEquals(1, foundSource.get().getLocation().getId());
        Assert.assertEquals( destinationId, foundSource.get().getDestination().getIdAndType().getId());

        foundSource.get().setPath("/test/source2/import");
        foundSource.get().setStatus(SourceStatusType.SST_ERROR);
        foundSource.get().setFilter("FRD.*");
        sourceRepository.save(foundSource.get());

        Optional<ImportSource> foundSource2 = importSourceRepository.findById(newId);
        Assert.assertTrue(foundSource2.isPresent());

        Assert.assertEquals(SourceStatusType.SST_ERROR, foundSource2.get().getStatus());
        Assert.assertEquals("/test/source2/import", foundSource2.get().getPath());
        Assert.assertEquals("FRD.*", foundSource2.get().getFilter());
        Assert.assertEquals( destinationId, foundSource2.get().getDestination().getIdAndType().getId());

        sourceRepository.delete(foundSource2.get());

        foundSource2 = importSourceRepository.findById(newId);
        Assert.assertFalse(foundSource2.isPresent());
    }
}
