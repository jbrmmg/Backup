package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.IgnoreFileRepository;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.*;
import com.jbr.middletier.backup.manager.*;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportManager;
import org.jetbrains.annotations.NotNull;
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.testcontainers.containers.MySQLContainer;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {ImportIT.Initializer.class})
@ActiveProfiles(value="it-import")
public class ImportIT extends FileTester {
    private static final Logger LOG = LoggerFactory.getLogger(ImportIT.class);

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
    IgnoreFileRepository ignoreFileRepository;

    @Autowired
    ImportManager importManager;

    @Autowired
    DriveManager driveManager;

    @Autowired
    ActionManager actionManager;

    Source source;
    ImportSource importSource;
    PreImportSource preImportSource;
    PostImportSource postImportSource;

    @Before
    public void initialise() throws IOException, InvalidClassificationIdException, InvalidLocationIdException, SourceAlreadyExistsException {
        initialiseDirectories();

        // Update JPG so it gets an MD5
        for (Classification nextClassification : associatedFileDataManager.findAllClassifications()) {
            if (nextClassification.getRegex().contains("jpg")) {
                ClassificationDTO updateClassification = getClassificationDTO(nextClassification);

                associatedFileDataManager.updateClassification(associatedFileDataManager.convertToEntity(updateClassification));
            }
        }

        fileSystemObjectManager.deleteAllFileObjects();
        associatedFileDataManager.deleteAllSource();
        associatedFileDataManager.deleteAllImportSource();

        Optional<Location> existingLocation = associatedFileDataManager.findLocationById(1);
        if (existingLocation.isEmpty())
            fail();

        LocationDTO location = associatedFileDataManager.convertToDTO(existingLocation.get());
        location.setCheckDuplicates(true);
        associatedFileDataManager.updateLocation(associatedFileDataManager.convertToEntity(location));

        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setLocation(associatedFileDataManager.convertToDTO(existingLocation.get()));
        sourceDTO.setStatus("OK");
        sourceDTO.setPath(SOURCE_DIRECTORY);

        this.source = associatedFileDataManager.createSource(associatedFileDataManager.convertToEntity(sourceDTO));

        ImportSourceDTO importSourceDTO = new ImportSourceDTO();
        importSourceDTO.setLocation(associatedFileDataManager.convertToDTO(existingLocation.get()));
        importSourceDTO.setStatus("OK");
        importSourceDTO.setPath(IMPORT_DIRECTORY);
        importSourceDTO.setDestinationId(this.source.getIdAndType().getId());

        this.importSource = associatedFileDataManager.createImportSource(associatedFileDataManager.convertToEntity(importSourceDTO));

        PreImportSourceDTO preImportSourceDTO = new PreImportSourceDTO();
        preImportSourceDTO.setLocation(associatedFileDataManager.convertToDTO(existingLocation.get()));
        preImportSourceDTO.setStatus("OK");
        preImportSourceDTO.setPath(PRE_IMPORT_DIRECTORY);

        this.preImportSource = associatedFileDataManager.createPreImportSource(associatedFileDataManager.convertToEntity(preImportSourceDTO));

        PostImportSourceDTO postImportSourceDTO = new PostImportSourceDTO();
        postImportSourceDTO.setLocation(associatedFileDataManager.convertToDTO(existingLocation.get()));
        postImportSourceDTO.setStatus("OK");
        postImportSourceDTO.setPath(POST_IMPORT_DIRECTORY);

        this.postImportSource = associatedFileDataManager.createPostImportSource(associatedFileDataManager.convertToEntity(postImportSourceDTO));
    }

    private static @NotNull ClassificationDTO getClassificationDTO(Classification nextClassification) {
        ClassificationDTO updateClassification = new ClassificationDTO();
        updateClassification.setId(nextClassification.getId());
        updateClassification.setIcon(nextClassification.getIcon());
        updateClassification.setRegex(nextClassification.getRegex());
        updateClassification.setAction(nextClassification.getAction());
        updateClassification.setIsVideo(nextClassification.getIsVideo());
        updateClassification.setOrder(1);
        updateClassification.setIsImage(true);
        updateClassification.setUseMD5(true);
        return updateClassification;
    }

