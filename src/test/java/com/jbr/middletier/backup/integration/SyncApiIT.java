package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.exception.InvalidSourceIdException;
import com.jbr.middletier.backup.exception.SourceAlreadyExistsException;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
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

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
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

    //TODO refactor to remove the need for file & directory repository etc and use the managers.
    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    ImportSourceRepository importSourceRepository;

    @Autowired
    SynchronizeRepository synchronizeRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    DirectoryRepository directoryRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    IgnoreFileRepository ignoreFileRepository;

    @Autowired
    ImportFileRepository importFileRepository;

    @Autowired
    ClassificationRepository classificationRepository;

    @Autowired
    ActionConfirmRepository actionConfirmRepository;

    @Autowired
    BackupManager backupManager;

    @Autowired
    FileSystemObjectManager fileSystemObjectManager;

    @Autowired
    AssociatedFileDataManager associatedFileDataManager;

    private Source source;
    private Source destination;
    private Synchronize synchronize;

    @Before
    public void setupClassification() throws IOException {
        backupManager.clearMessageCache();

        addClassification(classificationRepository,".*\\._\\.ds_store$", ClassificationActionType.CA_DELETE, 1, false, false, false);
        addClassification(classificationRepository,".*\\.ds_store$", ClassificationActionType.CA_IGNORE, 2, true, false, false);
        addClassification(classificationRepository,".*\\.heic$", ClassificationActionType.CA_BACKUP, 2, false, true, false);
        addClassification(classificationRepository,".*\\.mov$", ClassificationActionType.CA_BACKUP, 2, false, false, true);
        addClassification(classificationRepository,".*\\.mp4$", ClassificationActionType.CA_BACKUP, 2, false, false, true);

        // Update JPG so it gets an MD5
        for(Classification nextClassification : classificationRepository.findAllByOrderByIdAsc()) {
            if(nextClassification.getRegex().contains("jpg")) {
                ClassificationDTO updateClassification = new ClassificationDTO();
                updateClassification.setIcon(nextClassification.getIcon());
                updateClassification.setRegex(nextClassification.getRegex());
                updateClassification.setAction(nextClassification.getAction());
                updateClassification.setVideo(nextClassification.getIsVideo());
                updateClassification.setOrder(1);
                updateClassification.setImage(true);
                updateClassification.setId(nextClassification.getId());
                updateClassification.setUseMD5(true);

                nextClassification.update(updateClassification);
                classificationRepository.save(nextClassification);
            }
        }

        // During this test create files in the following directories
        String sourceDirectory = "./target/it_test/source";
        deleteDirectoryContents(new File(sourceDirectory).toPath());
        Files.createDirectories(new File(sourceDirectory).toPath());

        String destinationDirectory = "./target/it_test/destination";
        deleteDirectoryContents(new File(destinationDirectory).toPath());
        Files.createDirectories(new File(destinationDirectory).toPath());

        // Create the standard sources
        Optional<Location> existingLocation = locationRepository.findById(1);
        if(!existingLocation.isPresent())
            fail();
        Location location = existingLocation.get();
        location.setCheckDuplicates();
        locationRepository.save(location);

        this.source = new Source();
        this.source.setLocation(location);
        this.source.setStatus(SourceStatusType.SST_OK);
        this.source.setPath(sourceDirectory);

        sourceRepository.save(this.source);

        this.destination = new Source();
        this.destination.setLocation(location);
        this.destination.setStatus(SourceStatusType.SST_OK);
        this.destination.setPath(destinationDirectory);

        sourceRepository.save(this.destination);

        // Create the source and synchronise entries
        this.synchronize = new Synchronize();
        synchronize.setId(1);
        synchronize.setSource(this.source);
        synchronize.setDestination(this.destination);

        synchronizeRepository.save(synchronize);
    }

    @After
    public void cleanUpTest() {
        // Remove the sources, files & directories.
        synchronizeRepository.deleteAll();
        actionConfirmRepository.deleteAll();
        fileRepository.deleteAll();
        ignoreFileRepository.deleteAll();
        importFileRepository.deleteAll();

        List<DirectoryInfo> dbDirectories = new ArrayList<>(directoryRepository.findAllByOrderByIdAsc());
        for(DirectoryInfo nextDirectory : dbDirectories) {
            nextDirectory.setParent(null);
            directoryRepository.save(nextDirectory);
        }

        directoryRepository.deleteAll();
        sourceRepository.deleteAll();
        importSourceRepository.deleteAll();
    }

    @Test
    @Order(1)
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
                .andExpect(status().isOk());

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
                .andExpect(status().isOk());

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
                .andExpect(status().isOk());

        validateSource(fileSystemObjectManager, synchronize.getSource(),sourceDescription);

        // Test the get file.
        List<FileInfo> files = new ArrayList<>();
        fileRepository.findAll().forEach(files::add);
        Assert.assertNotEquals(0, files.size());
        getMockMvc().perform(get("/jbr/int/backup/file?id="+files.get(0).getIdAndType().getId())
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

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
                updateClassification.setUseMD5(false);

                nextClassification.update(updateClassification);
                classificationRepository.save(nextClassification);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    @Order(2)
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
                .andExpect(status().isOk());

        validateSource(fileSystemObjectManager,synchronize.getSource(),sourceDescription);

        LOG.info("Synchronize the data.");
        getMockMvc().perform(post("/jbr/int/backup/sync")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesCopied", is(14)));

        // Check that no errors.
        Assert.assertEquals(1, backupManager.getMessageCache(BackupManager.webLogLevel.WARN).size());
        Assert.assertEquals(0, backupManager.getMessageCache(BackupManager.webLogLevel.ERROR).size());

        LOG.info("Gather the data again.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

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
        for(FileInfo nextFile : fileRepository.findAll()) {
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
        String error = getMockMvc().perform(get("/jbr/int/backup/file?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException().getMessage();
        Assert.assertEquals("File with id ("+missingId+") not found.", error);


        getMockMvc().perform(get("/jbr/int/backup/file?id=" + validId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("file.filename", is("Bills.ods")))
                .andExpect(jsonPath("backups[0].filename", is("Bills.ods")));

        // Get the image file.
        error = getMockMvc().perform(get("/jbr/int/backup/fileImage?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException().getMessage();
        Assert.assertEquals("File with id ("+missingId+") not found.", error);

        error = getMockMvc().perform(get("/jbr/int/backup/fileImage?id=" + validId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException().getMessage();
        Assert.assertEquals("File is not of type image", error);

        getMockMvc().perform(get("/jbr/int/backup/fileImage?id=" + imageId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Get the video file.
        error = getMockMvc().perform(get("/jbr/int/backup/fileVideo?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException().getMessage();
        Assert.assertEquals("File with id ("+missingId+") not found.", error);

        error = getMockMvc().perform(get("/jbr/int/backup/fileVideo?id=" + validId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException().getMessage();
        Assert.assertEquals("File is not of type video", error);

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
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].displayName", is("Photo")))
                .andExpect(jsonPath("$[1].displayName", is("Documents")));

        // Request another level
        int directoryId = -1;
        for(DirectoryInfo nextDirectory : directoryRepository.findByParentId(this.source.getIdAndType().getId())) {
            if(nextDirectory.getName().equals("Documents")) {
                directoryId = nextDirectory.getIdAndType().getId();
            }
        }
        hierarchyResponse.setId(directoryId);
        getMockMvc().perform(post("/jbr/int/backup/hierarchy")
                        .content(this.json(hierarchyResponse))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)));

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
    }

    @Test
    @Order(3)
    public void gatherWithDelete() throws Exception {
        LOG.info("Delete with Gather Testing");

        // During this test create files in the following directories
        initialiseDirectories();

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, sourceDirectory);

        // Remove the destination source or this test.
        synchronizeRepository.delete(this.synchronize);
        sourceRepository.delete(this.destination);

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
                .andExpect(jsonPath("$[0].failed", is(false)));

        validateSource(fileSystemObjectManager, this.source,sourceDescription);
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
                .andExpect(jsonPath("$[0].failed", is(false)));
        Assert.assertFalse(Files.exists(new File(sourceDirectory + "/Documents/Text1.txt").toPath()));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    @Order(4)
    public void importTest() throws Exception {
        LOG.info("Delete with Gather Testing");

         // During this test create files in the following directories
        initialiseDirectories();

        List<StructureDescription> sourceDescription = getTestStructure("test6_src");
        copyFiles(sourceDescription, sourceDirectory);

        sourceDescription = getTestStructure("test6");
        copyFiles(sourceDescription, importDirectory);

        // Check that it fails if the request has not been sent
        String error = getMockMvc().perform(post("/jbr/int/backup/importprocess")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException().getMessage();
        Assert.assertEquals("There is no import source defined.", error);

        // Set up a request with invalid path, check exception.
        ImportRequest importRequest = new ImportRequest();
        importRequest.setPath(importDirectory + "x");
        importRequest.setSource(this.source.getIdAndType().getId());

        LOG.info("Gather the data.");
        error = getMockMvc().perform(post("/jbr/int/backup/import")
                        .content(this.json(importRequest))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException().getMessage();
        Assert.assertEquals("The path does not exist - " + importDirectory + "x", error);

        // Insert an ignore file to check it doesn't interfere.
        IgnoreFile ignoreFile = new IgnoreFile();
        ignoreFile.clearRemoved();
        ignoreFile.setDate(new Date());
        ignoreFile.setName("Text.txt");
        ignoreFile.setMD5(new MD5("C714A0B2E792EB102F706DC2424BAA83"));
        ignoreFile.setSize(523);
        ignoreFileRepository.save(ignoreFile);
        ignoreFile = new IgnoreFile();
        ignoreFile.clearRemoved();
        ignoreFile.setDate(new Date());
        ignoreFile.setName("Text.txt");
        ignoreFile.setMD5(new MD5("C714A0B2E792EB102F706DC2424BAA83"));
        ignoreFile.setSize(12);
        ignoreFileRepository.save(ignoreFile);

        // Set up a request with invalid source, check exception.
        importRequest = new ImportRequest();
        int badId = this.source.getIdAndType().getId() + 1;
        if(this.destination.getIdAndType().getId() == badId) {
            badId = this.destination.getIdAndType().getId() + 1;
        }
        importRequest.setPath(importDirectory);
        importRequest.setSource(badId);

        LOG.info("Gather the data.");
        error = getMockMvc().perform(post("/jbr/int/backup/import")
                        .content(this.json(importRequest))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException().getMessage();
        Assert.assertEquals("The source does not exist - " + badId, error);

        // Set up the correct request.
        importRequest = new ImportRequest();
        importRequest.setPath(importDirectory);
        importRequest.setSource(this.source.getIdAndType().getId());

        // Remove the import location temporarily.
        Optional<Location> location = locationRepository.findById(4);
        if(location.isPresent()) {
            location.get().setName("Import x");
            locationRepository.save(location.get());
        }

        LOG.info("Gather the data.");
        error = getMockMvc().perform(post("/jbr/int/backup/import")
                        .content(this.json(importRequest))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException().getMessage();
        Assert.assertEquals("Cannot find import location.", error);

        // Restore the location.
        if(location.isPresent()) {
            location.get().setName("Import");
            locationRepository.save(location.get());
        }

        // Perform the gather - should be 5 files, 1 directory.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/import")
                        .content(this.json(importRequest))
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
        for(ImportSource nextImportSource : importSourceRepository.findAllByOrderByIdAsc()) {
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
                .andExpect(jsonPath("$[0].filesInserted", is(6)));

        // Set up the actions that will be performed.
        for(ActionConfirm nextAction : actionConfirmRepository.findAll()) {
            if(nextAction.getPath().getName().equals("Statement.jpg")) {
                nextAction.setConfirmed(true);
                nextAction.setParameter("Blah");
                actionConfirmRepository.save(nextAction);
            } else if (nextAction.getPath().getName().equals("Letter.jpg")) {
                nextAction.setConfirmed(true);
                nextAction.setParameter("ignore");
                actionConfirmRepository.save(nextAction);
            }
        }

        // Remove any files from the database that have been deleted.
        getMockMvc().perform(delete("/jbr/int/backup/import")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deletes", is(1)))
                .andExpect(jsonPath("$[0].filesInserted", is(6)));

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
                .andExpect(jsonPath("$[0].filesInserted", is(5)));

        // Import should still look the same.
        validateSource(fileSystemObjectManager, importSource, sourceDescription);

        // Gather the data from the source.
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Check that the source has files.
        sourceDescription = getTestStructure("test6_2");
        validateSource(fileSystemObjectManager, this.source, sourceDescription);

        // Reset the import again.
        sourceDescription = getTestStructure("test6");
        copyFiles(sourceDescription, importDirectory);

        LOG.info("Reset the data.");
        getMockMvc().perform(post("/jbr/int/backup/import")
                        .content(this.json(importRequest))
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
                .andExpect(jsonPath("$[0].filesInserted", is(6)));

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
                        .content(this.json(importRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filesInserted", is(0)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(0)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    @Order(5)
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
                .andExpect(status().isOk());

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
        for(FileInfo nextFile : fileRepository.findAll()) {
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

        String error = getMockMvc().perform(delete("/jbr/int/backup/file?id=" + missingId)
                        .content(this.json("testing"))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException().getMessage();
        Assert.assertEquals("File with id ("+missingId+") not found.", error);
    }

    @Test
    @Order(6)
    public void testActionApi() throws Exception {
        // Need a file for the actions
        FileInfo file = new FileInfo();
        file.clearRemoved();
        file.setName("Testing.txt");
        fileRepository.save(file);

        // Setup some actions.
        ActionConfirm actionConfirm1 = new ActionConfirm();
        actionConfirm1.setAction(ActionConfirmType.AC_DELETE);
        actionConfirm1.setConfirmed(false);
        actionConfirm1.setFlags("C");
        actionConfirm1.setParameterRequired(true);
        actionConfirm1.setParameter("Blah");
        actionConfirm1.setFileInfo(file);

        actionConfirmRepository.save(actionConfirm1);

        ActionConfirm actionConfirm2 = new ActionConfirm();
        actionConfirm2.setAction(ActionConfirmType.AC_IMPORT);
        actionConfirm2.setConfirmed(true);
        actionConfirm2.setFlags("F");
        actionConfirm2.setParameterRequired(true);
        actionConfirm2.setParameter("Blah");
        actionConfirm2.setFileInfo(file);

        actionConfirmRepository.save(actionConfirm2);

        // Get the database files
        getMockMvc().perform(get("/jbr/int/backup/actions")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fileName", is("Testing.txt")))
                .andExpect(jsonPath("$[0].action", is(ActionConfirmType.AC_DELETE.toString())));

        // Get the database files
        getMockMvc().perform(get("/jbr/int/backup/confirmed-actions")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fileName", is("Testing.txt")))
                .andExpect(jsonPath("$[0].action", is(ActionConfirmType.AC_IMPORT.toString())));

        ConfirmActionRequest request = new ConfirmActionRequest();
        request.setId(actionConfirm1.getId());
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
    @Order(7)
    public void testAssociateFileDataManager() throws InvalidSourceIdException {
        try {
            associatedFileDataManager.internalFindSourceById(1);
            Assert.fail();
        } catch(InvalidSourceIdException e) {
            Assert.assertEquals("Source with id (1) not found.", e.getMessage());
        }

        associatedFileDataManager.internalFindSourceById(this.source.getIdAndType().getId());

        try {
            SourceDTO sourceDTO = new SourceDTO();
            sourceDTO.setId(10);
            associatedFileDataManager.createSource(sourceDTO);
            Assert.fail();
        } catch(SourceAlreadyExistsException e) {
            Assert.assertEquals("Source with id (10) already exists.", e.getMessage());
        }

    }
}
