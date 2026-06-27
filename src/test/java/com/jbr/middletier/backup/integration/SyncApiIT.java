package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.*;
import com.jbr.middletier.backup.manager.*;
import com.jbr.middletier.backup.summary.Summary;
import org.apache.commons.io.FileUtils;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MiddleTier.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@ContextConfiguration(initializers = {SyncApiIT.Initializer.class})
@ActiveProfiles(value="it")
@Testcontainers
public class SyncApiIT extends FileTester {
    private static final Logger LOG = LoggerFactory.getLogger(SyncApiIT.class);

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
    DbLoggingManager dbLoggingManager;

    @Autowired
    FileSystemObjectManager fileSystemObjectManager;

    @Autowired
    AssociatedFileDataManager associatedFileDataManager;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    ActionManager actionManager;

    private Source source;
    private Source destination;
    private Synchronize synchronize;

    private ClassificationDTO initialiseClassification(Classification nextClassification) {
        ClassificationDTO updateClassification = new ClassificationDTO();
        updateClassification.setId(nextClassification.getId());
        updateClassification.setIcon(nextClassification.getIcon());
        updateClassification.setRegex(nextClassification.getRegex());
        updateClassification.setAction(nextClassification.getAction());
        updateClassification.setIsVideo(nextClassification.getIsVideo());
        updateClassification.setOrder(1);
        updateClassification.setIsImage(true);
        updateClassification.setCheckMetaData(true);

        return updateClassification;
    }

    @BeforeEach
    void setupClassification() throws IOException, InvalidClassificationIdException, InvalidLocationIdException, SourceAlreadyExistsException, SynchronizeAlreadyExistsException, ClassificationIdException {
        dbLoggingManager.clearMessageCache();

        addClassification(associatedFileDataManager,".*\\._\\.ds_store$", ClassificationActionType.CA_DELETE, 1, false, false);
        addClassification(associatedFileDataManager,".*\\.ds_store$", ClassificationActionType.CA_IGNORE, 2, false, false);
        addClassification(associatedFileDataManager,".*\\.heic$", ClassificationActionType.CA_BACKUP, 2, true, false);
        addClassification(associatedFileDataManager,".*\\.mov$", ClassificationActionType.CA_BACKUP, 2, false, true);
        addClassification(associatedFileDataManager,".*\\.mp4$", ClassificationActionType.CA_BACKUP, 2, false, true);

        // Update the JPG so that it gets metadata.
        for(Classification nextClassification : associatedFileDataManager.findAllClassifications()) {
            if(nextClassification.getRegex().contains("jpg") || nextClassification.getRegex().contains("jpeg")) {
                ClassificationDTO updateClassification = initialiseClassification(nextClassification);
                associatedFileDataManager.updateClassification(associatedFileDataManager.convertToEntity(updateClassification));
            }
        }

        // During this test create files in the following directories
        deleteDirectoryContents(new File(SOURCE_DIRECTORY).toPath());
        Files.createDirectories(new File(SOURCE_DIRECTORY).toPath());

        deleteDirectoryContents(new File(DESTINATION_DIRECTORY).toPath());
        Files.createDirectories(new File(DESTINATION_DIRECTORY).toPath());

        deleteDirectoryContents(new File(IMPORT_DIRECTORY).toPath());
        Files.createDirectories(new File(IMPORT_DIRECTORY).toPath());

        deleteDirectoryContents(new File(PRE_IMPORT_DIRECTORY).toPath());
        Files.createDirectories(new File(PRE_IMPORT_DIRECTORY).toPath());

        // Create the standard sources
        Optional<Location> existingLocation = associatedFileDataManager.findLocationById(1);
        if(existingLocation.isEmpty())
            fail();

        LocationDTO location = associatedFileDataManager.convertToDTO(existingLocation.get());
        location.setCheckDuplicates(true);
        associatedFileDataManager.updateLocation(associatedFileDataManager.convertToEntity(location));

        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setLocation(associatedFileDataManager.convertToDTO(existingLocation.get()));
        sourceDTO.setStatus("OK");
        sourceDTO.setPath(SOURCE_DIRECTORY);
        sourceDTO.setGatherMetaData(true);
        sourceDTO.setPrimary(true);

        this.source = associatedFileDataManager.createSource(associatedFileDataManager.convertToEntity(sourceDTO));

        sourceDTO = new SourceDTO();
        sourceDTO.setLocation(associatedFileDataManager.convertToDTO(existingLocation.get()));
        sourceDTO.setStatus("OK");
        sourceDTO.setPath(DESTINATION_DIRECTORY);

        this.destination = associatedFileDataManager.createSource(associatedFileDataManager.convertToEntity(sourceDTO));

        Optional<Location> importLocation = associatedFileDataManager.findLocationById(4);
        if(importLocation.isEmpty())
            fail();

        ImportSourceDTO importSourceDTO = new ImportSourceDTO();
        importSourceDTO.setLocation(associatedFileDataManager.convertToDTO(importLocation.get()));
        importSourceDTO.setStatus("OK");
        importSourceDTO.setPath(IMPORT_DIRECTORY);
        importSourceDTO.setDestinationId(this.source.getIdAndType().getId());

        PreImportSourceDTO preImportSourceDTO = new PreImportSourceDTO();
        preImportSourceDTO.setLocation(associatedFileDataManager.convertToDTO(importLocation.get()));
        preImportSourceDTO.setStatus("OK");
        preImportSourceDTO.setPath(PRE_IMPORT_DIRECTORY);

        associatedFileDataManager.createPreImportSource(associatedFileDataManager.convertToEntity(preImportSourceDTO));

        // Create the source and synchronise entries
        SynchronizeDTO synchronizeDTO = new SynchronizeDTO();
        synchronizeDTO.setId(1);
        synchronizeDTO.setSource(associatedFileDataManager.convertToDTO(this.source));
        synchronizeDTO.setDestination(associatedFileDataManager.convertToDTO(this.destination));

        this.synchronize = associatedFileDataManager.createSynchronize(associatedFileDataManager.convertToEntity(synchronizeDTO));
    }

