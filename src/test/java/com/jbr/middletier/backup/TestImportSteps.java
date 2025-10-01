package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.IgnoreFileRepository;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.ImportFileDTO;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import com.jbr.middletier.backup.manager.importing.step.CheckActivePhotoFile;
import com.jbr.middletier.backup.manager.importing.step.CheckFileConfirmedImported;
import com.jbr.middletier.backup.manager.importing.step.CheckFileIgnored;
import com.jbr.middletier.backup.manager.importing.step.process.ImportFile;
import com.jbr.middletier.backup.manager.importing.step.process.ImportProcessException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.jbr.middletier.backup.manager.importing.FileProcessingStepType.FPS_CHECK_FILE_CONFIRMED_IMPORTED;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestImportSteps {
    @Test
    public void TestStepProcessImportFileInvalidState() {
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
            Assert.fail();
        } catch (ImportProcessException ex) {
            Assert.assertEquals("TEST_FILE.txt invalid step FPS_CHECK_FILE_CONFIRMED_IMPORTED status TL_GREEN", ex.getMessage());
        }
    }

    @Test
    public void TestStepProcessImportFileDestinationEmpty1() {
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
            Assert.fail();
        } catch (ImportProcessException ex) {
            Assert.assertEquals("Destination of TEST_FILE.txt is empty", ex.getMessage());
        }
    }

    @Test
    public void TestStepProcessImportFileDestinationEmpty2() {
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
            Assert.fail();
        } catch (ImportProcessException ex) {
            Assert.assertEquals("Destination of TEST_FILE.txt is empty", ex.getMessage());
        }
    }

    @Test
    public void TestStepProcessImportFileNotExist() {
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
            Assert.fail();
        } catch (ImportProcessException ex) {
            Assert.assertEquals("The file TEST_FILE.txt does not exist.", ex.getMessage());
        }
    }

    @Test
    public void TestStepCheckActivePhotoFile() {
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

        Assert.assertEquals(TrafficLightType.TL_GREEN,checkActivePhotoFile.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.isVideo()).thenReturn(true);
        when(preImportFileDTO.getDuration()).thenReturn(2.1);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.txt");

        Assert.assertEquals(TrafficLightType.TL_GREEN,checkActivePhotoFile.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.isVideo()).thenReturn(true);
        when(preImportFileDTO.getDuration()).thenReturn(2.1);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.mov");
        when(preImportFileDTO.getImportName()).thenReturn("TEST_FILE.txt");

        Assert.assertEquals(TrafficLightType.TL_GREEN,checkActivePhotoFile.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.isVideo()).thenReturn(true);
        when(preImportFileDTO.getDuration()).thenReturn(2.1);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.mov");
        when(preImportFileDTO.getImportName()).thenReturn("TEST_FILE.mp4");
        when(preImportFileDTO.getImportDate()).thenReturn(LocalDateTime.of(2023,12,23,0,0,1));

        Assert.assertEquals(TrafficLightType.TL_RED,checkActivePhotoFile.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.isVideo()).thenReturn(true);
        when(preImportFileDTO.getDuration()).thenReturn(2.1);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.mov");
        when(preImportFileDTO.getImportName()).thenReturn("TEST_FILE.mp4");
        when(preImportFileDTO.getImportDate()).thenReturn(LocalDateTime.of(2023,12,23,0,0,30));

        Assert.assertEquals(TrafficLightType.TL_GREEN,checkActivePhotoFile.performStep(preImportFileDTO));
    }

    @Test
    public void TestStepCheckFileIgnored() {
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);

        CheckFileIgnored checkFileIgnored = new CheckFileIgnored(importFileRepository,importSourceManager,ignoreFileRepository);

        PreImportFileDTO preImportFileDTO = mock(PreImportFileDTO.class);

        Assert.assertEquals(TrafficLightType.TL_GREEN,checkFileIgnored.performStep(preImportFileDTO));

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

        Assert.assertEquals(TrafficLightType.TL_RED,checkFileIgnored.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.getMd5Optional()).thenReturn(Optional.of(new MD5("8D4F46976377897DFADF214D0526CF57")));
        when(preImportFileDTO.getSize()).thenReturn(11L);
        when(preImportFileDTO.getImportMd5Optional()).thenReturn(Optional.of(new MD5("8D4F46976377897DFADF214D0526CF56")));
        when(preImportFileDTO.getImportSize()).thenReturn(10L);
        Assert.assertEquals(TrafficLightType.TL_RED,checkFileIgnored.performStep(preImportFileDTO));

        preImportFileDTO = mock(PreImportFileDTO.class);
        when(preImportFileDTO.getMd5Optional()).thenReturn(Optional.of(new MD5("8D4F46976377897DFADF214D0526CF57")));
        when(preImportFileDTO.getSize()).thenReturn(10L);
        when(preImportFileDTO.getImportMd5Optional()).thenReturn(Optional.of(new MD5("8D4F46976377897DFADF214D0526CF57")));
        when(preImportFileDTO.getImportSize()).thenReturn(11L);
        when(preImportFileDTO.getFilename()).thenReturn("TEST_FILE.txt");
        Assert.assertEquals(TrafficLightType.TL_AMBER,checkFileIgnored.performStep(preImportFileDTO));
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
    public void checkFileConfirmedImportedMd5() {
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);

        CheckFileConfirmedImported checkFileConfirmedImported = new CheckFileConfirmedImported(importFileRepository,importSourceManager);

        // Check that its amber if in post import.
        PreImportFileDTO  importFile = new PreImportFileDTO();
        importFile.setInPostImport(true);

        Assert.assertEquals(TrafficLightType.TL_AMBER, checkFileConfirmedImported.performStep(importFile));

        // Check that its red if there are no matches.
        importFile = new PreImportFileDTO();

        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        // Need to get the process to go through all the paths.
        importFile = setupImportFile();
        ImportFileDTO similarFile = setupSimilarFile(importFile);
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setType(FileSystemObjectType.FSO_FILE);
        similarFile.setMd5(null);
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        importFile.setImportMd5(null);
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        importFile.setImportMd5(new MD5("8D4F46976377897DFADF214D0526CF58"));
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        importFile.setImportMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));
    }

    @Test
    public void checkFileConfirmedImportedFilename() {
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        ImportSourceManager importSourceManager = mock(ImportSourceManager.class);

        CheckFileConfirmedImported checkFileConfirmedImported = new CheckFileConfirmedImported(importFileRepository,importSourceManager);

        PreImportFileDTO importFile = setupImportFile();
        ImportFileDTO similarFile = setupSimilarFile(importFile);
        similarFile.setType(FileSystemObjectType.FSO_FILE);
        similarFile.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        importFile.setImportMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));

        similarFile.setFilename(null);
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setFilename("");
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setFilename("similar.txt");
        importFile.setImportName(null);
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportName("");
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportName("sim.txt");
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportName("similar.txt");
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));
    }

    @Test
    public void checkFileConfirmedImportedFileSize() {
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
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setSize(10L);
        importFile.setImportSize(null);
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportSize(11L);
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportSize(10L);
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));
    }

    @Test
    public void checkFileConfirmedImportedFileDate() {
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
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        similarFile.setDate(LocalDateTime.of(2020, 1, 1, 0, 0));
        importFile.setImportDate(null);
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportDate(LocalDateTime.of(2020, 1, 2, 0, 0));
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));

        importFile.setImportDate(LocalDateTime.of(2020, 1, 1, 0, 0));
        Assert.assertEquals(TrafficLightType.TL_GREEN, checkFileConfirmedImported.performStep(importFile));

        // Add a second similar file.
        ImportFileDTO similarFile2 = new ImportFileDTO();
        similarFile2.setSize(10L);
        similarFile2.setFilename("xsimilar.txt");
        similarFile2.setMd5(new MD5("8D4F46976377897DFADF214D0526CF57"));
        similarFile2.setDate(LocalDateTime.of(2020, 1, 1, 0, 0));
        similarFile2.setType(FileSystemObjectType.FSO_FILE);
        importFile.addSimilarFile(similarFile2);
        Assert.assertEquals(TrafficLightType.TL_RED, checkFileConfirmedImported.performStep(importFile));
    }
}
