package com.jbr.middletier.backup.integration.migrate;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {MigrateIT.Initializer.class})
@ActiveProfiles(value="it-migration")
public class MigrateIT {
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
    FileSystemObjectManager fileSystemObjectManager;

    @Autowired
    AssociatedFileDataManager associatedFileDataManager;

    @Test
    public void testMigrationSource() {
        // Check that expected source and import source records are in the database.
        int count = 0;
        int importCount = 0;
        for(Source source :associatedFileDataManager.internalFindAllSource()) {
            count++;

            switch(source.getIdAndType().getId()) {
                case 1:
                    Assert.assertEquals("/media/Shared/Photo", source.getPath());
                    Assert.assertEquals("d{4}$", source.getFilter());
                    Assert.assertEquals(1, (source.getLocation().getId()));
                    Assert.assertEquals(SourceStatusType.SST_OK,source.getStatus());
                    break;
                case 2:
                    Assert.assertEquals("/media/Backup/Photo", source.getPath());
                    Assert.assertEquals("", source.getFilter());
                    Assert.assertEquals(1, source.getLocation().getId());
                    Assert.assertEquals(SourceStatusType.SST_ERROR,source.getStatus());
                    break;
                case 3:
                    Assert.assertEquals("/home/jason/Pictures/Martina Single", source.getPath());
                    Assert.assertEquals("", source.getFilter());
                    Assert.assertEquals(1, source.getLocation().getId());
                    Assert.assertEquals(SourceStatusType.SST_OK,source.getStatus());
                    break;
                default:
                    Assert.fail();
            }

            if(source.getIdAndType().getType().equals(FileSystemObjectType.FSO_IMPORT_SOURCE)) {
                importCount++;
            }
        }
        Assert.assertEquals(3,count);
        Assert.assertEquals(1,importCount);

        count = 0;
        for(ImportSource nextImportSource : associatedFileDataManager.internalFindAllImportSource()) {
            Assert.assertEquals(3, (long)nextImportSource.getIdAndType().getId());
            Assert.assertEquals(1, (long)nextImportSource.getDestination().getIdAndType().getId());
            count++;
        }
        Assert.assertEquals(1,count);

        // Check the FK's
        try {
            SourceDTO delete = new SourceDTO(1, "ignored");
            associatedFileDataManager.deleteSource(delete);
            Assert.fail();
        } catch(Exception e) {
            Assert.assertEquals("could not execute statement; SQL [n/a]; constraint [null]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement", e.getMessage());
        }
    }

    @Test
    public void testMigrationFile() {
        // How many files / directories are there on the
        List<DirectoryInfo> directories = new ArrayList<>();
        List<FileInfo> files = new ArrayList<>();
        fileSystemObjectManager.loadByParent(1, directories, files);

        Assert.assertEquals(10, directories.size());
        Assert.assertEquals(666, files.size());

        // Do some spot checks.
        Optional<FileSystemObject> fso = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(107, FileSystemObjectType.FSO_DIRECTORY));
        Assert.assertTrue(fso.isPresent());
        DirectoryInfo directory = (DirectoryInfo) fso.get();
        Assert.assertEquals("/2016/September/Boom Banger/Day 6", directory.getName());
        Assert.assertFalse(directory.getRemoved());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        fso = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(625 + 20102, FileSystemObjectType.FSO_FILE));
        Assert.assertTrue(fso.isPresent());
        FileInfo file = (FileInfo) fso.get();
        Assert.assertEquals("WP_20160917_00_27_06_Selfie.jpg", file.getName());
        Assert.assertEquals(9 + 101, (long)file.getParentId().getId());
        Assert.assertEquals(FileSystemObjectType.FSO_DIRECTORY, file.getParentId().getType());
        Assert.assertEquals(5, (long)file.getClassification().getId());
        Assert.assertEquals(1702065, (long)file.getSize());
        Assert.assertEquals("2016-10-01 15:30", sdf.format(file.getDate()));
        Assert.assertFalse(file.getRemoved());
        Assert.assertEquals("F75A4959EC58FA4F30877900B88C3340", file.getMD5().toString());
    }

    @Test
    public void testIgnoreFile() {
        Iterable<FileSystemObject> ignoreFiles = fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_IGNORE_FILE);
        int count = 0;
        for(FileSystemObject nextFile : ignoreFiles) {
            count++;
        }
        Assert.assertEquals(31,count);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Optional<FileSystemObject> fso = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(24 + 20102, FileSystemObjectType.FSO_FILE));
        Assert.assertTrue(fso.isPresent());
        FileInfo file = (FileInfo) fso.get();
        Assert.assertEquals("IMG_1252.JPG", file.getName());
        Assert.assertNull(file.getParentId());
        Assert.assertNull(file.getClassification());
        Assert.assertEquals(670911, (long)file.getSize());
        Assert.assertEquals("2019-05-26 00:06", sdf.format(file.getDate()));
        Assert.assertFalse(file.getRemoved());
        Assert.assertEquals("FE0F70C0583A6189679377495516652D", file.getMD5().toString());
    }

    @Test
    public void testImportFile() {
        Iterable<FileSystemObject> ignoreFiles = fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_IMPORT_FILE);
        int count = 0;
        for(FileSystemObject nextFile : ignoreFiles) {
            count++;
        }
        Assert.assertEquals(6 ,count);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        //12169,COMPLETE,90560
//        90560,IMG_5739.jpg,10,5,1645188,2022-01-03 18:44:31,0,1CE144410E8E0E960938D899183A32BF,
        Optional<FileSystemObject> fso = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(90560 + 320103, FileSystemObjectType.FSO_FILE));
        Assert.assertTrue(fso.isPresent());
        FileInfo file = (FileInfo) fso.get();
        Assert.assertEquals("IMG_5739.JPG", file.getName());
        Assert.assertEquals(10 + 101, (long)file.getParentId().getId());
        Assert.assertEquals(FileSystemObjectType.FSO_DIRECTORY, file.getParentId().getType());
        Assert.assertEquals(5, (long)file.getClassification().getId());
        Assert.assertEquals(1645188, (long)file.getSize());
        Assert.assertEquals("2022-01-03 18:44", sdf.format(file.getDate()));
        Assert.assertFalse(file.getRemoved());
        Assert.assertEquals("1CE144410E8E0E960938D899183A32BF", file.getMD5().toString());
    }
}
