package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.*;
import com.jbr.middletier.backup.manager.*;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportManager;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
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
                ClassificationDTO updateClassification = new ClassificationDTO();
                updateClassification.setId(nextClassification.getId());
                updateClassification.setIcon(nextClassification.getIcon());
                updateClassification.setRegex(nextClassification.getRegex());
                updateClassification.setAction(nextClassification.getAction());
                updateClassification.setIsVideo(nextClassification.getIsVideo());
                updateClassification.setOrder(1);
                updateClassification.setIsImage(true);
                updateClassification.setUseMD5(true);

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
        sourceDTO.setPath(sourceDirectory);

        this.source = associatedFileDataManager.createSource(associatedFileDataManager.convertToEntity(sourceDTO));

        ImportSourceDTO importSourceDTO = new ImportSourceDTO();
        importSourceDTO.setLocation(associatedFileDataManager.convertToDTO(existingLocation.get()));
        importSourceDTO.setStatus("OK");
        importSourceDTO.setPath(importDirectory);
        importSourceDTO.setDestinationId(this.source.getIdAndType().getId());

        this.importSource = associatedFileDataManager.createImportSource(associatedFileDataManager.convertToEntity(importSourceDTO));

        PreImportSourceDTO preImportSourceDTO = new PreImportSourceDTO();
        preImportSourceDTO.setLocation(associatedFileDataManager.convertToDTO(existingLocation.get()));
        preImportSourceDTO.setStatus("OK");
        preImportSourceDTO.setPath(preImportDirectory);

        this.preImportSource = associatedFileDataManager.createPreImportSource(associatedFileDataManager.convertToEntity(preImportSourceDTO));

        PostImportSourceDTO postImportSourceDTO = new PostImportSourceDTO();
        postImportSourceDTO.setLocation(associatedFileDataManager.convertToDTO(existingLocation.get()));
        postImportSourceDTO.setStatus("OK");
        postImportSourceDTO.setPath(postImportDirectory);

        this.postImportSource = associatedFileDataManager.createPostImportSource(associatedFileDataManager.convertToEntity(postImportSourceDTO));
    }

    private void checkGather(List<GatherDataDTO> result, int fileInsert, int dirInsert) {
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).hasProblems());
        Assert.assertEquals(0, result.get(0).getCount(GatherDataDTO.GatherDataCountType.DELETES));
        Assert.assertEquals(fileInsert, result.get(0).getCount(GatherDataDTO.GatherDataCountType.FILES_INSERTED));
        Assert.assertEquals(dirInsert, result.get(0).getCount(GatherDataDTO.GatherDataCountType.DIRECTORIES_INSERTED));
        Assert.assertEquals(0, result.get(0).getCount(GatherDataDTO.GatherDataCountType.DIRECTORIES_REMOVED));
        Assert.assertEquals(0, result.get(0).getCount(GatherDataDTO.GatherDataCountType.FILES_REMOVED));
    }

    private void checkImport(List<ImportDataDTO> result, int imported, int ignoredImport, int alreadyImported, int ignored, int nonBackup) {
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(imported, result.get(0).getCount(ImportDataDTO.ImportDataCountType.IMPORTED));
        Assert.assertEquals(ignoredImport, result.get(0).getCount(ImportDataDTO.ImportDataCountType.IGNORED_IMPORTS));
        Assert.assertEquals(alreadyImported, result.get(0).getCount(ImportDataDTO.ImportDataCountType.ALREADY_IMPORTED));
        Assert.assertEquals(ignored, result.get(0).getCount(ImportDataDTO.ImportDataCountType.IGNORED));
        Assert.assertEquals(nonBackup, result.get(0).getCount(ImportDataDTO.ImportDataCountType.NON_BACKUP_CLASSIFICATIONS));
    }

    private void checkPreImport(List<ImportProcessDTO> result, int processed, int alreadyPresent, int imageFiles, int movFiles) {
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(processed, result.get(0).getCount(ImportProcessDTO.ImportProcessCountType.FILES_PROCESSED));
        Assert.assertEquals(alreadyPresent, result.get(0).getCount(ImportProcessDTO.ImportProcessCountType.ALREADY_PRESENT));
        Assert.assertEquals(imageFiles, result.get(0).getCount(ImportProcessDTO.ImportProcessCountType.IMAGE_FILES));
        Assert.assertEquals(movFiles, result.get(0).getCount(ImportProcessDTO.ImportProcessCountType.MOV_FILES));
    }

    private void confirmActions() {
        List<ActionConfirmDTO> actions = actionManager.externalFindByConfirmed(false);
        actions.forEach(action -> {
            ConfirmActionRequest request = new ConfirmActionRequest();
            request.setId(action.getId());
            request.setConfirm(true);
            request.setParameter("TestDir");
            actionManager.confirmAction(request);
        });
    }

    private void confirmActionsIgnoreOrRecipe(String filename, boolean ignore) {
        AtomicInteger id = new AtomicInteger(-1);

        fileSystemObjectManager.findFileSystemObjectByName(filename, FileSystemObjectType.FSO_IMPORT_FILE)
                .forEach(file -> id.set(file.getIdAndType().getId()));

        List<ActionConfirmDTO> actions = actionManager.externalFindByConfirmed(false);
        actions.forEach(action -> {
            if(action.getFileId() == id.get()) {
                ConfirmActionRequest request = new ConfirmActionRequest();
                request.setId(action.getId());
                request.setConfirm(true);
                request.setParameter(ignore ? "ignore" : "<recipe>");
                actionManager.confirmAction(request);
            }
        });

        Assert.assertNotEquals(-1, id.get());
    }

    private void waitForQueue() throws InterruptedException {
        // Wait for a maximum time for the items to be processed.
        LocalDateTime limit = LocalDateTime.now();
        limit = limit.plusSeconds(10);
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
        copyFiles(sourceDescription, sourceDirectory);

        List<StructureDescription> importDescription = getTestStructure("test16_import");
        copyFiles(importDescription, preImportDirectory);

        driveManager.gather();
        validateSource(fileSystemObjectManager, this.source, sourceDescription);

        // trigger the refresh.
//                .andDo(MockMvcResultHandlers.print())
//                .andReturn();
        getMockMvc().perform(get("/jbr/int/backup/import-files?limit=0")
                        .contentType(getContentType()))
                .andExpect(status().isOk());
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
    public void gatherTestIgnore() throws IOException, ImportRequestException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        List<StructureDescription> importDesciption = getTestStructure("test14_1");
        copyFiles(importDesciption, importDirectory);

//        List<GatherDataDTO> result = importManager.importPhoto();
//        checkGather(result, 5, 0);

//        List<ImportDataDTO> importResult = importManager.processImportFiles();
//        checkImport(importResult, 0, 0, 0, 0, 0);

//        confirmActionsIgnoreOrRecipe("IMG_8233.jpg", true);
//        confirmActionsIgnoreOrRecipe("IMG_8234.jpg", true);
//        confirmActions();

//        importResult = importManager.processImportFiles();
//        checkImport(importResult, 3, 0, 0, 2, 0);

//        importDesciption = getTestStructure("test14_1");
//        copyFiles(importDesciption, importDirectory);

//        result = importManager.importPhoto();
//        checkGather(result, 0, 0);

//        importResult = importManager.processImportFiles();
//        checkImport(importResult, 0, 2, 0, 0, 0);

        actionManager.clearImportActions();
    }

    @Test
    public void testNonBackup() throws IOException, ImportRequestException {
        List<StructureDescription> sourceDescription = getTestStructure("test7");
        copyFiles(sourceDescription, sourceDirectory);

        List<StructureDescription> importDesciption = getTestStructure("test1");
        copyFiles(importDesciption, importDirectory);

//        List<GatherDataDTO> result = importManager.importPhoto();
//        checkGather(result, 1, 1);

//        List<ImportDataDTO> importResult = importManager.processImportFiles();
//        checkImport(importResult, 0, 0, 0, 0, 1);
    }

    @Test
    public void testRecipe() throws IOException, ImportRequestException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        List<StructureDescription> importDesciption = getTestStructure("test14_1");
        copyFiles(importDesciption, importDirectory);

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
    public void testSimilarFile() throws IOException, ImportRequestException, InvalidFileIdException {
        List<StructureDescription> sourceDescription = getTestStructure("test16");
        copyFiles(sourceDescription, sourceDirectory);

        List<StructureDescription> importDesciption = getTestStructure("test16_import");
        copyFiles(importDesciption, importDirectory);

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
    public void testHeicFile() throws IOException, ImportRequestException {
        List<StructureDescription> sourceDescription = getTestStructure("test17");
        copyFiles(sourceDescription, sourceDirectory);

        List<StructureDescription> importDescription = getTestStructure("test17_import");
        copyFiles(importDescription, preImportDirectory);

        // Import the source data
        driveManager.gather();

//        List<ImportProcessDTO> convertData = importManager.convertImportFiles();
//        checkPreImport(convertData,1,0,1,0);

//        List<GatherDataDTO> result = importManager.importPhoto();
//        checkGather(result, 1, 0);
    }
}
