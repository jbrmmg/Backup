package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.ImportSource;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.integration.FileTester;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import com.jbr.middletier.backup.manager.importing.step.process.Delete;
import com.jbr.middletier.backup.manager.importing.step.process.ImportFile;
import com.jbr.middletier.backup.manager.importing.step.process.ImportProcessException;
import com.jbr.middletier.backup.manager.importing.step.process.Read;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = MiddleTier.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestImportComponents extends FileTester {
    private static final Logger LOG = LoggerFactory.getLogger(TestImportComponents.class);

    @Test
    public void testDeleteStep() throws ImportProcessException {
        LOG.info("Delete step");

        ImportSourceManager mockImportSourceManager = mock(ImportSourceManager.class);
        when(mockImportSourceManager.getImportDirectory()).thenReturn(new File("."));
        when(mockImportSourceManager.getPostImportDirectory()).thenReturn(new File("."));
        when(mockImportSourceManager.getPreImportDirectory()).thenReturn(new File("."));

        PreImportFileDTO file = new PreImportFileDTO();
        file.setFilename("TestFile.txt");
        file.setImportName("TestFile.txt");
        file.setStepStatus(FileProcessingStepType.FPS_READ_PREIMPORT_FILE,TrafficLightType.TL_GREEN);
        file.setStepStatus(FileProcessingStepType.FPS_GATHER_META_DATA,TrafficLightType.TL_UNKNOWN);

        Delete delete = new Delete(mockImportSourceManager);

        try {
            delete.process(file);
            fail("Should have thrown an exception");
        } catch (ImportProcessException e) {
            assertEquals("TestFile.txt invalid step FPS_GATHER_META_DATA status TL_UNKNOWN", e.getMessage());
        }

        file.setStepStatus(FileProcessingStepType.FPS_GATHER_META_DATA,TrafficLightType.TL_GREEN);
        TrafficLightType result = delete.process(file);
        assertEquals(TrafficLightType.TL_GREEN,result);
        assertEquals("REMOVED",file.getStatus());
    }

    @Test
    public void testImportFileStep() throws IOException, ImportProcessException {
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, SOURCE_DIRECTORY);

        List<StructureDescription> importDescription = getTestStructure("test16_import");
        copyFiles(importDescription, PRE_IMPORT_DIRECTORY);

        ImportSourceManager mockImportSourceManager = mock(ImportSourceManager.class);
        when(mockImportSourceManager.getImportDirectory()).thenReturn(new File(IMPORT_DIRECTORY));
        when(mockImportSourceManager.getPostImportDirectory()).thenReturn(new File(POST_IMPORT_DIRECTORY));
        when(mockImportSourceManager.getPreImportDirectory()).thenReturn(new File(PRE_IMPORT_DIRECTORY));

        AssociatedFileDataManager mockAssociatedFileDataManager = mock(AssociatedFileDataManager.class);

        FileSystem mockFileSystem = mock(FileSystem.class);

        PreImportFileDTO file = new PreImportFileDTO();
        file.setFilename("IMG_8231.jpeg");
        file.setImportName("IMG_8231.jpeg");
        file.setStepStatus(FileProcessingStepType.FPS_READ_PREIMPORT_FILE,TrafficLightType.TL_GREEN);
        file.setStepStatus(FileProcessingStepType.FPS_GATHER_META_DATA,TrafficLightType.TL_GREEN);
        file.setStepStatus(FileProcessingStepType.FPS_CHECK_FILE_CONFIRMED_IMPORTED, TrafficLightType.TL_GREEN);
        file.setStepStatus(FileProcessingStepType.FPS_CHECK_FILE_IGNORED, TrafficLightType.TL_GREEN);

        ImportFile importFile = new ImportFile(mockImportSourceManager, mockAssociatedFileDataManager, mockFileSystem);

        try {
            importFile.process(file);
            fail("Should have thrown an exception");
        } catch (ImportProcessException e) {
            assertEquals("IMG_8231.jpeg invalid step FPS_CHECK_FILE_CONFIRMED_IMPORTED status TL_GREEN", e.getMessage());
        }

        file.setStepStatus(FileProcessingStepType.FPS_CHECK_FILE_CONFIRMED_IMPORTED,TrafficLightType.TL_RED);
        try {
            importFile.process(file);
            fail("Should have thrown an exception");
        } catch(ImportProcessException e) {
            assertEquals("Destination of IMG_8231.jpeg is empty", e.getMessage());
        }

        file.setDestination("Directory");
        file.setInPostImport(true);
        try {
            importFile.process(file);
            fail("Should have thrown an exception");
        } catch(ImportProcessException e) {
            assertEquals("IMG_8231.jpeg cannot be in the post import directory.", e.getMessage());
        }

        File source = new File(PRE_IMPORT_DIRECTORY,"IMG_8231.jpeg");
        File destination = new File(IMPORT_DIRECTORY,"IMG_8231.jpeg");
        Files.copy(source.toPath(),destination.toPath());

        file.setInPostImport(false);
        try {
            importFile.process(file);
            fail("Should have thrown an exception");
        } catch(ImportProcessException e) {
            assertEquals("Import source is invalid.", e.getMessage());
        }

        List<ImportSource> importSources = new ArrayList<>();
        ImportSource importSource = new ImportSource();
        importSource.setPath(IMPORT_DIRECTORY);
        importSource.setId(1);
        Source importDestination = new Source();
        importDestination.setPath(SOURCE_DIRECTORY);
        importDestination.setId(1);
        importSource.setDestination(importDestination);
        importSources.add(importSource);
        when(mockAssociatedFileDataManager.findAllImportSource()).thenReturn(importSources);
        when(mockAssociatedFileDataManager.findSourceIfExists(any())).thenReturn(Optional.empty());

        try {
            importFile.process(file);
            fail("Should have thrown an exception");
        } catch (ImportProcessException e) {
            assertEquals("Import destination is invalid.", e.getMessage());
        }

        file.setImportDate(LocalDateTime.of(2022, 5, 22, 14, 23, 21));
        when(mockAssociatedFileDataManager.findSourceIfExists(any())).thenReturn(Optional.of(importSource));
        TrafficLightType result = importFile.process(file);

        assertEquals(TrafficLightType.TL_GREEN,result);
    }

    @Test
    public void testReadStep() throws ImportProcessException {
        ImportSourceManager mockImportSourceManager = mock(ImportSourceManager.class);

        PreImportFileDTO file = new PreImportFileDTO();
        file.setFilename("Testing.txt");
        file.setStepStatus(FileProcessingStepType.FPS_READ_PREIMPORT_FILE,TrafficLightType.TL_RED);

        Read readStep = new Read(mockImportSourceManager);
        try {
            readStep.process(file);
            fail("Should have thrown an exception");
        } catch(ImportProcessException e) {
            assertEquals("Testing.txt invalid step FPS_READ_PREIMPORT_FILE status TL_RED", e.getMessage());
        }

        file.setStepStatus(FileProcessingStepType.FPS_READ_PREIMPORT_FILE,TrafficLightType.TL_GREEN);
        TrafficLightType result = readStep.process(file);

        assertEquals(TrafficLightType.TL_GREEN,result);
    }
}
