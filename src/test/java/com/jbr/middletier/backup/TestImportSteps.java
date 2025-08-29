package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import com.jbr.middletier.backup.manager.importing.step.CheckActivePhotoFile;
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
}
