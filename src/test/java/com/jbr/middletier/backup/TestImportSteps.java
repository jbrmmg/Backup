package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import com.jbr.middletier.backup.manager.importing.step.process.ImportFile;
import com.jbr.middletier.backup.manager.importing.step.process.ImportProcessException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;

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
            //{FileProcessingStepType@17793}FPS_CHECK_FILE_CONFIRMED_IMPORTED -> {ArrayList@17852} size = 3
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
            //{FileProcessingStepType@17793}FPS_CHECK_FILE_CONFIRMED_IMPORTED -> {ArrayList@17852} size = 3
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
            //{FileProcessingStepType@17793}FPS_CHECK_FILE_CONFIRMED_IMPORTED -> {ArrayList@17852} size = 3
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
            //{FileProcessingStepType@17793}FPS_CHECK_FILE_CONFIRMED_IMPORTED -> {ArrayList@17852} size = 3
            importFile.process(preImportFileDTO);
            Assert.fail();
        } catch (ImportProcessException ex) {
            Assert.assertEquals("The file TEST_FILE.txt does not exist.", ex.getMessage());
        }
    }
}
