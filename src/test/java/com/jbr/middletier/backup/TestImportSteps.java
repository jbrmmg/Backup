package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.CustomMetaDataRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.IgnoreFileRepository;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.ImportFileBaseDTO;
import com.jbr.middletier.backup.dto.ImportFileDTO;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import com.jbr.middletier.backup.manager.importing.step.CheckActivePhotoFile;
import com.jbr.middletier.backup.manager.importing.step.CheckDuplicateFile;
import com.jbr.middletier.backup.manager.importing.step.CheckFileConfirmedImported;
import com.jbr.middletier.backup.manager.importing.step.CheckFileIgnored;
import com.jbr.middletier.backup.manager.importing.step.process.ImportFile;
import com.jbr.middletier.backup.manager.importing.step.process.ImportProcessException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.jbr.middletier.backup.manager.importing.FileProcessingStepType.FPS_CHECK_FILE_CONFIRMED_IMPORTED;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = MiddleTier.class)
class TestImportSteps {
    @Test
    void TestStepProcessImportFileInvalidState() {
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        PreImportFileDTO preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.getStepStatus(any())).thenReturn(TrafficLightType.TL_GREEN);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.txt");

        ImportFile importFile = new  ImportFile(importSourceManager,associatedFileDataManager,fileSystem);