    private void waitForQueue() throws InterruptedException {
        // Wait for a maximum time for the items to be processed.
        LocalDateTime limit = LocalDateTime.now();
        limit = limit.plusSeconds(120);
        LOG.info("Waiting for the queue to complete.");

        boolean done = false;
        while (!done) {
            done = true;
            ImportFileSummaryDTO summary = this.importManager.getImportSummary();
            if(summary.getQueued() > 0) {
                done = false;
            } else {
                Map<FileProcessingStepType, ImportFileSummaryStepDTO> counts = summary.getCounts();

                for (Map.Entry<FileProcessingStepType, ImportFileSummaryStepDTO> entry : counts.entrySet()) {
                    if (entry.getValue().getCount(TrafficLightType.TL_UNKNOWN) > 0) {
                        done = false;
                    }
                }
            }

            if(!done) {
                if(limit.isBefore(LocalDateTime.now())) {
                    throw new IllegalStateException("It has taken too long for the import processing to finish.");
                }
            }
            LOG.info("Waiting for the queue to complete.");
            Thread.sleep(300);
        }
    }

    @Test
    public void basicImportTest() throws Exception {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> importDescription = getTestStructure("test16_import");
        copyFiles(importDescription, PRE_IMPORT_DIRECTORY);

        driveManager.gather();
        validateSource(fileSystemObjectManager, this.source, sourceDescription);

        // trigger the refresh.
        getMockMvc().perform(get("/jbr/int/backup/import-files?limit=0")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        waitForQueue();

        // Check that file processed OK.
        getMockMvc().perform(get("/jbr/int/backup/import-files?limit=0")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filename", is("IMG_8231.jpeg")))
                .andExpect(jsonPath("$[0].date", is("2013-10-11T15:23:00")))
                .andExpect(jsonPath("$[0].size", is(1102542)))
                .andExpect(jsonPath("$[0].md5",is("56FDC164DC8A27C015170014821A7DCE")))
                .andExpect(jsonPath("$[0].importName", is("IMG_8231.jpeg")))
                .andExpect(jsonPath("$[0].importDate", is("2022-05-20T13:21:59")))
                .andExpect(jsonPath("$[0].importSize", is(1102542)))
                .andExpect(jsonPath("$[0].importMd5",is("56FDC164DC8A27C015170014821A7DCE")))
                .andExpect(jsonPath("$[0].location.lat", closeTo(37.224, 0.001)))
                .andExpect(jsonPath("$[0].location.long",closeTo(BigDecimal.valueOf(-115.815),BigDecimal.valueOf(0.001))))
                .andExpect(jsonPath("$[0].image", is(true)))
                .andExpect(jsonPath("$[0].video", is(false)))
                .andExpect(jsonPath("$[0].errorInPostImport", is(false)))
                .andExpect(jsonPath("$[0].errorInImport", is(false)))
                .andExpect(jsonPath("$[0].inPostImport", is(false)))
                .andExpect(jsonPath("$[0].inImport", is(true)))
                .andExpect(jsonPath("$[0].stepStatus.readPreImportFile", is("GREEN")))
                .andExpect(jsonPath("$[0].stepStatus.gatherMetaData", is("GREEN")))
                .andExpect(jsonPath("$[0].stepStatus.copyFileToImport", is("GREEN")))
                .andExpect(jsonPath("$[0].stepStatus.checkFileIgnored", is("GREEN")))
                .andExpect(jsonPath("$[0].stepStatus.checkActivePhotoFile", is("GREEN")))
                .andExpect(jsonPath("$[0].stepStatus.checkDuplicateFile", is("GREEN")))
                .andExpect(jsonPath("$[0].stepStatus.checkFileConfirmedImported", is("RED")))
                .andExpect(jsonPath("$[0].stepStatus.processImport", is("GREEN")))
                .andExpect(jsonPath("$[0].stepStatus.completed", is("GREEN")))
                .andExpect(jsonPath("$[0].status", is("READ")));

        // Check that the summary works.
        getMockMvc().perform(get("/jbr/int/backup/import-files-summary")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("PreImport", is(1)));

        // Cleanup.
        importManager.clearImportData();
    }

    @Test
    public void testIgnore() throws Exception {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> importDescription = getTestStructure("test14_1");
        copyFiles(importDescription, PRE_IMPORT_DIRECTORY);

        driveManager.gather();
        validateSource(fileSystemObjectManager, this.source, sourceDescription);

        // trigger the refresh.
        getMockMvc().perform(get("/jbr/int/backup/import-files?limit=0")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        waitForQueue();

        // Mark one of the files as to be ignored.
        getMockMvc().perform(post("/jbr/int/backup/ignore-file")
                        .content("IMG_8234.jpg")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        // Wait for any queued changes to complete.
        waitForQueue();

        // Check there are 5 files.
        getMockMvc().perform(get("/jbr/int/backup/import-files?limit=0")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].filename").value(containsInAnyOrder("IMG_8231.jpg","IMG_8232.jpg","IMG_8233.jpg","IMG_8234.jpg","IMG_8235.jpg")));

        // Remove ignored files.
        getMockMvc().perform(delete("/jbr/int/backup/delete-ignored")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        waitForQueue();

        // The ignored file should be removed.
        getMockMvc().perform(get("/jbr/int/backup/import-files?limit=0")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].filename").value(containsInAnyOrder("IMG_8231.jpg","IMG_8232.jpg","IMG_8233.jpg","IMG_8235.jpg")));

        // Clear the ignored files.
        ignoreFileRepository.deleteAll();

        // Mark one of the files as to be ignored.
        getMockMvc().perform(post("/jbr/int/backup/ignore-file")
                        .content("IMG_8232.jpg")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        waitForQueue();

        // Mark one of the files as to be ignored.
        getMockMvc().perform(post("/jbr/int/backup/un-ignore-file")
                        .content("IMG_8232.jpg")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        waitForQueue();

        // Ask to remove any ignored.
        getMockMvc().perform(delete("/jbr/int/backup/delete-ignored")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        waitForQueue();

        // Nothing should have been removed.
        getMockMvc().perform(get("/jbr/int/backup/import-files?limit=0")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].filename").value(containsInAnyOrder("IMG_8231.jpg","IMG_8232.jpg","IMG_8233.jpg","IMG_8235.jpg")));

        // Cleanup.
        importManager.clearImportData();
    }

    @Test
    public void testNonBackup() throws IOException {
        List<StructureDescription> sourceDescription = getTestStructure("test7");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> importDescription = getTestStructure("test1");
        copyFiles(importDescription, IMPORT_DIRECTORY);

//        List<GatherDataDTO> result = importManager.importPhoto();
//        checkGather(result, 1, 1);

//        List<ImportDataDTO> importResult = importManager.processImportFiles();
//        checkImport(importResult, 0, 0, 0, 0, 1);
    }

    @Test
    public void testRecipe() throws IOException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> importDescription = getTestStructure("test14_1");
        copyFiles(importDescription, IMPORT_DIRECTORY);

//        List<GatherDataDTO> result = importManager.importPhoto();
//        checkGather(result, 5, 0);

//        List<ImportDataDTO> importResult = importManager.processImportFiles();
//        checkImport(importResult, 0, 0, 0, 0, 0);

//        confirmActionsIgnoreOrRecipe("IMG_8233.jpg", false);
//        confirmActionsIgnoreOrRecipe("IMG_8234.jpg", false);
//        confirmActions();

//        importResult = importManager.processImportFiles();
//        checkImport(importResult, 5, 0, 0, 0, 0);

//        sourceDescription = getTestStructure("test14_recipe");
//        driveManager.gather();
//        validateSource(fileSystemObjectManager, this.source, sourceDescription);

        actionManager.clearImportActions();
    }

    @Test
    public void testSimilarFile() throws IOException {
        List<StructureDescription> sourceDescription = getTestStructure("test16");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> importDescription = getTestStructure("test16_import");
        copyFiles(importDescription, IMPORT_DIRECTORY);

        // Import the source data.
        driveManager.gather();

//        List<GatherDataDTO> result = importManager.importPhoto();
//        checkGather(result, 1, 0);

        // Process the import.
//        importManager.processImportFiles();

        // Request the id of the file created.
        AtomicInteger id = new AtomicInteger(-1);
        fileSystemObjectManager.findFileSystemObjectByName("IMG_8231.jpeg", FileSystemObjectType.FSO_IMPORT_FILE)
                .forEach(file -> id.set(file.getIdAndType().getId()) );

//        ImportFileDTO file = importManager.externalFindImportFile(id.get());
//        Assert.assertEquals(id.get(),file.getId().longValue());
//        Assert.assertEquals(1,file.getSimilarFiles().size());
    }

    @Test
    public void testHeicFile() throws IOException {
        List<StructureDescription> sourceDescription = getTestStructure("test17");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> importDescription = getTestStructure("test17_import");
        copyFiles(importDescription, PRE_IMPORT_DIRECTORY);

        // Import the source data
        driveManager.gather();

//        List<ImportProcessDTO> convertData = importManager.convertImportFiles();
//        checkPreImport(convertData,1,0,1,0);

//        List<GatherDataDTO> result = importManager.importPhoto();
//        checkGather(result, 1, 0);
    }
}
