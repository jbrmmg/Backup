package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.*;
import com.jbr.middletier.backup.manager.ActionManager;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import com.jbr.middletier.backup.summary.Summary;
import org.apache.commons.io.FileUtils;
import org.junit.*;
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
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {SyncApiIT.Initializer.class})
@ActiveProfiles(value="it")
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
    BackupManager backupManager;

    @Autowired
    FileSystemObjectManager fileSystemObjectManager;

    @Autowired
    AssociatedFileDataManager associatedFileDataManager;

    @Autowired
    ActionManager actionManager;

    private Source source;
    private Source destination;
    private ImportSource importSource;
    private Synchronize synchronize;

    @Before
    public void setupClassification() throws IOException, InvalidClassificationIdException, InvalidLocationIdException, SourceAlreadyExistsException, InvalidSourceIdException, SynchronizeAlreadyExistsException, ClassificationIdException {
        backupManager.clearMessageCache();

        addClassification(associatedFileDataManager,".*\\._\\.ds_store$", ClassificationActionType.CA_DELETE, 1, false, false, false);
        addClassification(associatedFileDataManager,".*\\.ds_store$", ClassificationActionType.CA_IGNORE, 2, true, false, false);
        addClassification(associatedFileDataManager,".*\\.heic$", ClassificationActionType.CA_BACKUP, 2, false, true, false);
        addClassification(associatedFileDataManager,".*\\.mov$", ClassificationActionType.CA_BACKUP, 2, false, false, true);
        addClassification(associatedFileDataManager,".*\\.mp4$", ClassificationActionType.CA_BACKUP, 2, false, false, true);

        // Update JPG so it gets an MD5
        for(Classification nextClassification : associatedFileDataManager.internalFindAllClassification()) {
            if(nextClassification.getRegex().contains("jpg")) {
                ClassificationDTO updateClassification = new ClassificationDTO();
                updateClassification.setId(nextClassification.getId());
                updateClassification.setIcon(nextClassification.getIcon());
                updateClassification.setRegex(nextClassification.getRegex());
                updateClassification.setAction(nextClassification.getAction());
                updateClassification.setVideo(nextClassification.getIsVideo());
                updateClassification.setOrder(1);
                updateClassification.setImage(true);
                updateClassification.setUseMD5(true);

                associatedFileDataManager.updateClassification(updateClassification);
            }
        }

        // During this test create files in the following directories
        deleteDirectoryContents(new File(sourceDirectory).toPath());
        Files.createDirectories(new File(sourceDirectory).toPath());

        deleteDirectoryContents(new File(destinationDirectory).toPath());
        Files.createDirectories(new File(destinationDirectory).toPath());

        deleteDirectoryContents(new File(importDirectory).toPath());
        Files.createDirectories(new File(importDirectory).toPath());

        deleteDirectoryContents(new File(preImportDirectory).toPath());
        Files.createDirectories(new File(preImportDirectory).toPath());

        // Create the standard sources
        Optional<Location> existingLocation = associatedFileDataManager.findLocationById(1);
        if(!existingLocation.isPresent())
            fail();

        LocationDTO location = new LocationDTO(existingLocation.get());
        location.setCheckDuplicates(true);
        associatedFileDataManager.updateLocation(location);

        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setLocation(new LocationDTO(existingLocation.get()));
        sourceDTO.setStatus(SourceStatusType.SST_OK);
        sourceDTO.setPath(sourceDirectory);

        this.source = associatedFileDataManager.createSource(sourceDTO);

        sourceDTO = new SourceDTO();
        sourceDTO.setLocation(new LocationDTO(existingLocation.get()));
        sourceDTO.setStatus(SourceStatusType.SST_OK);
        sourceDTO.setPath(destinationDirectory);

        this.destination = associatedFileDataManager.createSource(sourceDTO);

        ImportSourceDTO importSourceDTO = new ImportSourceDTO();
        importSourceDTO.setLocation(new LocationDTO(existingLocation.get()));
        importSourceDTO.setStatus(SourceStatusType.SST_OK);
        importSourceDTO.setPath(importDirectory);
        importSourceDTO.setDestinationId(this.source.getIdAndType().getId());

        this.importSource = associatedFileDataManager.createImportSource(importSourceDTO);

        PreImportSourceDTO preImportSourceDTO = new PreImportSourceDTO();
        preImportSourceDTO.setLocation(new LocationDTO(existingLocation.get()));
        preImportSourceDTO.setStatus(SourceStatusType.SST_OK);
        preImportSourceDTO.setPath(importDirectory);

        associatedFileDataManager.createPreImportSource(preImportSourceDTO);

        // Create the source and synchronise entries
        SynchronizeDTO synchronizeDTO = new SynchronizeDTO();
        synchronizeDTO.setId(1);
        synchronizeDTO.setSource(new SourceDTO(this.source));
        synchronizeDTO.setDestination(new SourceDTO(this.destination));

        this.synchronize = associatedFileDataManager.createSynchronize(synchronizeDTO);
    }

    @After
    public void cleanUpTest() {
        // Remove the sources, files & directories.
        associatedFileDataManager.deleteAllSynchronize();
        actionManager.deleteAllActions();
        fileSystemObjectManager.deleteAllFileObjects();
        associatedFileDataManager.deleteAllPreImportSource();
        associatedFileDataManager.deleteAllImportSource();
        associatedFileDataManager.deleteAllSource();
    }

    @Test
    public void gather() throws Exception {
        LOG.info("Synchronize Testing");

        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(1)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(1)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        validateSource(fileSystemObjectManager, synchronize.getSource(),sourceDescription);

        // Update the directory structure
        sourceDescription = getTestStructure("test2");
        deleteDirectoryContents(new File(sourceDirectory).toPath());
        Files.createDirectories(new File(sourceDirectory).toPath());
        copyFiles(sourceDescription, sourceDirectory);

        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(13)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(10)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        validateSource(fileSystemObjectManager, synchronize.getSource(),sourceDescription);

        // Update the directory structure again.
        sourceDescription = getTestStructure("test3");
        deleteDirectoryContents(new File(sourceDirectory).toPath());
        Files.createDirectories(new File(sourceDirectory).toPath());
        copyFiles(sourceDescription, sourceDirectory);

        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(3)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(1)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        validateSource(fileSystemObjectManager, synchronize.getSource(),sourceDescription);

        // Test the get file.
        List<FileSystemObject> files = new ArrayList<>();
        fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_FILE).forEach(files::add);
        Assert.assertNotEquals(0, files.size());
        getMockMvc().perform(get("/jbr/int/backup/file?id="+files.get(0).getIdAndType().getId())
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
    }

    @Test
    public void gatherIgnore() throws Exception {
        LOG.info("Synchronize Testing");

        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test15");
        copyFiles(sourceDescription, sourceDirectory);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(13)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(11)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        sourceDescription = getTestStructure("test15_chk");
        validateSource(fileSystemObjectManager, synchronize.getSource(),sourceDescription);
    }

    @Test
    public void getFileInvalidId() throws Exception {
        // Check that the various get file URL's will fail for invalid id.
        int missingId = 1;
        String error = Objects.requireNonNull(getMockMvc().perform(get("/jbr/int/backup/file?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("File with id ("+missingId+") not found.", error);

        error = Objects.requireNonNull(getMockMvc().perform(get("/jbr/int/backup/fileImage?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("File with id ("+missingId+") not found.", error);

        error = Objects.requireNonNull(getMockMvc().perform(get("/jbr/int/backup/fileVideo?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("File with id ("+missingId+") not found.", error);
    }

    @Test
    public void getFileInvalidType() throws Exception {
        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, sourceDirectory);

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(14)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(11)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        List<FileInfo> files = new ArrayList<>();
        List<DirectoryInfo> directories = new ArrayList<>();
        fileSystemObjectManager.loadByParent(synchronize.getSource().getIdAndType().getId(), directories, files);
        Optional<FileInfo> testFile = Optional.empty();
        for(FileInfo nextFile : files) {
            if(nextFile.getName().equals("Bills.ods")) {
                testFile = Optional.of(nextFile);
            }
        }
        Assert.assertTrue(testFile.isPresent());

        String error = Objects.requireNonNull(getMockMvc().perform(get("/jbr/int/backup/fileImage?id=" + testFile.get().getIdAndType().getId())
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("File is not of type image", error);

        error = Objects.requireNonNull(getMockMvc().perform(get("/jbr/int/backup/fileVideo?id=" + testFile.get().getIdAndType().getId())
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("File is not of type video", error);
    }

    @Test
    public void synchronize() throws Exception {
        LOG.info("Synchronize Testing");

        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, sourceDirectory);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(14)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(11)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);

        LOG.info("Synchronize the data.");
        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(11)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(11)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(1)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(1)));

        // Check that no errors.
        Assert.assertEquals(1, backupManager.getMessageCache(BackupManager.webLogLevel.WARN).size());
        Assert.assertEquals(0, backupManager.getMessageCache(BackupManager.webLogLevel.ERROR).size());

        LOG.info("Gather the data again.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(1)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        sourceDescription = getTestStructure("test2_sync");
        validateSource(fileSystemObjectManager, synchronize.getDestination(), sourceDescription);

        sourceDescription = getTestStructure("test2_post_sync");
        validateSource(fileSystemObjectManager, synchronize.getSource(), sourceDescription);

        // Find file id's there can be used in the next test.
        int missingId = 1;
        int validId = -1;
        int imageId = -1;
        int videoId = -1;
        List<Integer> usedIds = new ArrayList<>();
        List<FileInfo> files = new ArrayList<>();
        List<DirectoryInfo> directories = new ArrayList<>();
        fileSystemObjectManager.loadByParent(synchronize.getSource().getIdAndType().getId(), directories, files);
        for(FileInfo nextFile : files) {
            usedIds.add(nextFile.getIdAndType().getId());
            switch (nextFile.getName()) {
                case "Bills.ods":
                    validId = nextFile.getIdAndType().getId();
                    break;
                case "IMG_8231.jpg":
                    imageId = nextFile.getIdAndType().getId();
                    break;
                case "Cycle.MOV":
                    videoId = nextFile.getIdAndType().getId();
                    break;
                default:
                    // No more options are required.
            }
        }
        while(usedIds.contains(missingId)) {
            missingId++;
        }

        // Check get file info.
        getMockMvc().perform(get("/jbr/int/backup/file?id=" + validId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("file.name", is("Bills.ods")))
                .andExpect(jsonPath("backups[0].name", is("Bills.ods")));

        // Get the image file.
        getMockMvc().perform(get("/jbr/int/backup/fileImage?id=" + imageId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Get the video file.
        getMockMvc().perform(get("/jbr/int/backup/fileVideo?id=" + videoId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        LOG.info("Get the hierarchy");
        HierarchyResponse hierarchyResponse = new HierarchyResponse();
        getMockMvc().perform(post("/jbr/int/backup/hierarchy")
                        .content(this.json(hierarchyResponse))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Request another level
        hierarchyResponse.setId(this.source.getIdAndType().getId());
        getMockMvc().perform(post("/jbr/int/backup/hierarchy")
                        .content(this.json(hierarchyResponse))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].displayName", containsInAnyOrder("", "Photo", "Documents")));

        // Request another level
        int directoryId = -1;
        files = new ArrayList<>();
        directories = new ArrayList<>();
        fileSystemObjectManager.loadByParent(this.source.getIdAndType().getId(), directories, files);
        for(DirectoryInfo nextDirectory : directories) {
            if(nextDirectory.getName().equals("Documents")) {
                directoryId = nextDirectory.getIdAndType().getId();
            }
        }
        hierarchyResponse.setId(directoryId);
        getMockMvc().perform(post("/jbr/int/backup/hierarchy")
                        .content(this.json(hierarchyResponse))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));

        LOG.info("Check for duplicates");
        getMockMvc().perform(post("/jbr/int/backup/duplicate")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].checked", is(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[1].checked", is(1)))
                .andExpect(jsonPath("$[1].failed", is(false)));

        LOG.info("Check Summary");
        getMockMvc().perform(get("/jbr/int/backup/summary")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("valid", is(true)));
    }

    @Test
    public void gatherWithDelete() throws Exception {
        LOG.info("Delete with Gather Testing");

        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, sourceDirectory);

        // Remove the destination source or this test.
        associatedFileDataManager.internalDeleteSynchronize(this.synchronize);
        associatedFileDataManager.internalDeleteSource(this.destination);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(2)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        validateSource(fileSystemObjectManager, this.source,sourceDescription);
        Assert.assertTrue(Files.exists(new File(sourceDirectory + "/Documents/Text1.txt").toPath()));

        Optional<FileInfo> deleteFile = Optional.empty();
        List<FileInfo> files = new ArrayList<>();
        List<DirectoryInfo> directories = new ArrayList<>();
        fileSystemObjectManager.loadByParent(this.source.getIdAndType().getId(),directories,files);
        for(FileInfo nextFile : files) {
            if(nextFile.getName().equalsIgnoreCase("Text1.txt")) {
                deleteFile = Optional.of(nextFile);
            }
        }
        Assert.assertTrue(deleteFile.isPresent());
        ActionConfirmDTO action =  actionManager.createFileDeleteAction(deleteFile.get());
        ConfirmActionRequest confirmRequest = new ConfirmActionRequest();
        confirmRequest.setId(action.getId());
        confirmRequest.setConfirm(true);
        confirmRequest.setParameter("");
        actionManager.confirmAction(confirmRequest);

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
                .andExpect(jsonPath("$[0].failed", is(false)));
        Assert.assertFalse(Files.exists(new File(sourceDirectory + "/Documents/Text1.txt").toPath()));
    }

    @Test
    public void importTestInvalidPath() throws Exception {
        initialiseDirectories();

        SourceDTO updateSource = new SourceDTO(this.importSource);
        updateSource.setPath(importDirectory + "x");

        associatedFileDataManager.updateSource(updateSource);

        LOG.info("Gather the data.");
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/int/backup/import")
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("The path does not exist - " + importDirectory + "x", error);
    }

    @Test
    public void importTestNotSetup() throws Exception {
        initialiseDirectories();

        // Check that it fails if the request has not been sent
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/int/backup/importprocess")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("There is no import source defined.", error);
    }

    @Test
    public void importTest() throws Exception {
        LOG.info("Delete with Gather Testing");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm");

         // During this test create files in the following directories
        initialiseDirectories();

        List<StructureDescription> sourceDescription = getTestStructure("test6_src");
        copyFiles(sourceDescription, sourceDirectory);

        sourceDescription = getTestStructure("test6");
        copyFiles(sourceDescription, preImportDirectory);

        sourceDescription = getTestStructure("test6_3");
        copyFiles(sourceDescription, importDirectory);

        // Insert an ignore file to check it doesn't interfere.
        IgnoreFile ignoreFile = new IgnoreFile();
        ignoreFile.setDate(LocalDateTime.parse("2022-05-01-23-27",formatter));
        ignoreFile.setName("Text.txt");
        ignoreFile.setMD5(new MD5("C714A0B2E792EB102F706DC2424BAA83"));
        ignoreFile.setSize(523);
        fileSystemObjectManager.save(ignoreFile);
        ignoreFile = new IgnoreFile();
        ignoreFile.setDate(LocalDateTime.parse("2022-05-01-23-27",formatter));
        ignoreFile.setName("Text.txt");
        ignoreFile.setMD5(new MD5("C714A0B2E792EB102F706DC2424BAA83"));
        ignoreFile.setSize(12);
        fileSystemObjectManager.save(ignoreFile);

        // Perform the gather - should be 5 files, 1 directory.
        LOG.info("Convert the files");
        getMockMvc().perform(post("/jbr/int/backup/convert")
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filesInserted", is(6)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(1)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        LOG.info("Gather the files");
        getMockMvc().perform(post("/jbr/int/backup/import")
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filesInserted", is(6)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(1)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        // Verify that the database matches the real world.
        ImportSource importSource = null;
        for(ImportSource nextImportSource : associatedFileDataManager.internalFindAllImportSource()) {
            // Only one is expected
            importSource = nextImportSource;
        }
        validateSource(fileSystemObjectManager, importSource, sourceDescription);

        // Check that the database contains 5 files.
        getMockMvc().perform(get("/jbr/int/backup/importfiles")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].filename", is("Bills.ods")))
                .andExpect(jsonPath("$[0].status", is(ImportFileStatusType.IFS_READ.toString())))
                .andExpect(jsonPath("$[1].filename", is("GetRid.ds_store")))
                .andExpect(jsonPath("$[1].status", is(ImportFileStatusType.IFS_READ.toString())))
                .andExpect(jsonPath("$[2].filename", is("Letter.jpg")))
                .andExpect(jsonPath("$[2].status", is(ImportFileStatusType.IFS_READ.toString())))
                .andExpect(jsonPath("$[3].filename", is("Statement.jpg")))
                .andExpect(jsonPath("$[3].status", is(ImportFileStatusType.IFS_READ.toString())))
                .andExpect(jsonPath("$[4].filename", is("Text.bscf")))
                .andExpect(jsonPath("$[4].status", is(ImportFileStatusType.IFS_READ.toString())))
                .andExpect(jsonPath("$[5].filename", is("Text.txt")))
                .andExpect(jsonPath("$[5].status", is(ImportFileStatusType.IFS_READ.toString())));

        // Import these files - this should create the actions.
        getMockMvc().perform(post("/jbr/int/backup/importprocess")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].nonBackupClassification", is(1)))
                .andExpect(jsonPath("$[0].ignoredImports", is(0)))
                .andExpect(jsonPath("$[0].alreadyImported", is(0)))
                .andExpect(jsonPath("$[0].ignored", is(0)))
                .andExpect(jsonPath("$[0].imported", is(0)));

        // Set up the actions that will be performed.
        for(ActionConfirmDTO nextAction : actionManager.externalFindByConfirmed(false)) {
            ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
            confirmActionRequest.setId(nextAction.getId());
            confirmActionRequest.setConfirm(true);

            if(nextAction.getFileName().equals("Statement.jpg")) {
                confirmActionRequest.setParameter("Blah");
                actionManager.confirmAction(confirmActionRequest);
            } else if (nextAction.getFileName().equals("Letter.jpg")) {
                confirmActionRequest.setParameter("ignore");
                actionManager.confirmAction(confirmActionRequest);
            }
        }

        // Remove any files from the database that have been deleted.
        getMockMvc().perform(delete("/jbr/int/backup/import")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deletes", is(1)))
                .andExpect(jsonPath("$[0].filesInserted", is(6)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(1)));

        // Import should now not have the deleted file.
        sourceDescription = getTestStructure("test6_1");
        validateSource(fileSystemObjectManager, importSource, sourceDescription);

        // Reset the status of the files.
        getMockMvc().perform(put("/jbr/int/backup/importfiles")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].filename", is("Bills.ods")))
                .andExpect(jsonPath("$[0].status", is(ImportFileStatusType.IFS_READ.toString())))
                .andExpect(jsonPath("$[1].filename", is("Letter.jpg")))
                .andExpect(jsonPath("$[1].status", is(ImportFileStatusType.IFS_READ.toString())))
                .andExpect(jsonPath("$[2].filename", is("Statement.jpg")))
                .andExpect(jsonPath("$[2].status", is(ImportFileStatusType.IFS_READ.toString())))
                .andExpect(jsonPath("$[3].filename", is("Text.bscf")))
                .andExpect(jsonPath("$[3].status", is(ImportFileStatusType.IFS_READ.toString())))
                .andExpect(jsonPath("$[4].filename", is("Text.txt")))
                .andExpect(jsonPath("$[4].status", is(ImportFileStatusType.IFS_READ.toString())));

        // Perform the import again - this should perform the actions.
        getMockMvc().perform(post("/jbr/int/backup/importprocess")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].nonBackupClassification", is(0)))
                .andExpect(jsonPath("$[0].ignoredImports", is(0)))
                .andExpect(jsonPath("$[0].alreadyImported", is(0)))
                .andExpect(jsonPath("$[0].ignored", is(1)))
                .andExpect(jsonPath("$[0].imported", is(1)));

        // Import should still look the same.
        validateSource(fileSystemObjectManager, importSource, sourceDescription);

        // Gather the data from the source.
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(2)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(3)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        // Check that the source has files.
        sourceDescription = getTestStructure("test6_2");
        validateSource(fileSystemObjectManager, this.source, sourceDescription);

        // Reset the import again.
        sourceDescription = getTestStructure("test6");
        copyFiles(sourceDescription, importDirectory);

        LOG.info("Reset the data.");
        getMockMvc().perform(post("/jbr/int/backup/import")
                        .content(this.json("test"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filesInserted", is(6)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(1)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        // Re process the imports
        getMockMvc().perform(post("/jbr/int/backup/importprocess")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].nonBackupClassification", is(1)))
                .andExpect(jsonPath("$[0].ignoredImports", is(1)))
                .andExpect(jsonPath("$[0].alreadyImported", is(1)))
                .andExpect(jsonPath("$[0].ignored", is(0)))
                .andExpect(jsonPath("$[0].imported", is(0)));

        // Check the ignore files.
        getMockMvc().perform(get("/jbr/int/backup/ignore")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].filename", is("Letter.jpg")));

        // Reset the information.
        initialiseDirectories();
        getMockMvc().perform(post("/jbr/int/backup/import")
                        .content(this.json("test"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));
    }

    @Test
    public void moreFileProcessTesting() throws Exception {
        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, sourceDirectory);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(14)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(11)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);

        // Get the database files
        getMockMvc().perform(get("/jbr/int/backup/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(14)))
                .andExpect(jsonPath("$[0].filename", is("Backup.dxf~")))
                .andExpect(jsonPath("$[0].type", is(FileSystemObjectType.FSO_FILE.toString())))
                .andExpect(jsonPath("$[0].date", startsWith("1998-04-10")))
                .andExpect(jsonPath("$[0].size", is(12)))
                .andExpect(jsonPath("$[0].md5.set", is(false)))
                .andExpect(jsonPath("$[0].parentType", is(FileSystemObjectType.FSO_DIRECTORY.toString())))
                .andExpect(jsonPath("$[1].filename", is("Bills.ods")))
                .andExpect(jsonPath("$[2].filename", is("Cycle.MOV")))
                .andExpect(jsonPath("$[3].filename", is("GetRid.ds_store")))
                .andExpect(jsonPath("$[4].filename", is("IMG_2329.HEIC")))
                .andExpect(jsonPath("$[5].filename", is("IMG_3891.jpeg")))
                .andExpect(jsonPath("$[6].filename", is("IMG_8231.jpg")))
                .andExpect(jsonPath("$[6].md5.set", is(true)))
                .andExpect(jsonPath("$[6].md5.value", is("C714A0B2E792EB102F706DC2424B0083")))
                .andExpect(jsonPath("$[7].filename", is("IMG_931d.png")))
                .andExpect(jsonPath("$[8].filename", is("Letter.odt")))
                .andExpect(jsonPath("$[9].filename", is("NotHere._.ds_store")))
                .andExpect(jsonPath("$[10].filename", is("Party.mp4")))
                .andExpect(jsonPath("$[11].filename", is("Statement.pdf")))
                .andExpect(jsonPath("$[12].filename", is("Text.txt")))
                .andExpect(jsonPath("$[13].filename", is("Text.txt")));

        // Perform a delete.
        int missingId = 1;
        int validId = -1;
        List<Integer> usedIds = new ArrayList<>();
        List<FileInfo> files = new ArrayList<>();
        List<DirectoryInfo> directories = new ArrayList<>();
        fileSystemObjectManager.loadByParent(this.source.getIdAndType().getId(), directories, files);
        for(FileInfo nextFile : files) {
            usedIds.add(nextFile.getIdAndType().getId());
            if(nextFile.getName().equals("Bills.ods")) {
                validId = nextFile.getIdAndType().getId();
            }
        }
        Assert.assertNotEquals(-1,validId);
        while(usedIds.contains(missingId)) {
            missingId++;
        }

        getMockMvc().perform(delete("/jbr/int/backup/file?id=" + validId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("fileName", is("Bills.ods")));

        String error = Objects.requireNonNull(getMockMvc().perform(delete("/jbr/int/backup/file?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("File with id ("+missingId+") not found.", error);
    }

    @Test
    public void testActionApi() throws Exception {
        // Need a file for the actions
        FileInfo file = new FileInfo();
        file.setName("Testing.txt");
        fileSystemObjectManager.save(file);

        // Setup some actions.
        ActionConfirmDTO deleteAction = actionManager.createFileDeleteAction(file);
        ActionConfirmDTO importAction = actionManager.createFileImportAction(file,"C");

        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(importAction.getId());
        confirmActionRequest.setParameter("Blah");
        actionManager.confirmAction(confirmActionRequest);

        // Get the database files
        getMockMvc().perform(get("/jbr/int/backup/actions")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fileName", is("Testing.txt")))
                .andExpect(jsonPath("$[0].action", is(ActionConfirmType.AC_DELETE.getTypeName())));

        // Get the database files
        getMockMvc().perform(get("/jbr/int/backup/confirmed-actions")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fileName", is("Testing.txt")))
                .andExpect(jsonPath("$[0].action", is(ActionConfirmType.AC_IMPORT.getTypeName())));

        ConfirmActionRequest request = new ConfirmActionRequest();
        request.setId(deleteAction.getId());
        request.setConfirm(true);
        request.setParameter("Blha");
        getMockMvc().perform(post("/jbr/int/backup/actions")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/backup/actionemail")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/int/backup/summary")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("valid", is(true)));
    }

    @Test
    public void testAssociateFileDataManager() throws Exception {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setName("Test");

        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setId(1);
        sourceDTO.setPath("Test");
        sourceDTO.setLocation(locationDTO);

        String error = Objects.requireNonNull(getMockMvc().perform(put("/jbr/ext/backup/source")
                        .content(this.json(sourceDTO))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Source with id (1) not found.", error);

        associatedFileDataManager.internalFindSourceById(this.source.getIdAndType().getId());

        sourceDTO = new SourceDTO();
        sourceDTO.setId(this.source.getIdAndType().getId());
        error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/ext/backup/source")
                        .content(this.json(sourceDTO))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Source with id (" + sourceDTO.getId() + ") already exists.", error);
    }

    @Test
    public void checkActionInvalid() throws Exception {
        ConfirmActionRequest request = new ConfirmActionRequest();
        request.setId(1);
        request.setConfirm(true);
        request.setParameter("Blha");
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/int/backup/actions")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Action 1 not found.", error);
    }

    @Test
    public void duplicateTesting() throws Exception {
        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test7");
        copyFiles(sourceDescription, sourceDirectory);

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(3)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        LOG.info("Check for duplicates");
        getMockMvc().perform(post("/jbr/int/backup/duplicate")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].checked", is(1)))
                .andExpect(jsonPath("$[0].deleted", is(0)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[1].checked", is(0)))
                .andExpect(jsonPath("$[1].deleted", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)));

        // Confirm one of the actions.
        List<ActionConfirmDTO> actions = actionManager.externalFindByConfirmed(false);
        int count = 0;
        int actionId = -1;
        for(ActionConfirmDTO next : actions) {
            count++;
            actionId = next.getId();
        }
        Assert.assertEquals(2, count);
        Assert.assertNotEquals(-1, actionId);

        // Confirm the action.
        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(actionId);
        confirmActionRequest.setParameter("");
        confirmActionRequest.setConfirm(true);

        getMockMvc().perform(post("/jbr/int/backup/actions")
                        .content(this.json(confirmActionRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/backup/duplicate")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].checked", is(1)))
                .andExpect(jsonPath("$[0].deleted", is(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[1].checked", is(0)))
                .andExpect(jsonPath("$[1].deleted", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)));
    }

    @Test
    public void duplicateTestingWithMD5() throws Exception {
        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test8");
        copyFiles(sourceDescription, sourceDirectory);

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(3)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        LOG.info("Check for duplicates");
        getMockMvc().perform(post("/jbr/int/backup/duplicate")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].checked", is(1)))
                .andExpect(jsonPath("$[0].deleted", is(0)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[1].checked", is(0)))
                .andExpect(jsonPath("$[1].deleted", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)));

        // Confirm one of the actions.
        List<ActionConfirmDTO> actions = actionManager.externalFindByConfirmed(false);
        int count = 0;
        int actionId = -1;
        for(ActionConfirmDTO next : actions) {
            count++;
            actionId = next.getId();
        }
        Assert.assertEquals(2, count);
        Assert.assertNotEquals(-1, actionId);

        // Confirm the action.
        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(actionId);
        confirmActionRequest.setParameter("");
        confirmActionRequest.setConfirm(true);

        getMockMvc().perform(post("/jbr/int/backup/actions")
                        .content(this.json(confirmActionRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/backup/duplicate")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].checked", is(1)))
                .andExpect(jsonPath("$[0].deleted", is(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[1].checked", is(0)))
                .andExpect(jsonPath("$[1].deleted", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)));
    }

    @Test
    public void syncWithDelete() throws Exception {
        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, sourceDirectory);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(14)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(11)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);

        LOG.info("Synchronize the data.");
        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(11)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(11)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(1)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(1)));

        // Find the file that we will delete.
        ArrayList<FileInfo> files = new ArrayList<>();
        ArrayList<DirectoryInfo> directories = new ArrayList<>();

        fileSystemObjectManager.loadByParent(source.getIdAndType().getId(), directories, files);

        int deleteId = -1;
        for(FileInfo nextFile : files) {
            if(nextFile.getName().equals("Letter.odt")) {
                deleteId = nextFile.getIdAndType().getId();
            }
        }
        Assert.assertNotEquals(-1, deleteId);

        getMockMvc().perform(delete("/jbr/int/backup/file?id=" + deleteId)
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Find the action and confirm it.
        List<ActionConfirmDTO> actions = actionManager.externalFindByConfirmed(false);
        int count = 0;
        int actionId = -1;
        for(ActionConfirmDTO next : actions) {
            count++;
            actionId = next.getId();
        }
        Assert.assertEquals(1, count);
        Assert.assertNotEquals(-1, actionId);

        // Confirm the action.
        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(actionId);
        confirmActionRequest.setParameter("");
        confirmActionRequest.setConfirm(true);

        getMockMvc().perform(post("/jbr/int/backup/actions")
                        .content(this.json(confirmActionRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Process the gather and syncs again.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].deletes", is(1)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(2)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(11)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(2)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(1)));

        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(0)))
                .andExpect(jsonPath("$[0].filesDeleted", is(1)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(1)));

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)));
    }

    @Test
    public void testSyncWithFilesRemoved() throws Exception {
        // Check what happens when a synced directory has a file removed
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test9");
        copyFiles(sourceDescription, sourceDirectory);

        // Gather the files.
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(4)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(1)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);

        LOG.info("Synchronize the data.");
        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(4)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(1)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(4)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(1)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        File fileToDelete = new File(sourceDirectory + "/Documents/Bills.ods");
        Files.deleteIfExists(fileToDelete.toPath());

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(1)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesDeleted", is(1)))
                .andExpect(jsonPath("$[0].filesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        // Find the action and confirm it.
        List<ActionConfirmDTO> actions = actionManager.externalFindByConfirmed(false);
        int count = 0;
        int actionId = -1;
        for(ActionConfirmDTO next : actions) {
            count++;
            actionId = next.getId();
        }
        Assert.assertEquals(1, count);
        Assert.assertNotEquals(-1, actionId);

        // Confirm the action.
        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(actionId);
        confirmActionRequest.setParameter("");
        confirmActionRequest.setConfirm(true);

        getMockMvc().perform(post("/jbr/int/backup/actions")
                        .content(this.json(confirmActionRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesDeleted", is(1)))
                .andExpect(jsonPath("$[0].filesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(1)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));
    }

    @Test
    public void testSyncWithDirectoryRemoved() throws Exception {
        // Check what happens when a synced directory has a file removed
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test10");
        copyFiles(sourceDescription, sourceDirectory);

        // Gather the files.
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(4)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);

        LOG.info("Synchronize the data.");
        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(4)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(2)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(4)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        File directoryToDelete = new File(sourceDirectory + "/Documents/sub");
        FileUtils.deleteDirectory(directoryToDelete);

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(1)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(1)))
                .andExpect(jsonPath("$[0].filesRemoved", is(1)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesDeleted", is(1)))
                .andExpect(jsonPath("$[0].filesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(1)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        // Find the action and confirm it.
        List<ActionConfirmDTO> actions = actionManager.externalFindByConfirmed(false);
        int count = 0;
        int actionId = -1;
        for(ActionConfirmDTO next : actions) {
            count++;
            actionId = next.getId();
        }
        Assert.assertEquals(1, count);
        Assert.assertNotEquals(-1, actionId);

        // Confirm the action.
        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(actionId);
        confirmActionRequest.setParameter("");
        confirmActionRequest.setConfirm(true);

        getMockMvc().perform(post("/jbr/int/backup/actions")
                        .content(this.json(confirmActionRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesDeleted", is(1)))
                .andExpect(jsonPath("$[0].filesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(1)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(1)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(1)))
                .andExpect(jsonPath("$[1].deletes", is(0)));
    }

    @Test
    public void testSyncFileToDirectory() throws Exception {
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test12_2");
        copyFiles(sourceDescription, sourceDirectory);

        // Gather the files.
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(2)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(1)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);

        initialiseDirectories();
        sourceDescription = getTestStructure("test12");
        copyFiles(sourceDescription, sourceDirectory);

        // Gather the files.
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(1)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(1)))
                .andExpect(jsonPath("$[0].filesRemoved", is(1)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);
    }

    @Test
    public void testSyncDirectoryToFile() throws Exception {
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test12");
        copyFiles(sourceDescription, sourceDirectory);

        // Gather the files.
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(2)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(2)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);

        initialiseDirectories();
        sourceDescription = getTestStructure("test12_2");
        copyFiles(sourceDescription, sourceDirectory);

        // Gather the files.
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(1)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(1)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(1)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);
    }

    @Test
    public void testSyncEqualiseDate() throws Exception {
        // Check what happens when a synced directory has a file removed
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test11");
        copyFiles(sourceDescription, sourceDirectory);

        List<StructureDescription> destinationDescription = getTestStructure("test11_dest");
        copyFiles(destinationDescription, destinationDirectory);

        // Gather the files.
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(2)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(1)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(2)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(1)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);
        validateSource(fileSystemObjectManager,synchronize.getDestination(),destinationDescription);

        // Perform the sync.
        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(1)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted", is(1)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getDestination(),sourceDescription);
    }

    @Test
    public void testSyncSourceBusy() throws Exception {
        // Check what happens when a synced directory has a file removed
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, sourceDirectory);

        // Remove the destination
        associatedFileDataManager.updateSourceStatus(this.destination, SourceStatusType.SST_GATHERING);

        // Gather the files.
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(2)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)))
                .andExpect(jsonPath("$[1].failed", is(true)))
                .andExpect(jsonPath("$[1].filesInserted", is(0)))
                .andExpect(jsonPath("$[1].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);
    }

    @Test
    public void testSummary() throws Exception {
        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, sourceDirectory);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(14)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(11)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        Summary.forceInstance(associatedFileDataManager, fileSystemObjectManager);
        Summary summary = Summary.getInstance(associatedFileDataManager, fileSystemObjectManager);

        Assert.assertTrue(summary.isValid());
        List<SourceDTO> sources = summary.getSources();
        Assert.assertEquals(2,sources.size());
        Assert.assertEquals(14,sources.get(0).getFileCount());
        Assert.assertEquals(11,sources.get(0).getDirectoryCount());
        Assert.assertEquals(6622444,sources.get(0).getLargestFile());
        Assert.assertEquals(15970561,sources.get(0).getTotalFileSize());
        Assert.assertEquals(0,sources.get(1).getFileCount());
        Assert.assertEquals(0,sources.get(1).getDirectoryCount());
        Assert.assertEquals(0,sources.get(1).getLargestFile());
        Assert.assertEquals(0,sources.get(1).getTotalFileSize());
    }

    @Test
    public void gatherMountCheck() throws Exception {
        LOG.info("Mount check testing");

        // During this test create files in the following directories
        initialiseDirectories();
        this.source.setMountCheck("./target/it_test/import/thisfileismissing.txt");
        associatedFileDataManager.updateSource(this.source.getSourceDTO());

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, sourceDirectory);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(true)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        this.source.setMountCheck(null);
        associatedFileDataManager.updateSource(this.source.getSourceDTO());
    }

    @Test
    public void syncMountCheck() throws Exception {
        LOG.info("Mount check testing");

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
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(2)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        this.source.setMountCheck("./target/it_test/import/thisfileismissing.txt");
        associatedFileDataManager.updateSource(this.source.getSourceDTO());

        // Perform the sync.
        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(true)))
                .andExpect(jsonPath("$[0].filesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        this.source.setMountCheck(null);
        associatedFileDataManager.updateSource(this.source.getSourceDTO());
    }

    @Test
    public void gatherMountCheckPositive() throws Exception {
        LOG.info("Mount check testing");

        initialiseDirectories();
        File checkMountFile = new File("./target/it_test/import/mountcheck.txt");
        Files.createFile(checkMountFile.toPath());

        // During this test create files in the following directories
        this.source.setMountCheck(checkMountFile.toString());
        associatedFileDataManager.updateSource(this.source.getSourceDTO());

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, sourceDirectory);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(2)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        this.source.setMountCheck(null);
        associatedFileDataManager.updateSource(this.source.getSourceDTO());
    }

    @Test
    public void syncMountCheckPositive() throws Exception {
        LOG.info("Mount check testing");

        // During this test create files in the following directories
        initialiseDirectories();
        File checkMountFile = new File("./target/it_test/import/mountcheck.txt");
        Files.createFile(checkMountFile.toPath());

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, sourceDirectory);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(2)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        this.source.setMountCheck(checkMountFile.toString());
        associatedFileDataManager.updateSource(this.source.getSourceDTO());

        // Perform the sync.
        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(2)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(2)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        this.source.setMountCheck(null);
        associatedFileDataManager.updateSource(this.source.getSourceDTO());
    }
}