        // Check invalid step
        try {
            importFile.process(preImportFileDTO);
            fail();
        } catch (ImportProcessException ex) {
            assertEquals("TEST_FILE.txt invalid step FPS_CHECK_FILE_CONFIRMED_IMPORTED status TL_GREEN", ex.getMessage());
        }
    }

    @Test
    void TestStepProcessImportFileDestinationEmpty1() {
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        PreImportFileDTO preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.txt");
        doAnswer(invocation -> {
            FileProcessingStepType arg = invocation.getArgument(0);
            return FPS_CHECK_FILE_CONFIRMED_IMPORTED.equals(arg) ? TrafficLightType.TL_UNKNOWN : TrafficLightType.TL_GREEN;
        }).when(preImportFileDTO).getStepStatus(any(FileProcessingStepType.class));

        ImportFile importFile = new  ImportFile(importSourceManager,associatedFileDataManager,fileSystem);

        // Check invalid step
        try {
            importFile.process(preImportFileDTO);
            fail();
        } catch (ImportProcessException ex) {
            assertEquals("Destination of TEST_FILE.txt is empty", ex.getMessage());
        }
    }

    @Test
    void TestStepProcessImportFileDestinationEmpty2() {
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        PreImportFileDTO preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.txt");
        when(preImportFileDTO.getDestination()).thenReturn("");
        doAnswer(invocation -> {
            FileProcessingStepType arg = invocation.getArgument(0);
            return FPS_CHECK_FILE_CONFIRMED_IMPORTED.equals(arg) ? TrafficLightType.TL_UNKNOWN : TrafficLightType.TL_GREEN;
        }).when(preImportFileDTO).getStepStatus(any(FileProcessingStepType.class));

        ImportFile importFile = new  ImportFile(importSourceManager,associatedFileDataManager,fileSystem);

        // Check invalid step
        try {
            importFile.process(preImportFileDTO);
            fail();
        } catch (ImportProcessException ex) {
            assertEquals("Destination of TEST_FILE.txt is empty", ex.getMessage());
        }
    }

    @Test
    void TestStepProcessImportFileNotExist() {
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);
        when(importSourceManager.getImportDirectory()).thenReturn(new File("./target/"));

        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        PreImportFileDTO preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.txt");
        when(preImportFileDTO.getDestination()).thenReturn("Invalid");
        when(preImportFileDTO.getImportName()).thenReturn("TEST_FILE.txt");
        doAnswer(invocation -> {
            FileProcessingStepType arg = invocation.getArgument(0);
            return FPS_CHECK_FILE_CONFIRMED_IMPORTED.equals(arg) ? TrafficLightType.TL_UNKNOWN : TrafficLightType.TL_GREEN;
        }).when(preImportFileDTO).getStepStatus(any(FileProcessingStepType.class));

        ImportFile importFile = new  ImportFile(importSourceManager,associatedFileDataManager,fileSystem);

        // Check invalid step
        try {
            importFile.process(preImportFileDTO);
            fail();
        } catch (ImportProcessException ex) {
            assertEquals("The file TEST_FILE.txt does not exist.", ex.getMessage());
        }
    }

    @Test
    void TestStepCheckActivePhotoFile() {
        com.jbr.middletier.backup.data.ImportFile fileInfo = mock(com.jbr.middletier.backup.data.ImportFile.class);
        when(fileInfo.getDate()).thenReturn(LocalDateTime.of(2023,12,23,0,0,0));
        List<FileInfo> fileInfos = Collections.singletonList(fileInfo);

        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        FileRepository fileRepository = mock(FileRepository.class);
        when(fileRepository.findByName(any())).thenReturn(fileInfos);

        CheckActivePhotoFile checkActivePhotoFile = new  CheckActivePhotoFile(importFileRepository,fileRepository,importSourceManager);

        PreImportFileDTO preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.isVideo()).thenReturn(true);
        when(preImportFileDTO.getDuration()).thenReturn(10.0);

        assertEquals(TrafficLightType.TL_GREEN,checkActivePhotoFile.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.isVideo()).thenReturn(true);
        when(preImportFileDTO.getDuration()).thenReturn(2.1);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.txt");

        assertEquals(TrafficLightType.TL_GREEN,checkActivePhotoFile.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.isVideo()).thenReturn(true);
        when(preImportFileDTO.getDuration()).thenReturn(2.1);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.mov");
        when(preImportFileDTO.getImportName()).thenReturn("TEST_FILE.txt");

        assertEquals(TrafficLightType.TL_GREEN,checkActivePhotoFile.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.isVideo()).thenReturn(true);
        when(preImportFileDTO.getDuration()).thenReturn(2.1);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.mov");
        when(preImportFileDTO.getImportName()).thenReturn("TEST_FILE.mp4");
        when(preImportFileDTO.getImportDate()).thenReturn(LocalDateTime.of(2023,12,23,0,0,1));

        assertEquals(TrafficLightType.TL_RED,checkActivePhotoFile.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.isVideo()).thenReturn(true);
        when(preImportFileDTO.getDuration()).thenReturn(2.1);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.mov");
        when(preImportFileDTO.getImportName()).thenReturn("TEST_FILE.mp4");
        when(preImportFileDTO.getImportDate()).thenReturn(LocalDateTime.of(2023,12,23,0,0,30));

        assertEquals(TrafficLightType.TL_GREEN,checkActivePhotoFile.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.isVideo()).thenReturn(true);
        when(preImportFileDTO.getDuration()).thenReturn(2.1);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.mov");
        when(preImportFileDTO.getImportName()).thenReturn("TEST_FILE.mp4");
        when(preImportFileDTO.getImportDate()).thenReturn(LocalDateTime.of(2023,12,22,0,0,30));

        assertEquals(TrafficLightType.TL_GREEN,checkActivePhotoFile.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.isVideo()).thenReturn(true);
        when(preImportFileDTO.getDuration()).thenReturn(2.1);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.mov");
        when(preImportFileDTO.getImportName()).thenReturn("TEST_FILE.mp4");
        when(preImportFileDTO.getImportDate()).thenReturn(null);

        assertEquals(TrafficLightType.TL_GREEN,checkActivePhotoFile.performStep(preImportFileDTO));
    }

    @Test
    void TestStepCheckFileIgnored() {
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);

        CheckFileIgnored checkFileIgnored = new CheckFileIgnored(importFileRepository,importSourceManager,ignoreFileRepository);

        PreImportFileDTO preImportFileDTO = mock(PreImportFileDTO.class);

        assertEquals(TrafficLightType.TL_GREEN,checkFileIgnored.performStep(preImportFileDTO));

        IgnoreFile ignoreFile = mock(IgnoreFile.class);
        when(ignoreFile.getMd5()).thenReturn(Optional.of(new MD5("8D4F46976377897DFADF214D0526CF56")));
        when(ignoreFile.getSize()).thenReturn(10L);
        when(ignoreFile.getName()).thenReturn("TEST_FILE.txt");
        when(ignoreFile.getDate()).thenReturn(LocalDateTime.of(2023,12,23,0,0,0));
        when(ignoreFile.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_IGNORE_FILE));
        List<IgnoreFile> ignoreFiles = Collections.singletonList(ignoreFile);

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.getMd5Optional()).thenReturn(Optional.of(new MD5("8D4F46976377897DFADF214D0526CF56")));
        when(preImportFileDTO.getSize()).thenReturn(10L);

        ignoreFileRepository = mock(IgnoreFileRepository.class);
        when(ignoreFileRepository.findAllByOrderByIdAsc()).thenReturn(ignoreFiles);

        checkFileIgnored = new CheckFileIgnored(importFileRepository,importSourceManager,ignoreFileRepository);

        assertEquals(TrafficLightType.TL_RED,checkFileIgnored.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.getMd5Optional()).thenReturn(Optional.of(new MD5("8D4F46976377897DFADF214D0526CF57")));
        when(preImportFileDTO.getSize()).thenReturn(11L);
        when(preImportFileDTO.getImportMd5Optional()).thenReturn(Optional.of(new MD5("8D4F46976377897DFADF214D0526CF56")));
        when(preImportFileDTO.getImportSize()).thenReturn(10L);
        assertEquals(TrafficLightType.TL_RED,checkFileIgnored.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.getMd5Optional()).thenReturn(Optional.of(new MD5("8D4F46976377897DFADF214D0526CF57")));
        when(preImportFileDTO.getSize()).thenReturn(10L);
        when(preImportFileDTO.getImportMd5Optional()).thenReturn(Optional.of(new MD5("8D4F46976377897DFADF214D0526CF57")));
        when(preImportFileDTO.getImportSize()).thenReturn(11L);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.txt");
        assertEquals(TrafficLightType.TL_AMBER,checkFileIgnored.performStep(preImportFileDTO));
    }

    private PreImportFileDTO setupImportFile() {
        PreImportFileDTO importFile = new PreImportFileDTO();

        importFile.setFilename("import.txt");
        importFile.setImportSize(11L);
        importFile.setImportMd5(new MD5("8D4F46976377897DFADF214D0526CF5C"));
        importFile.setImportDate(LocalDateTime.of(2023,12,23,0,22,0));
        importFile.setImportName("import.txt");

        return importFile;
    }

    private ImportFileDTO setupSimilarFile(PreImportFileDTO importFile) {
        ImportFileDTO similarFile = new ImportFileDTO();
        similarFile.setFilename("Similar.txt");
        similarFile.setSize(10L);
        similarFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        similarFile.setDate(LocalDateTime.of(2023,12,23,0,0,0));
        similarFile.setType(FileSystemObjectType.FSO_IGNORE_FILE);

        importFile.addSimilarFile(similarFile);

        return similarFile;
    }

    @Test
    void checkFileConfirmedImportedMd5() {
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);

        CheckFileConfirmedImported checkFileConfirmedImported = new CheckFileConfirmedImported(importFileRepository,importSourceManager);

        // Check that its amber if in post import.
        PreImportFileDTO  importFile = new PreImportFileDTO();
        importFile.setInPostImport(true);

        assertEquals(TrafficLightType.TL_AMBER, checkFileConfirmedImported.performStep(importFile));

        // Check that its red if there are no matches.
        importFile = new PreImportFileDTO();

        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        // Need to get the process to go through all the paths.
        importFile = setupImportFile();
        ImportFileDTO similarFile = setupSimilarFile(importFile);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setType(FileSystemObjectType.FSO_FILE);
        similarFile.setMd5(null);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        importFile.setImportMd5(null);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        importFile.setImportMd5(new MD5("8D4F46976377897DFADF214D0526CF58"));
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        importFile.setImportMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));
    }

    @Test
    void checkFileConfirmedImportedFilename() {
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);

        CheckFileConfirmedImported checkFileConfirmedImported = new CheckFileConfirmedImported(importFileRepository,importSourceManager);

        PreImportFileDTO importFile = setupImportFile();
        ImportFileDTO similarFile = setupSimilarFile(importFile);
        similarFile.setType(FileSystemObjectType.FSO_FILE);
        similarFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        importFile.setImportMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));

        similarFile.setFilename(null);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setFilename("");
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setFilename("similar.txt");
        importFile.setImportName(null);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportName("");
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportName("sim.txt");
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportName("similar.txt");
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));
    }

    @Test
    void checkFileConfirmedImportedFileSize() {
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);

        CheckFileConfirmedImported checkFileConfirmedImported = new CheckFileConfirmedImported(importFileRepository,importSourceManager);

        PreImportFileDTO importFile = setupImportFile();
        ImportFileDTO similarFile = setupSimilarFile(importFile);
        similarFile.setFilename("similar.txt");
        importFile.setImportName("similar.txt");
        similarFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        importFile.setImportMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        similarFile.setType(FileSystemObjectType.FSO_FILE);

        similarFile.setSize(null);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setSize(10L);
        importFile.setImportSize(null);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportSize(11L);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportSize(10L);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));
    }

    @Test
    void checkFileConfirmedImportedFileDate() {
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);

        CheckFileConfirmedImported checkFileConfirmedImported = new CheckFileConfirmedImported(importFileRepository,importSourceManager);

        PreImportFileDTO importFile = setupImportFile();
        ImportFileDTO similarFile = setupSimilarFile(importFile);
        similarFile.setSize(10L);
        similarFile.setFilename("similar.txt");
        similarFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        similarFile.setType(FileSystemObjectType.FSO_FILE);
        importFile.setImportSize(10L);
        importFile.setImportName("similar.txt");
        importFile.setImportMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));

        similarFile.setDate(null);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setDate(LocalDateTime.of(2020, 1, 1, 0, 0));
        importFile.setImportDate(null);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportDate(LocalDateTime.of(2020, 1, 2, 0, 0));
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportDate(LocalDateTime.of(2020, 1, 1, 0, 0));
        assertEquals(TrafficLightType.TL_GREEN, checkFileConfirmedImported.performStep(importFile));

        // Add a second similar file.
        ImportFileDTO similarFile2 = new ImportFileDTO();
        similarFile2.setSize(10L);
        similarFile2.setFilename("xsimilar.txt");
        similarFile2.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        similarFile2.setDate(LocalDateTime.of(2020, 1, 1, 0, 0));
        similarFile2.setType(FileSystemObjectType.FSO_FILE);
        importFile.addSimilarFile(similarFile2);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));
    }

    @Test
    void checkDuplicateFileByOriginalMd5() {
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        FileRepository fileRepository = mock(FileRepository.class);
        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);
        CustomMetaDataRepository customMetaDataRepository = mock(CustomMetaDataRepository.class);

        when(associatedFileDataManager.findAllSynchronize()).thenReturn(Collections.emptyList());
        when(fileRepository.findByMd5(any())).thenReturn(Collections.emptyList());

        // Library MP4 in the DB with its own MD5
        FileInfo libraryMp4 = mock(FileInfo.class);
        when(libraryMp4.getIdAndType()).thenReturn(new FileSystemObjectId(42, FileSystemObjectType.FSO_FILE));
        when(libraryMp4.getSize()).thenReturn(1163000L);
        when(libraryMp4.getMd5()).thenReturn(Optional.of(new MD5("AABBCCDDAABBCCDDAABBCCDDAABBCCDD")));
        when(libraryMp4.getDate()).thenReturn(LocalDateTime.of(2025, 3, 2, 19, 25, 54));
        // Path == name so validSource() skips the source list check
        when(fileSystemObjectManager.getFile(libraryMp4)).thenReturn(new File("IMG_1015.mp4"));
        when(fileRepository.findById(42)).thenReturn(Optional.of(libraryMp4));

        // CustomMetaData linking the MP4 to the original MOV
        CustomMetaData customMetaData = new CustomMetaData();
        customMetaData.setId(42);
        customMetaData.setOriginalFile("img_1015.mov");
        customMetaData.setOriginalMd5("8D4F46976377897DFADF214D0526CF56");
        customMetaData.setOriginalSize(1821435L);
        when(customMetaDataRepository.findByOriginalMd5("8D4F46976377897DFADF214D0526CF56"))
                .thenReturn(Collections.singletonList(customMetaData));

        CheckDuplicateFile checkDuplicateFile = new CheckDuplicateFile(importFileRepository, importSourceManager,
                associatedFileDataManager, fileRepository, fileSystemObjectManager, customMetaDataRepository);

        // Re-presented MOV: its MD5 is the original MOV MD5 stored in custom_meta_data
        PreImportFileDTO movFile = new PreImportFileDTO();
        movFile.setFilename("IMG_1015.MOV");
        movFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF56"));
        movFile.setSize(1821435L);

        TrafficLightType result = checkDuplicateFile.performStep(movFile);

        // Not a duplicate (only one similar file found), but the library MP4 is now in the similar list
        assertEquals(TrafficLightType.TL_GREEN, result);
        assertEquals(1, movFile.getSimilarFiles().size());

        ImportFileBaseDTO similar = movFile.getSimilarFiles().get(0);
        assertEquals("img_1015.mov", similar.getOriginalFile());
        assertEquals("8D4F46976377897DFADF214D0526CF56", similar.getOriginalMd5());
        assertEquals(1821435L, similar.getOriginalSize());
        assertEquals(LocalDateTime.of(2025, 3, 2, 19, 25, 54), similar.getDate());
    }

    @Test
    void checkFileConfirmedImportedByOriginalCustomFields() {
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);

        CheckFileConfirmedImported checkFileConfirmedImported = new CheckFileConfirmedImported(importFileRepository, importSourceManager);

        LocalDateTime originalDate = LocalDateTime.of(2025, 3, 2, 19, 25, 54);

        // The incoming re-presented MOV
        PreImportFileDTO movFile = new PreImportFileDTO();
        movFile.setFilename("IMG_1015.MOV");
        movFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF56"));
        movFile.setSize(1821435L);
        movFile.setImportDate(originalDate);

        // The library MP4 as found by CheckDuplicateFile via custom metadata
        ImportFileBaseDTO libraryMp4Entry = new ImportFileBaseDTO();
        libraryMp4Entry.setType(FileSystemObjectType.FSO_FILE);
        libraryMp4Entry.setFilename("IMG_1015.mp4");
        libraryMp4Entry.setDate(originalDate);
        libraryMp4Entry.setOriginalFile("img_1015.mov");
        libraryMp4Entry.setOriginalMd5("8D4F46976377897DFADF214D0526CF56");
        libraryMp4Entry.setOriginalSize(1821435L);
        movFile.addSimilarFile(libraryMp4Entry);

        assertEquals(TrafficLightType.TL_GREEN, checkFileConfirmedImported.performStep(movFile));

        // Wrong original MD5 -> RED
        libraryMp4Entry.setOriginalMd5("AABBCCDDAABBCCDDAABBCCDDAABBCCDD");
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(movFile));
        libraryMp4Entry.setOriginalMd5("8D4F46976377897DFADF214D0526CF56");

        // Wrong original size -> RED
        libraryMp4Entry.setOriginalSize(9999L);
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(movFile));
        libraryMp4Entry.setOriginalSize(1821435L);

        // Wrong original filename -> RED
        libraryMp4Entry.setOriginalFile("other.mov");
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(movFile));
        libraryMp4Entry.setOriginalFile("img_1015.mov");

        // Wrong date -> RED
        libraryMp4Entry.setDate(originalDate.plusHours(1));
        assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(movFile));
    }
}
