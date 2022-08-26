package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.*;
import com.jbr.middletier.backup.manager.*;
import org.junit.*;
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

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {ImportIT.Initializer.class})
@ActiveProfiles(value="it")
public class ImportIT extends FileTester {
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
        if (!existingLocation.isPresent())
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
        preImportSourceDTO.setPath(sourceDirectory);

        this.preImportSource = associatedFileDataManager.createPreImportSource(associatedFileDataManager.convertToEntity(preImportSourceDTO));
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

    @Test
    public void basicImportTest() throws IOException, ImportRequestException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        List<StructureDescription> importDesciption = getTestStructure("test10");
        copyFiles(importDesciption, importDirectory);

        driveManager.gather();
        validateSource(fileSystemObjectManager, this.source, sourceDescription);

        List<GatherDataDTO> result = importManager.importPhoto();
        checkGather(result, 4, 2);

        List<ImportDataDTO> importResult = importManager.processImportFiles();
        checkImport(importResult, 0, 0, 0, 0, 0);

        confirmActions();

        importResult = importManager.processImportFiles();
        checkImport(importResult, 4, 0, 0, 0, 0);

        driveManager.gather();
        sourceDescription = getTestStructure("test13");
        validateSource(fileSystemObjectManager, this.source, sourceDescription);

        importDesciption = getTestStructure("test10");
        copyFiles(importDesciption, importDirectory);

        result = importManager.importPhoto();
        checkGather(result, 0, 0);

        importResult = importManager.processImportFiles();
        checkImport(importResult, 0, 0, 4, 0, 0);
    }

    @Test
    public void gatherTestIgnore() throws IOException, ImportRequestException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        List<StructureDescription> importDesciption = getTestStructure("test14_1");
        copyFiles(importDesciption, importDirectory);

        List<GatherDataDTO> result = importManager.importPhoto();
        checkGather(result, 5, 0);

        List<ImportDataDTO> importResult = importManager.processImportFiles();
        checkImport(importResult, 0, 0, 0, 0, 0);

        confirmActionsIgnoreOrRecipe("IMG_8233.jpg", true);
        confirmActionsIgnoreOrRecipe("IMG_8234.jpg", true);
        confirmActions();

        importResult = importManager.processImportFiles();
        checkImport(importResult, 3, 0, 0, 2, 0);

        importDesciption = getTestStructure("test14_1");
        copyFiles(importDesciption, importDirectory);

        result = importManager.importPhoto();
        checkGather(result, 0, 0);

        importResult = importManager.processImportFiles();
        checkImport(importResult, 0, 2, 0, 0, 0);

        actionManager.clearImportActions();
    }

    @Test
    public void testNonBackup() throws IOException, ImportRequestException {
        List<StructureDescription> sourceDescription = getTestStructure("test7");
        copyFiles(sourceDescription, sourceDirectory);

        List<StructureDescription> importDesciption = getTestStructure("test1");
        copyFiles(importDesciption, importDirectory);

        List<GatherDataDTO> result = importManager.importPhoto();
        checkGather(result, 1, 1);

        List<ImportDataDTO> importResult = importManager.processImportFiles();
        checkImport(importResult, 0, 0, 0, 0, 1);
    }

    @Test
    public void testRecipe() throws IOException, ImportRequestException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        List<StructureDescription> importDesciption = getTestStructure("test14_1");
        copyFiles(importDesciption, importDirectory);

        List<GatherDataDTO> result = importManager.importPhoto();
        checkGather(result, 5, 0);

        List<ImportDataDTO> importResult = importManager.processImportFiles();
        checkImport(importResult, 0, 0, 0, 0, 0);

        confirmActionsIgnoreOrRecipe("IMG_8233.jpg", false);
        confirmActionsIgnoreOrRecipe("IMG_8234.jpg", false);
        confirmActions();

        importResult = importManager.processImportFiles();
        checkImport(importResult, 5, 0, 0, 0, 0);

        sourceDescription = getTestStructure("test14_recipe");
        driveManager.gather();
        validateSource(fileSystemObjectManager, this.source, sourceDescription);

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

        List<GatherDataDTO> result = importManager.importPhoto();
        checkGather(result, 1, 0);

        // Process the import.
        importManager.processImportFiles();

        // Request the id of the file created.
        AtomicInteger id = new AtomicInteger(-1);
        fileSystemObjectManager.findFileSystemObjectByName("IMG_8231.jpeg", FileSystemObjectType.FSO_IMPORT_FILE)
                .forEach(file -> id.set(file.getIdAndType().getId()) );

        ImportFileDTO file = importManager.externalFindImportFile(id.get());
        Assert.assertEquals(id.get(),file.getId().longValue());
        Assert.assertEquals(1,file.getSimilarFiles().size());
    }
}
