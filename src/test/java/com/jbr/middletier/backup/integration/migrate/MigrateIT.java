package com.jbr.middletier.backup.integration.migrate;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.WebTester;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.ActionConfirmDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.manager.ActionManager;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testcontainers.containers.MySQLContainer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {MigrateIT.Initializer.class})
@ActiveProfiles(value="it-migration")
public class MigrateIT extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(MigrateIT.class);

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

    @Autowired
    ActionManager actionManager;

    @Test
    public void testMigrationSource() {
        LOG.info("Test source migration");

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
        LOG.info("Test file migration");

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
        LOG.info("Test ignore file migration");

        Iterable<FileSystemObject> ignoreFiles = fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_IGNORE_FILE);
        int count = 0;
        for(FileSystemObject ignored : ignoreFiles) {
            count++;
        }
        Assert.assertEquals(31,count);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Optional<FileSystemObject> fso = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(24 + 320103, FileSystemObjectType.FSO_IGNORE_FILE));
        Assert.assertTrue(fso.isPresent());
        IgnoreFile file = (IgnoreFile) fso.get();
        Assert.assertNotNull(file);
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
        LOG.info("Test import file migration");

        Iterable<FileSystemObject> ignoreFiles = fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_IMPORT_FILE);
        int count = 0;
        for(FileSystemObject ignored : ignoreFiles) {
            count++;
        }
        Assert.assertEquals(6 ,count);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Optional<FileSystemObject> fso = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(90560 + 20102, FileSystemObjectType.FSO_IMPORT_FILE));
        Assert.assertTrue(fso.isPresent());
        ImportFile file = (ImportFile) fso.get();
        Assert.assertNotNull(file);
        Assert.assertEquals("IMG_5739.jpg", file.getName());
        Assert.assertEquals(10 + 101, (long)file.getParentId().getId());
        Assert.assertEquals(FileSystemObjectType.FSO_DIRECTORY, file.getParentId().getType());
        Assert.assertEquals(5, (long)file.getClassification().getId());
        Assert.assertEquals(1645188, (long)file.getSize());
        Assert.assertEquals("2022-01-03 18:44", sdf.format(file.getDate()));
        Assert.assertFalse(file.getRemoved());
        Assert.assertEquals("1CE144410E8E0E960938D899183A32BF", file.getMD5().toString());
        Assert.assertEquals(ImportFileStatusType.IFS_COMPLETE, file.getStatus());

        // Check the FK's
        try {
            Optional<FileSystemObject> directory = fileSystemObjectManager.findFileSystemObject(file.getParentId());
            Assert.assertTrue(directory.isPresent());
            fileSystemObjectManager.delete(directory.get());
            Assert.fail();
        } catch(Exception e) {
            Assert.assertEquals("could not execute statement; SQL [n/a]; constraint [null]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement", e.getMessage());
        }
    }

    @Test
    public void testActionsImport() {
        LOG.info("Test action migration");

        List<ActionConfirmDTO> actions = actionManager.externalFindByConfirmed(false);
        Assert.assertEquals(2, actions.size());


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for(ActionConfirmDTO next : actions) {
            if (next.getId() == 1) {
                Assert.assertEquals(ActionConfirmType.AC_IMPORT.getTypeName(), next.getAction());
                Optional<FileSystemObject> importFile = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(next.getFileId(), FileSystemObjectType.FSO_IMPORT_FILE));
                Assert.assertTrue(importFile.isPresent());
                ImportFile file = (ImportFile) importFile.get();
                Assert.assertNotNull(file);
                Assert.assertEquals("IMG_4060.jpg", file.getName());
                Assert.assertEquals(10 + 101, (long) file.getParentId().getId());
                Assert.assertEquals(FileSystemObjectType.FSO_DIRECTORY, file.getParentId().getType());
                Assert.assertEquals(5, (long) file.getClassification().getId());
                Assert.assertEquals(1570162, (long) file.getSize());
                Assert.assertEquals("2022-01-03 18:39", sdf.format(file.getDate()));
                Assert.assertFalse(file.getRemoved());
                Assert.assertEquals("09EC9A3FD7166D5394D916FB47B3903F", file.getMD5().toString());
                Assert.assertEquals(ImportFileStatusType.IFS_COMPLETE, file.getStatus());
            }
        }
    }

    @Test
    public void testActions() {
        LOG.info("Test action migration");

        List<ActionConfirmDTO> actions = actionManager.externalFindByConfirmed(false);
        Assert.assertEquals(2, actions.size());

        int testDeleteId = -1;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for(ActionConfirmDTO next : actions) {
            if (next.getId() == 2) {
                Assert.assertEquals(ActionConfirmType.AC_DELETE.getTypeName(), next.getAction());
                Optional<FileSystemObject> optFile = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(next.getFileId(), FileSystemObjectType.FSO_FILE));
                Assert.assertTrue(optFile.isPresent());
                FileInfo file = (FileInfo) optFile.get();
                Assert.assertNotNull(file);
                testDeleteId = next.getFileId();
                Assert.assertEquals("Report-September-2020.pdf", file.getName());
                Assert.assertEquals(11 + 101, (long) file.getParentId().getId());
                Assert.assertEquals(FileSystemObjectType.FSO_DIRECTORY, file.getParentId().getType());
                Assert.assertEquals(4, (long) file.getClassification().getId());
                Assert.assertEquals(712919, (long) file.getSize());
                Assert.assertEquals("2020-09-30 04:00", sdf.format(file.getDate()));
                Assert.assertTrue(file.getRemoved());
                Assert.assertEquals("76EEAAC078EED94423E10495D99BBF1C", file.getMD5().toString());
            }
        }

        // Check the FK's
        try {
            Assert.assertNotEquals(-1, testDeleteId);
            Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(testDeleteId, FileSystemObjectType.FSO_FILE));
            Assert.assertTrue(file.isPresent());
            fileSystemObjectManager.delete(file.get());
            Assert.fail();
        } catch(Exception e) {
            Assert.assertEquals("could not execute statement; SQL [n/a]; constraint [null]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement", e.getMessage());
        }
    }

    @Test
    public void testPostMigration() throws Exception {
        LOG.info("Test post migration");

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/migration")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].blanksRemoved", is(1)))
                .andExpect(jsonPath("$[0].dotFilesRemoved", is(9)))
                .andExpect(jsonPath("$[0].directoriesUpdated", is(10)))
                .andExpect(jsonPath("$[0].newDirectories", is(5)));

        // How many files / directories are there on the
        List<DirectoryInfo> directories = new ArrayList<>();
        List<FileInfo> files = new ArrayList<>();
        fileSystemObjectManager.loadByParent(1, directories, files);

        Assert.assertEquals(15, directories.size());
        Assert.assertEquals(657, files.size());

        Optional<FileSystemObject> fso = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(444 + 20102, FileSystemObjectType.FSO_FILE));
        Assert.assertTrue(fso.isPresent());
        FileInfo file = (FileInfo) fso.get();
        Assert.assertEquals("IMG_20160914_172148374_HDR.jpg", file.getName());

        fso = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(6 + 101, FileSystemObjectType.FSO_DIRECTORY));
        Assert.assertTrue(fso.isPresent());
        DirectoryInfo directory = (DirectoryInfo) fso.get();
        Assert.assertEquals("Day 6", directory.getName());
        FileSystemObjectId checkSameParent = fso.get().getParentId();

        fso = fileSystemObjectManager.findFileSystemObject(fso.get().getParentId());
        Assert.assertTrue(fso.isPresent());
        directory = (DirectoryInfo) fso.get();
        Assert.assertEquals("Boom Banger", directory.getName());

        fso = fileSystemObjectManager.findFileSystemObject(fso.get().getParentId());
        Assert.assertTrue(fso.isPresent());
        directory = (DirectoryInfo) fso.get();
        Assert.assertEquals("September", directory.getName());

        fso = fileSystemObjectManager.findFileSystemObject(fso.get().getParentId());
        Assert.assertTrue(fso.isPresent());
        directory = (DirectoryInfo) fso.get();
        Assert.assertEquals("2016", directory.getName());

        fso = fileSystemObjectManager.findFileSystemObject(fso.get().getParentId());
        Assert.assertTrue(fso.isPresent());
        Source source = (Source) fso.get();
        Assert.assertEquals("/media/Shared/Photo", source.getName());
        Assert.assertNull(source.getParentId());

        fso = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(8 + 101, FileSystemObjectType.FSO_DIRECTORY));
        Assert.assertTrue(fso.isPresent());
        directory = (DirectoryInfo) fso.get();
        Assert.assertEquals("Day 2", directory.getName());
        Assert.assertEquals(checkSameParent, directory.getParentId());
    }
}