    @AfterEach
    void cleanUpTest() {
        // Remove the sources, files and directories.
        associatedFileDataManager.deleteAllSynchronize();
        actionManager.deleteAllActions();
        fileSystemObjectManager.deleteAllFileObjects();
        associatedFileDataManager.deleteAllPreImportSource();
        associatedFileDataManager.deleteAllPostImportSource();
        associatedFileDataManager.deleteAllImportSource();
        associatedFileDataManager.deleteAllSource();
    }

    @Test
    void gather() throws Exception {
        LOG.info("Gather Testing");

        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Perform a gather.
        LOG.info("Gather the data for test1.");
        getMockMvc().perform(post("/api/v1/gather")
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
        deleteDirectoryContents(new File(SOURCE_DIRECTORY).toPath());
        Files.createDirectories(new File(SOURCE_DIRECTORY).toPath());
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        LOG.info("Gather the data for test2.");
        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(13)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(10)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        validateSource(fileSystemObjectManager, synchronize.getSource(), sourceDescription);

        // Update the directory structure again.
        sourceDescription = getTestStructure("test3");
        deleteDirectoryContents(new File(SOURCE_DIRECTORY).toPath());
        Files.createDirectories(new File(SOURCE_DIRECTORY).toPath());
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        LOG.info("Gather the data from test 3.");
        getMockMvc().perform(post("/api/v1/gather")
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
        assertNotEquals(0, files.size());
        getMockMvc().perform(get("/api/v1/files/detail?id="+files.get(0).getIdAndType().getId())
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Check that the metadata was collected on the jpeg file.
        Optional<Integer> idOfJpeg = Optional.empty();
        for(FileSystemObject file : files){
            if(file.getName().toLowerCase().endsWith(".jpeg")) {
                idOfJpeg = Optional.of(file.getIdAndType().getId());
                break;
            }
        }

        assertTrue(idOfJpeg.isPresent());

        // Get the details for the jpeg file.
        getMockMvc().perform(get("/api/v1/files/detail?id="+idOfJpeg.get())
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.file.id", is(idOfJpeg.get())))
                .andExpect(jsonPath("$.metaData.image", is(true)))
                .andExpect(jsonPath("$.metaData.video", is(false)))
                .andExpect(jsonPath("$.metaData.imageHeight", is(3024)))
                .andExpect(jsonPath("$.metaData.imageWidth", is(4032)))
                .andExpect(jsonPath("$.metaData.latitude", is(37.2243778)))
                .andExpect(jsonPath("$.metaData.longitude", is(-115.8154806)));
    }

    @Test
    void gatherIgnore() throws Exception {
        LOG.info("Gather ignore Testing");

        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test15");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Perform a gather.
        LOG.info("Gather the data for test 15.");
        getMockMvc().perform(post("/api/v1/gather")
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
    void getFileInvalidId() throws Exception {
        // Check that the various get file URL's will fail for invalid id.
        int missingId = 1;
        String error = Objects.requireNonNull(getMockMvc().perform(get("/api/v1/files/detail?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        assertEquals("File with id ("+missingId+") not found.", error);

        error = Objects.requireNonNull(getMockMvc().perform(get("/api/v1/files/image?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        assertEquals("File with id ("+missingId+") not found.", error);

        error = Objects.requireNonNull(getMockMvc().perform(get("/api/v1/files/video?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        assertEquals("File with id ("+missingId+") not found.", error);
    }

    @Test
    void getFileInvalidType() throws Exception {
        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        getMockMvc().perform(post("/api/v1/gather")
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
        assertTrue(testFile.isPresent());

        String error = Objects.requireNonNull(getMockMvc().perform(get("/api/v1/files/image?id=" + testFile.get().getIdAndType().getId())
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException()).getMessage();
        assertEquals("File is not of type image", error);

        error = Objects.requireNonNull(getMockMvc().perform(get("/api/v1/files/video?id=" + testFile.get().getIdAndType().getId())
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException()).getMessage();
        assertEquals("File is not of type video", error);
    }

    @Test
    void synchronize() throws Exception {
        LOG.info("Synchronize Testing");

        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Perform a gather.
        LOG.info("Gather the data for test 2 (synchronize).");
        getMockMvc().perform(post("/api/v1/gather")
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
        getMockMvc().perform(post("/api/v1/sync/run")
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
        assertEquals(1, dbLoggingManager.getMessageCache(DbLogType.DLT_WARNING).size());
        assertEquals(0, dbLoggingManager.getMessageCache(DbLogType.DLT_ERROR).size());

        LOG.info("Gather the data again.");
        getMockMvc().perform(post("/api/v1/gather")
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

        // Find the file id's there can be used in the next test.
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
        getMockMvc().perform(get("/api/v1/files/detail?id=" + validId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("file.name", is("Bills.ods")))
                .andExpect(jsonPath("backups[0].name", is("Bills.ods")));

        // Get the image file.
        getMockMvc().perform(get("/api/v1/files/image?id=" + imageId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Get the video file.
        getMockMvc().perform(get("/api/v1/files/video?id=" + videoId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        LOG.info("Get the hierarchy");
        HierarchyResponse hierarchyResponse = new HierarchyResponse();
        getMockMvc().perform(post("/api/v1/hierarchy")
                        .content(this.json(hierarchyResponse))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Request another level
        hierarchyResponse.setId(this.source.getIdAndType().getId());
        getMockMvc().perform(post("/api/v1/hierarchy")
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
        getMockMvc().perform(post("/api/v1/hierarchy")
                        .content(this.json(hierarchyResponse))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));

        LOG.info("Check for duplicates in sync");
        getMockMvc().perform(post("/api/v1/duplicates")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].checked", is(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[1].checked", is(1)))
                .andExpect(jsonPath("$[1].failed", is(false)));

        LOG.info("Check Summary");
        getMockMvc().perform(get("/api/v1/summary")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("valid", is(true)));
    }

    @Test
    void gatherWithDelete() throws Exception {
        LOG.info("Delete with Gather Testing");

        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Remove the destination source or this test.
        associatedFileDataManager.deleteSynchronize(this.synchronize);
        associatedFileDataManager.deleteSource(this.destination);

        // Perform a gather.
        LOG.info("Gather the data for test 4.");
        getMockMvc().perform(post("/api/v1/gather")
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
        assertTrue(Files.exists(new File(SOURCE_DIRECTORY + "/Documents/Text1.txt").toPath()));

        Optional<FileInfo> deleteFile = Optional.empty();
        List<FileInfo> files = new ArrayList<>();
        List<DirectoryInfo> directories = new ArrayList<>();
        fileSystemObjectManager.loadByParent(this.source.getIdAndType().getId(),directories,files);
        for(FileInfo nextFile : files) {
            if(nextFile.getName().equalsIgnoreCase("Text1.txt")) {
                deleteFile = Optional.of(nextFile);
            }
        }
        assertTrue(deleteFile.isPresent());
        ActionConfirmDTO action =  actionManager.createFileDeleteAction(deleteFile.get());
        ConfirmActionRequest confirmRequest = new ConfirmActionRequest();
        confirmRequest.setId(action.getId());
        confirmRequest.setConfirm(true);
        confirmRequest.setParameter("");
        actionManager.confirmAction(confirmRequest);

        LOG.info("Gather the data after delete test.");
        getMockMvc().perform(post("/api/v1/gather")
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
        assertFalse(Files.exists(new File(SOURCE_DIRECTORY + "/Documents/Text1.txt").toPath()));
    }

    @Test
    void moreFileProcessTesting() throws Exception {
        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Perform a gather.
        LOG.info("Gather the data for test 2.");
        getMockMvc().perform(post("/api/v1/gather")
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
        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(14)))
                .andExpect(jsonPath("$[0].filename", is("Backup.dxf~")))
                .andExpect(jsonPath("$[0].type", is("FILE")))
                .andExpect(jsonPath("$[0].date", startsWith("1998-04-10")))
                .andExpect(jsonPath("$[0].size", is(12)))
                .andExpect(jsonPath("$[0].parentType", is("DIRY")))
                .andExpect(jsonPath("$[1].filename", is("Bills.ods")))
                .andExpect(jsonPath("$[2].filename", is("Cycle.MOV")))
                .andExpect(jsonPath("$[3].filename", is("GetRid.ds_store")))
                .andExpect(jsonPath("$[4].filename", is("IMG_2329.HEIC")))
                .andExpect(jsonPath("$[5].filename", is("IMG_3891.jpeg")))
                .andExpect(jsonPath("$[6].filename", is("IMG_8231.jpg")))
                .andExpect(jsonPath("$[6].md5", is("56FDC164DC8A27C015170014821A7DCE")))
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
        assertNotEquals(-1,validId);
        while(usedIds.contains(missingId)) {
            missingId++;
        }

        getMockMvc().perform(delete("/api/v1/file?id=" + validId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("fileName", is("Bills.ods")));

        String error = Objects.requireNonNull(getMockMvc().perform(delete("/api/v1/file?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        assertEquals("File with id ("+missingId+") not found.", error);
    }

    @Test
    void testActionApi() throws Exception {
        // Need a file for the actions
        FileInfo file = new FileInfo();
        file.setName("Testing.txt");
        fileSystemObjectManager.save(file);

        // Set up some actions.
        ActionConfirmDTO deleteAction = actionManager.createFileDeleteAction(file);
        ActionConfirmDTO importAction = actionManager.createFileImportAction(file,"C");

        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(importAction.getId());
        confirmActionRequest.setParameter("Blah");
        actionManager.confirmAction(confirmActionRequest);

        // Get the database files
        getMockMvc().perform(get("/api/v1/actions")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fileName", is("Testing.txt")))
                .andExpect(jsonPath("$[0].action", is(ActionConfirmType.AC_DELETE.getTypeName())));

        // Get the database files
        getMockMvc().perform(get("/api/v1/actions/confirmed")
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
        getMockMvc().perform(post("/api/v1/actions")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/api/v1/actions/email")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/api/v1/summary")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("valid", is(true)));
    }

    @Test
    void testAssociateFileDataManager() throws Exception {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setName("Test");

        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setId(1);
        sourceDTO.setPath("Test");
        sourceDTO.setLocation(locationDTO);
        sourceDTO.setStatus("OK");

        String error = Objects.requireNonNull(getMockMvc().perform(put("/api/v1/sources")
                        .content(this.json(sourceDTO))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        assertEquals("Source with id (1) not found.", error);

        associatedFileDataManager.findSourceById(this.source.getIdAndType().getId());

        sourceDTO = new SourceDTO();
        sourceDTO.setId(this.source.getIdAndType().getId());
        sourceDTO.setStatus("OK");
        error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/sources")
                        .content(this.json(sourceDTO))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        assertEquals("Source with id (" + sourceDTO.getId() + ") already exists.", error);
    }

    @Test
    void checkActionInvalid() throws Exception {
        ConfirmActionRequest request = new ConfirmActionRequest();
        request.setId(1);
        request.setConfirm(true);
        request.setParameter("Blha");
        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/actions")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        assertEquals("Action 1 not found.", error);
    }

    @Test
    void duplicateTesting() throws Exception {
        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test7");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(3)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(2)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        LOG.info("Check for duplicates in duplicate testing");
        getMockMvc().perform(post("/api/v1/duplicates")
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
        assertEquals(2, count);
        assertNotEquals(-1, actionId);

        // Confirm the action.
        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(actionId);
        confirmActionRequest.setParameter("");
        confirmActionRequest.setConfirm(true);

        getMockMvc().perform(post("/api/v1/actions")
                        .content(this.json(confirmActionRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/api/v1/duplicates")
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
    void duplicateTestingWithMD5() throws Exception {
        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test8");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        getMockMvc().perform(post("/api/v1/gather")
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
        getMockMvc().perform(post("/api/v1/duplicates")
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
        assertEquals(2, count);
        assertNotEquals(-1, actionId);

        // Confirm the action.
        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(actionId);
        confirmActionRequest.setParameter("");
        confirmActionRequest.setConfirm(true);

        getMockMvc().perform(post("/api/v1/actions")
                        .content(this.json(confirmActionRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/api/v1/duplicates")
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
    void syncWithDelete() throws Exception {
        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Perform a gather.
        LOG.info("Gather the data in sync with delete.");
        getMockMvc().perform(post("/api/v1/gather")
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

        LOG.info("Synchronize the data (Sync with delete).");
        getMockMvc().perform(post("/api/v1/sync/run")
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
        assertNotEquals(-1, deleteId);

        getMockMvc().perform(delete("/api/v1/file?id=" + deleteId)
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
        assertEquals(1, count);
        assertNotEquals(-1, actionId);

        // Confirm the action.
        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(actionId);
        confirmActionRequest.setParameter("");
        confirmActionRequest.setConfirm(true);

        getMockMvc().perform(post("/api/v1/actions")
                        .content(this.json(confirmActionRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Process the gather and syncs again.
        LOG.info("Gather the data after action update.");
        getMockMvc().perform(post("/api/v1/gather")
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

        getMockMvc().perform(post("/api/v1/sync/run")
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

        getMockMvc().perform(post("/api/v1/gather")
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
    void testSyncWithFilesRemoved() throws Exception {
        // Check what happens when a synced directory has a file removed
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test9");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Gather the files.
        getMockMvc().perform(post("/api/v1/gather")
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

        LOG.info("Synchronize the data. (sync with files removed.)");
        getMockMvc().perform(post("/api/v1/sync/run")
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

        getMockMvc().perform(post("/api/v1/gather")
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

        File fileToDelete = new File(SOURCE_DIRECTORY + "/Documents/Bills.ods");
        Files.deleteIfExists(fileToDelete.toPath());

        getMockMvc().perform(post("/api/v1/gather")
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

        getMockMvc().perform(post("/api/v1/sync/run")
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
        assertEquals(1, count);
        assertNotEquals(-1, actionId);

        // Confirm the action.
        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(actionId);
        confirmActionRequest.setParameter("");
        confirmActionRequest.setConfirm(true);

        getMockMvc().perform(post("/api/v1/actions")
                        .content(this.json(confirmActionRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/api/v1/sync/run")
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

        getMockMvc().perform(post("/api/v1/gather")
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
    void testSyncWithDirectoryRemoved() throws Exception {
        // Check what happens when a synced directory has a file removed
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test10");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Gather the files.
        getMockMvc().perform(post("/api/v1/gather")
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

        LOG.info("Synchronize the data. (sync with directory removed)");
        getMockMvc().perform(post("/api/v1/sync/run")
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

        getMockMvc().perform(post("/api/v1/gather")
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

        File directoryToDelete = new File(SOURCE_DIRECTORY + "/Documents/sub");
        FileUtils.deleteDirectory(directoryToDelete);

        getMockMvc().perform(post("/api/v1/gather")
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

        getMockMvc().perform(post("/api/v1/sync/run")
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
        assertEquals(1, count);
        assertNotEquals(-1, actionId);

        // Confirm the action.
        ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
        confirmActionRequest.setId(actionId);
        confirmActionRequest.setParameter("");
        confirmActionRequest.setConfirm(true);

        getMockMvc().perform(post("/api/v1/actions")
                        .content(this.json(confirmActionRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/api/v1/sync/run")
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

        getMockMvc().perform(post("/api/v1/gather")
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
    void testSyncFileToDirectory() throws Exception {
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test12_2");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Gather the files.
        getMockMvc().perform(post("/api/v1/gather")
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
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Gather the files.
        getMockMvc().perform(post("/api/v1/gather")
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
    void testSyncDirectoryToFile() throws Exception {
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test12");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Gather the files.
        getMockMvc().perform(post("/api/v1/gather")
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
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Gather the files.
        getMockMvc().perform(post("/api/v1/gather")
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
    void testSyncEqualiseDate() throws Exception {
        // Check what happens when a synced directory has a file removed
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test11");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> destinationDescription = getTestStructure("test11_dest");
        copyFiles(destinationDescription, DESTINATION_DIRECTORY);

        // Gather the files.
        getMockMvc().perform(post("/api/v1/gather")
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
        getMockMvc().perform(post("/api/v1/sync/run")
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
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        getMockMvc().perform(post("/api/v1/gather")
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
                .andExpect(jsonPath("$[1].filesRemoved", is(0)))
                .andExpect(jsonPath("$[1].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[1].deletes", is(0)));

        // Check that destination is unaffected just because the date is modified (size/MD5 are used to detect changes)
        validateSource(fileSystemObjectManager,synchronize.getDestination(),destinationDescription);
    }

    @Test
    void testSyncSourceBusy() throws Exception {
        // Check what happens when a synced directory has a file removed
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Remove the destination
        associatedFileDataManager.updateSourceStatus(this.destination, SourceStatusType.SST_GATHERING);

        // Gather the files.
        getMockMvc().perform(post("/api/v1/gather")
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
    void testSummary() throws Exception {
        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Perform a gather.
        LOG.info("Gather the data for test summary.");
        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(14)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(11)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        Summary.forceInstance(associatedFileDataManager, fileSystemObjectManager, applicationProperties);
        Summary summary = Summary.getInstance(associatedFileDataManager, fileSystemObjectManager, applicationProperties);

        assertTrue(summary.isValid());
        List<SourceDTO> sources = summary.getSources();
        assertEquals(3,sources.size());
        assertEquals(14,sources.get(0).getFileCount());
        assertEquals(11,sources.get(0).getDirectoryCount());
        assertEquals(6622444,sources.get(0).getLargestFile());
        assertEquals(15859756,sources.get(0).getTotalFileSize());
        assertEquals(0,sources.get(1).getFileCount());
        assertEquals(0,sources.get(1).getDirectoryCount());
        assertEquals(0,sources.get(1).getLargestFile());
        assertEquals(0,sources.get(1).getTotalFileSize());
    }

    @Test
    void gatherMountCheck() throws Exception {
        LOG.info("Mount check testing (gather)");

        // During this test create files in the following directories
        initialiseDirectories();
        this.source.setMountCheck("./target/it_test/import/thisfileismissing.txt");
        associatedFileDataManager.updateSource(this.source);

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Perform a gather.
        LOG.info("Gather the data in mount check.");
        getMockMvc().perform(post("/api/v1/gather")
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
        associatedFileDataManager.updateSource(this.source);
    }

    @Test
    void syncMountCheck() throws Exception {
        LOG.info("Mount check testing (Sync mount check)");

        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Perform a gather.
        LOG.info("Gather the data in sync mount check.");
        getMockMvc().perform(post("/api/v1/gather")
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
        associatedFileDataManager.updateSource(this.source);

        // Perform the sync.
        getMockMvc().perform(post("/api/v1/sync/run")
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
        associatedFileDataManager.updateSource(this.source);
    }

    @Test
    void gatherMountCheckPositive() throws Exception {
        LOG.info("Mount check testing (positive)");

        initialiseDirectories();
        File checkMountFile = new File("./target/it_test/import/mountcheck.txt");
        Files.createFile(checkMountFile.toPath());

        // During this test create files in the following directories
        this.source.setMountCheck(checkMountFile.toString());
        associatedFileDataManager.updateSource(this.source);

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Perform a gather.
        LOG.info("Gather the data in mount check positive.");
        getMockMvc().perform(post("/api/v1/gather")
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
        associatedFileDataManager.updateSource(this.source);
    }

    @Test
    void syncMountCheckPositive() throws Exception {
        LOG.info("Mount check testing");

        // During this test create files in the following directories
        initialiseDirectories();
        File checkMountFile = new File("./target/it_test/import/mountcheck.txt");
        Files.createFile(checkMountFile.toPath());

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        // Perform a gather.
        LOG.info("Gather the data mount check test4.");
        getMockMvc().perform(post("/api/v1/gather")
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
        associatedFileDataManager.updateSource(this.source);

        // Perform the sync.
        getMockMvc().perform(post("/api/v1/sync/run")
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
        associatedFileDataManager.updateSource(this.source);
    }

    @Test
    void testRefresh() throws Exception {
        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test18");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> destinationDescription = getTestStructure("test18");
        copyFiles(destinationDescription, DESTINATION_DIRECTORY);

        //gather
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[*].failed", containsInAnyOrder(false,false)))
                .andExpect(jsonPath("$[*].filesInserted",containsInAnyOrder(1,1)))
                .andExpect(jsonPath("$[*].directoriesInserted", containsInAnyOrder(4,4)))
                .andExpect(jsonPath("$[*].filesRemoved", containsInAnyOrder(0,0)))
                .andExpect(jsonPath("$[*].directoriesRemoved", containsInAnyOrder(0,0)))
                .andExpect(jsonPath("$[*].deletes", containsInAnyOrder(0,0)));

        // Get the files.
        List<FileSystemObject> files = new ArrayList<>();
        fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_FILE).forEach(files::add);

        // There should be two files, with no classification.
        assertEquals(2, files.size());
        FileInfo fileInfo = (FileInfo) files.get(0);
        int findId = fileInfo.getIdAndType().getId();
        assertNull(fileInfo.getClassification());
        fileInfo = (FileInfo) files.get(1);
        // Use the lowest id to find, as this will be the first created.
        if(fileInfo.getIdAndType().getId() < findId) {
            findId = fileInfo.getIdAndType().getId();
        }
        assertNull(fileInfo.getClassification());

        // Test the get file.
        getMockMvc().perform(get("/api/v1/files/detail?id=" + findId)
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.file.id", is(findId)))
                .andExpect(jsonPath("$.file.md5", is("56FDC164DC8A27C015170014821A7DCE")))
                .andExpect(jsonPath("$.metaData").value(IsNull.nullValue()))
                .andExpect(jsonPath("$.backups[0].md5", is("56FDC164DC8A27C015170014821A7DCE")));

        // Add the classification.
        Classification jpgxClassification = new Classification();
        jpgxClassification.setIcon("fa-picture-o");
        jpgxClassification.setRegex(".*\\.jpgx$");
        jpgxClassification.setAction(ClassificationActionType.CA_BACKUP);
        jpgxClassification.setIsVideo(false);
        jpgxClassification.setOrder(1);
        jpgxClassification.setIsImage(true);
        jpgxClassification.setCheckMetaData(true);
        associatedFileDataManager.createClassification(jpgxClassification);

        // Perform a refresh
        getMockMvc().perform(post("/api/v1/files/refresh?id="+findId)
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.file.id", is(findId)))
                .andExpect(jsonPath("$.file.md5", is("56FDC164DC8A27C015170014821A7DCE")))
                .andExpect(jsonPath("$.metaData.image", is(true)))
                .andExpect(jsonPath("$.metaData.imageHeight", is(3024)))
                .andExpect(jsonPath("$.metaData.imageWidth", is(4032)))
                .andExpect(jsonPath("$.metaData.date", is("2022-05-20T13:21:59")))
                .andExpect(jsonPath("$.backups[0].md5", is("56FDC164DC8A27C015170014821A7DCE")));

        // Test update and delete of meta data.
        fileInfo = (FileInfo) files.get(0);
        if(fileInfo.getIdAndType().getId() != findId) {
            fileInfo = (FileInfo) files.get(1);
        }
        Optional<MetaData> metaData = fileSystemObjectManager.findMetaDataForFile(fileInfo);

        assertTrue(metaData.isPresent());
        metaData.get().setDuration(10.0);
        fileSystemObjectManager.updateMetaData(metaData.get());

        fileSystemObjectManager.deleteMetaData(metaData.get());

        // Remove the classification
        fileSystemObjectManager.delete(files.get(0));
        fileSystemObjectManager.delete(files.get(1));
        associatedFileDataManager.deleteClassification(jpgxClassification);
    }

    @Test
    void TestPrimarySourceDateChange() throws Exception {
        // Simulate a date change (but no MD5 change) on a primary source - should recalculate the MD5.
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test18");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> destinationDescription = getTestStructure("test18");
        copyFiles(destinationDescription, DESTINATION_DIRECTORY);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5Updates",is(0)))
                .andExpect(jsonPath("$[1].md5Updates",is(0)));

        // Now change the date of the source.
        File file = new File("./target/it_test/source/Photo/2013/October/AtHome/IMG_8231.jpgx");

        if(file.exists()){
            assertTrue(file.setLastModified(System.currentTimeMillis()));
        } else {
            fail();
        }

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5Updates",is(1)))
                .andExpect(jsonPath("$[1].md5Updates",is(0)));

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5Updates",is(0)))
                .andExpect(jsonPath("$[1].md5Updates",is(0)));

        // Updating the date on the destination should not cause an MD5 update.
        file = new File("./target/it_test/destination/Photo/2013/October/AtHome/IMG_8231.jpgx");

        if(file.exists()){
            assertTrue(file.setLastModified(System.currentTimeMillis()));
        } else {
            fail();
        }

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5Updates",is(0)))
                .andExpect(jsonPath("$[1].md5Updates",is(0)));
    }

    private void modify(File file, int changePosition, long length, boolean insert) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.getAbsolutePath(), "rw")) {
            long position = insert ? length - changePosition : changePosition; // index of byte you want to modify (0-based)
            byte newValue = (byte) 0x7F; // new byte value

            // Move the file pointer to the position
            raf.seek(position);

            if(insert){
                // Insert the byte
                byte[] bytes = new byte[(int) changePosition];
                raf.readFully(bytes);

                raf.seek(position);
                raf.write(newValue);
                raf.write(bytes);
            } else {
                // Overwrite the byte
                raf.write(newValue);
            }
        }
    }

    @Test
    void TestPrimarySourceDateAndMD5Change() throws Exception {
        // Simulate a date change and MD5 change on a primary source - should recalculate the MD5.
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test18");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> destinationDescription = getTestStructure("test18");
        copyFiles(destinationDescription, DESTINATION_DIRECTORY);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("56FDC164DC8A27C015170014821A7DCE")))
                .andExpect(jsonPath("$[0].size",is(1102542)))
                .andExpect(jsonPath("$[1].md5",is("56FDC164DC8A27C015170014821A7DCE")))
                .andExpect(jsonPath("$[1].size",is(1102542)));

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5Updates",is(0)))
                .andExpect(jsonPath("$[1].md5Updates",is(0)));

        // Modify a byte within the file.
        modify(new File("./target/it_test/source/Photo/2013/October/AtHome/IMG_8231.jpgx"),5, 1102542, false);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5Updates",is(1)))
                .andExpect(jsonPath("$[1].md5Updates",is(0)));

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("88C38F9673F4D0C27E199499B5B413A0")))
                .andExpect(jsonPath("$[0].size",is(1102542)))
                .andExpect(jsonPath("$[1].md5",is("56FDC164DC8A27C015170014821A7DCE")))
                .andExpect(jsonPath("$[1].size",is(1102542)));

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5Updates",is(0)))
                .andExpect(jsonPath("$[1].md5Updates",is(0)));

        // Updating the date on the destination should not cause an MD5 update.
        modify(new File("./target/it_test/destination/Photo/2013/October/AtHome/IMG_8231.jpgx"),5, 1102542, false);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5Updates",is(0)))
                .andExpect(jsonPath("$[1].md5Updates",is(0)));

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("88C38F9673F4D0C27E199499B5B413A0")))
                .andExpect(jsonPath("$[0].size",is(1102542)))
                .andExpect(jsonPath("$[1].md5",is("56FDC164DC8A27C015170014821A7DCE")))
                .andExpect(jsonPath("$[1].size",is(1102542)));
    }

    @Test
    void TestPrimarySourceSizeChange() throws Exception {
        // Simulate a size change on a primary source - should recalculate the MD5.
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test18");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> destinationDescription = getTestStructure("test18");
        copyFiles(destinationDescription, DESTINATION_DIRECTORY);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("56FDC164DC8A27C015170014821A7DCE")))
                .andExpect(jsonPath("$[0].size",is(1102542)))
                .andExpect(jsonPath("$[1].md5",is("56FDC164DC8A27C015170014821A7DCE")))
                .andExpect(jsonPath("$[1].size",is(1102542)));

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5Updates",is(0)))
                .andExpect(jsonPath("$[1].md5Updates",is(0)));

        // Modify add byte within the file.
        modify(new File("./target/it_test/source/Photo/2013/October/AtHome/IMG_8231.jpgx"),5, 1102542, true);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5Updates",is(1)))
                .andExpect(jsonPath("$[1].md5Updates",is(0)));

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("9CBF6EE6B25038791D91137DB9E2685F")))
                .andExpect(jsonPath("$[0].size",is(1102543)))
                .andExpect(jsonPath("$[1].md5",is("56FDC164DC8A27C015170014821A7DCE")))
                .andExpect(jsonPath("$[1].size",is(1102542)));

        // Modify add byte within the file.
        modify(new File("./target/it_test/destination/Photo/2013/October/AtHome/IMG_8231.jpgx"),5, 1102542, true);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5Updates",is(0)))
                .andExpect(jsonPath("$[1].md5Updates",is(1)));

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("9CBF6EE6B25038791D91137DB9E2685F")))
                .andExpect(jsonPath("$[0].size",is(1102543)))
                .andExpect(jsonPath("$[1].md5",is("9CBF6EE6B25038791D91137DB9E2685F")))
                .andExpect(jsonPath("$[1].size",is(1102543)));
    }

    @Test
    void TestSyncSizeChange() throws Exception {
        // Simulate a size change - should copy the file and regenerate the MD5
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test19");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> destinationDescription = getTestStructure("test19");
        copyFiles(destinationDescription, DESTINATION_DIRECTORY);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[0].size",is(443707)))
                .andExpect(jsonPath("$[1].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[1].size",is(443707)));

        // Modify add byte within the file.
        modify(new File("./target/it_test/source/Photo/2013/October/AtHome/IMG_8231.HEIC"),6, 443707, true);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("E3BF544897461FDF8978CD2312E4C8D3")))
                .andExpect(jsonPath("$[0].size",is(443708)))
                .andExpect(jsonPath("$[1].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[1].size",is(443707)));

        getMockMvc().perform(post("/api/v1/sync/run")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(1)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("E3BF544897461FDF8978CD2312E4C8D3")))
                .andExpect(jsonPath("$[0].size",is(443708)))
                .andExpect(jsonPath("$[1].md5",is("E3BF544897461FDF8978CD2312E4C8D3")))
                .andExpect(jsonPath("$[1].size",is(443708)));

        getMockMvc().perform(post("/api/v1/sync/run")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));
    }

    @Test
    void TestSyncNoSizeButMD5Change() throws Exception {
        // Simulate an MD5 change - should copy the file and regenerate the MD5 on the destination.
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test19");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> destinationDescription = getTestStructure("test19");
        copyFiles(destinationDescription, DESTINATION_DIRECTORY);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[0].size",is(443707)))
                .andExpect(jsonPath("$[1].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[1].size",is(443707)));

        // Modify add byte within the file.
        modify(new File("./target/it_test/source/Photo/2013/October/AtHome/IMG_8231.HEIC"),6, 443707, false);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("1826804865212DDB2BE3498AB914A522")))
                .andExpect(jsonPath("$[0].size",is(443707)))
                .andExpect(jsonPath("$[1].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[1].size",is(443707)));

        getMockMvc().perform(post("/api/v1/sync/run")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(1)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("1826804865212DDB2BE3498AB914A522")))
                .andExpect(jsonPath("$[0].size",is(443707)))
                .andExpect(jsonPath("$[1].md5",is("1826804865212DDB2BE3498AB914A522")))
                .andExpect(jsonPath("$[1].size",is(443707)));

        getMockMvc().perform(post("/api/v1/sync/run")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));
    }

    @Test
    void TestSyncDateChangeNoCopy() throws Exception {
        // Simulate a date change on destination - there should be no file copy.
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test19");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> destinationDescription = getTestStructure("test19");
        copyFiles(destinationDescription, DESTINATION_DIRECTORY);

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[0].size",is(443707)))
                .andExpect(jsonPath("$[1].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[1].size",is(443707)))
                .andExpect(jsonPath("$[1].date",is("2013-10-11T15:23:00")));

        // Now change the date of the destination.
        File file = new File("./target/it_test/destination/Photo/2013/October/AtHome/IMG_8231.HEIC");

        long fileTime = System.currentTimeMillis();
        if(file.exists()){
            assertTrue(file.setLastModified(fileTime));
        } else {
            fail();
        }

        // Turn the file time into a string.
        String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .format(Instant.ofEpochMilli(fileTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime());

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted",is(1)));

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[0].size",is(443707)))
                .andExpect(jsonPath("$[1].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[1].size",is(443707)))
                .andExpect(jsonPath("$[1].date",startsWith(formatted.substring(0,13))));

        getMockMvc().perform(post("/api/v1/sync/run")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(0)))
                .andExpect(jsonPath("$[0].directoriesCopied", is(0)))
                .andExpect(jsonPath("$[0].filesDeleted", is(0)))
                .andExpect(jsonPath("$[0].directoriesDeleted", is(0)))
                .andExpect(jsonPath("$[0].sourcesRemoved", is(0)))
                .andExpect(jsonPath("$[0].datesUpdated", is(0)))
                .andExpect(jsonPath("$[0].filesWarned", is(0)));

        getMockMvc().perform(post("/api/v1/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[1].failed", is(false)))
                .andExpect(jsonPath("$[1].filesInserted",is(0)));

        getMockMvc().perform(get("/api/v1/files")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$[0].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[0].size",is(443707)))
                .andExpect(jsonPath("$[1].md5",is("C56590F0A404CF1778574FD18675B56C")))
                .andExpect(jsonPath("$[1].size",is(443707)))
                .andExpect(jsonPath("$[1].date",startsWith(formatted.substring(0,13))));
    }
}
