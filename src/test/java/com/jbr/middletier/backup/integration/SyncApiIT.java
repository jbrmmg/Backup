package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
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

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {SyncApiIT.Initializer.class})
@ActiveProfiles(value="it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SyncApiIT extends FileTester {
    private static final Logger LOG = LoggerFactory.getLogger(SyncApiIT.class);

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
    SourceRepository sourceRepository;

    @Autowired
    SynchronizeRepository synchronizeRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    DirectoryRepository directoryRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    ClassificationRepository classificationRepository;

    @Autowired
    ActionConfirmRepository actionConfirmRepository;

    private Location getLocation() {
        Optional<Location> location = locationRepository.findById(1);
        if(!location.isPresent())
            fail();

        return location.get();
    }

    private Source createSource(String path) {
        Source newSource = new Source();
        newSource.setLocation(getLocation());
        newSource.setStatus(SourceStatusType.SST_OK);
        newSource.setPath(path);

        sourceRepository.save(newSource);

        return newSource;
    }

    private void validateSource(Source source, List<StructureDescription> structure, boolean checkSizeAndMD5) {
        // Get the directories and files that were found.
        Iterable<DirectoryInfo> directories = directoryRepository.findAllByOrderByIdAsc();
        Iterable<FileInfo> files = fileRepository.findAllByOrderByIdAsc();

        // How many directories are expected?
        List<String> expectedDirectories = new ArrayList<>();
        for(StructureDescription nextFile : structure) {
            String[] elements = nextFile.directory.split(FileSystems.getDefault().getSeparator());
            String fullPath = elements[0];
            boolean skippedFirst = false;
            for(String nextElement: elements) {
                if(skippedFirst) {
                    fullPath = fullPath + FileSystems.getDefault().getSeparator() + nextElement;
                }

                if(!expectedDirectories.contains(fullPath)) {
                    expectedDirectories.add(fullPath);
                }

                skippedFirst = true;
            }
        }

        List<DirectoryInfo> dbDirectories = new ArrayList<>();
        directories.forEach(dbDirectories::add);

        Assert.assertEquals(expectedDirectories.size(), dbDirectories.size());

        DirectoryTree structureTree = new DirectoryTree(expectedDirectories);
        DirectoryTree dbTree = new DirectoryTree(source, directoryRepository);

        dbTree.AssertExpected(structureTree);

        // Check each file that is in the database.
        int fileCount = 0;
        for(FileInfo nextFile : files) {
            fileCount++;

            // Find this file in the structure.
            boolean found = false;
            for(StructureDescription nextExpectedFile : structure) {
                int expectedParentId = dbTree.FindDirectory(nextExpectedFile.directory);

                if(nextFile.getName().equals(nextExpectedFile.destinationName) && nextFile.getParentId().getId() == expectedParentId) {
                    found = true;
                    Assert.assertEquals(nextExpectedFile.dateTime,nextFile.getDate());
                    if(checkSizeAndMD5) {
                        if(nextExpectedFile.md5 != null) {
                            Assert.assertEquals(nextExpectedFile.md5,nextFile.getMD5());
                        }
                        if(nextExpectedFile.fileSize != null) {
                            Assert.assertEquals(nextExpectedFile.fileSize,nextFile.getSize());
                        }
                    }
                    nextExpectedFile.checked = true;
                }
            }
            Assert.assertTrue(found);
        }

        Assert.assertEquals(structure.size(), fileCount);
    }

    @Test
    @Order(1)
    public void gather() throws Exception {
        LOG.info("Synchronize Testing");

        // Update JPG so it gets an MD5
        for(Classification nextClassification : classificationRepository.findAllByOrderByIdAsc()) {
            if(nextClassification.getRegex().contains("jpg")) {
                ClassificationDTO updateClassification = new ClassificationDTO();
                updateClassification.setIcon(nextClassification.getIcon());
                updateClassification.setRegex(nextClassification.getRegex());
                updateClassification.setAction(nextClassification.getAction());
                updateClassification.setVideo(nextClassification.getIsVideo());
                updateClassification.setOrder(1);
                updateClassification.setId(nextClassification.getId());
                updateClassification.setUseMD5(true);

                nextClassification.update(updateClassification);
                classificationRepository.save(nextClassification);
            }
        }

        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        // Create the source and synchronise entries
        Synchronize synchronize = new Synchronize();
        synchronize.setId(1);
        synchronize.setSource(createSource("./target/it_test/source"));
        synchronize.setDestination(createSource("./target/it_test/destination"));

        synchronizeRepository.save(synchronize);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        validateSource(synchronize.getSource(),sourceDescription,true);

        // Update the directory structure
        sourceDescription = getTestStructure("test2");
        deleteDirectoryContents(new File(sourceDirectory).toPath());
        Files.createDirectories(new File(sourceDirectory).toPath());
        copyFiles(sourceDescription, sourceDirectory);

        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        validateSource(synchronize.getSource(),sourceDescription, true);

        // Update the directory structure again.
        sourceDescription = getTestStructure("test3");
        deleteDirectoryContents(new File(sourceDirectory).toPath());
        Files.createDirectories(new File(sourceDirectory).toPath());
        copyFiles(sourceDescription, sourceDirectory);

        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        validateSource(synchronize.getSource(),sourceDescription, true);

        synchronizeRepository.delete(synchronize);
        fileRepository.deleteAll();

        List<DirectoryInfo> dbDirectories = new ArrayList<>(directoryRepository.findAllByOrderByIdAsc());
        for(DirectoryInfo nextDirectory : dbDirectories) {
            nextDirectory.setParent(null);
            directoryRepository.save(nextDirectory);
        }

        directoryRepository.deleteAll();
        sourceRepository.delete(synchronize.getSource());
        sourceRepository.delete(synchronize.getDestination());
    }

    @Test
    @Order(2)
    public void synchronize() throws Exception {
        LOG.info("Synchronize Testing");

        // During this test create files in the following directories
        String sourceDirectory = "./target/it_test/source";
        deleteDirectoryContents(new File(sourceDirectory).toPath());
        Files.createDirectories(new File(sourceDirectory).toPath());

        String destinationDirectory = "./target/it_test/destination";
        deleteDirectoryContents(new File(destinationDirectory).toPath());
        Files.createDirectories(new File(destinationDirectory).toPath());

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, sourceDirectory);

        // Create the source and synchronise entries
        Synchronize synchronize = new Synchronize();
        synchronize.setId(1);
        synchronize.setSource(createSource("./target/it_test/source"));
        synchronize.setDestination(createSource("./target/it_test/destination"));

        synchronizeRepository.save(synchronize);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        validateSource(synchronize.getSource(),sourceDescription, false);

        LOG.info("Synchronize the data.");
        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(14)));

        validateSource(synchronize.getDestination(),sourceDescription, false);
    }

    @Test
    @Order(3)
    public void gatherWithDelete() throws Exception {
        LOG.info("Delete with Gather Testing");

        // Setup the source
        Source gatherSource = createSource("./target/it_test/source");

        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, sourceDirectory);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filesInserted", is(2)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[0].problems", is(false)));

        validateSource(gatherSource,sourceDescription,true);
        Assert.assertTrue(Files.exists(new File(sourceDirectory + "/Documents/Text1.txt").toPath()));


        //Text1.txt
        ActionConfirm deleteAction = new ActionConfirm();
        deleteAction.setAction(ActionConfirmType.AC_DELETE);
        deleteAction.setConfirmed(true);
        boolean found = false;
        for(FileInfo nextFile : fileRepository.findAllByOrderByIdAsc()) {
            if(nextFile.getName().equalsIgnoreCase("Text1.txt")) {
                deleteAction.setFileInfo(nextFile);
                found = true;
            }
        }
        Assert.assertTrue(found);
        actionConfirmRepository.save(deleteAction);

        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(1)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(1)))
                .andExpect(jsonPath("$[0].problems", is(false)));
        Assert.assertFalse(Files.exists(new File(sourceDirectory + "/Documents/Text1.txt").toPath()));

        fileRepository.deleteAll();

        List<DirectoryInfo> dbDirectories = new ArrayList<>(directoryRepository.findAllByOrderByIdAsc());
        for(DirectoryInfo nextDirectory : dbDirectories) {
            nextDirectory.setParent(null);
            directoryRepository.save(nextDirectory);
        }

        directoryRepository.deleteAll();
        sourceRepository.delete(gatherSource);
    }
}
