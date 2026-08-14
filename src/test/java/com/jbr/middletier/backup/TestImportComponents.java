package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.ImportSource;
import com.jbr.middletier.backup.data.MD5;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.integration.FileTester;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.FileSystemImageData;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import com.jbr.middletier.backup.manager.importing.step.CopyToImport;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = MiddleTier.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestImportComponents extends FileTester {
    private static final Logger LOG = LoggerFactory.getLogger(TestImportComponents.class);

    @Test
    void testDeleteStep() throws ImportProcessException {
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
    void testImportFileStep() throws IOException, ImportProcessException {
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
    void testReadStep() throws ImportProcessException {
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

    @Test
    void testCopyToImportMovFile() throws Exception {
        initialiseDirectories();

        File sourceFile = new File(PRE_IMPORT_DIRECTORY, "TEST.mov");
        Files.createFile(sourceFile.toPath());
        File destFile = new File(IMPORT_DIRECTORY, "TEST.mp4");

        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        when(importFileRepository.findByName(any())).thenReturn(Collections.emptyList());

        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);
        when(importSourceManager.getPreImportDirectory()).thenReturn(new File(PRE_IMPORT_DIRECTORY));
        when(importSourceManager.getImportDirectory()).thenReturn(new File(IMPORT_DIRECTORY));

        FileSystem fileSystem = mock(FileSystem.class);
        doAnswer(inv -> { Files.createFile(destFile.toPath()); return null; })
                .when(fileSystem).copyConvertMov(any(), any(), any(), any(), any(), any());
        when(fileSystem.getFileMD5(any(), any())).thenReturn(Optional.of(new MD5("AABBCCDDEEFF00112233445566778899")));

        CopyToImport copyToImport = new CopyToImport(importFileRepository, importSourceManager, fileSystem);

        LocalDateTime importDate = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        PreImportFileDTO file = new PreImportFileDTO();
        file.setFilename("TEST.mov");
        file.setMd5(new MD5("8D4F46976377897DFADF214D0526CF56"));
        file.setSize(12345L);
        file.setImportDate(importDate);
        file.setStepStatus(FileProcessingStepType.FPS_GATHER_META_DATA, TrafficLightType.TL_GREEN);

        TrafficLightType result = copyToImport.performStep(file);

        assertEquals(TrafficLightType.TL_GREEN, result);
        assertEquals("TEST.mp4", file.getImportName());
        assertTrue(file.isInImport());

        verify(fileSystem).copyConvertMov(
                eq(new File(PRE_IMPORT_DIRECTORY, "TEST.mov")),
                eq(new File(IMPORT_DIRECTORY, "TEST.mp4")),
                eq("TEST.mov"),
                eq("8D4F46976377897DFADF214D0526CF56"),
                eq(12345L),
                eq(importDate)
        );
    }

    @Test
    void testCopyConvertMovWithRealFfmpeg() throws Exception {
        File outputDir = new File("target/test-convert");
        Files.createDirectories(outputDir.toPath());

        File sourceFile = new File("src/test/resources/synchronise/IMG_1015.MOV");
        File destFile = new File(outputDir, "IMG_1015_converted.mp4");
        Files.deleteIfExists(destFile.toPath());

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        when(applicationProperties.getFfmpegCommand())
                .thenReturn("ffmpeg -i %%INPUT%% -movflags use_metadata_tags -qscale 0 %%OUTPUT%%");
        FileSystem fileSystem = new FileSystem(applicationProperties);

        String originalMd5 = "8D4F46976377897DFADF214D0526CF56";
        long originalSize = sourceFile.length();
        LocalDateTime originalDate = LocalDateTime.of(2025, 3, 2, 19, 25, 54);

        fileSystem.copyConvertMov(sourceFile, destFile,
                "IMG_1015.MOV", originalMd5, originalSize, originalDate);

        assertTrue(destFile.exists(), "Converted MP4 should exist");

        // Verify the date was set correctly by writeExifDate (reads DateTimeOriginal)
        Optional<FileSystemImageData> metadata = fileSystem.readImageMetaData(destFile);
        assertTrue(metadata.isPresent(), "MP4 should have readable metadata");
        assertTrue(metadata.get().isVideo(), "Output should be a video");
        assertEquals(originalDate, metadata.get().getDateTime(), "Date should match original MOV date");

        // Verify description and custom fields are present in the metadata
        Process exif = new ProcessBuilder("exiftool", destFile.getPath()).start();
        String exifOutput = new String(exif.getInputStream().readAllBytes()).toLowerCase();
        exif.waitFor();

        assertTrue(exifOutput.contains("converted from img_1015.mov"), "Description should reference original filename");
        assertTrue(exifOutput.contains(originalMd5.toLowerCase()), "Original MD5 should be embedded in metadata");
        assertTrue(exifOutput.contains(String.valueOf(originalSize)), "Original size should be embedded in metadata");
    }
}
