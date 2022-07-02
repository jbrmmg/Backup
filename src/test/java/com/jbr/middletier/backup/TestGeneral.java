package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.config.DefaultProfileUtil;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.ApiError;
import com.jbr.middletier.backup.integration.FileTester;
import com.jbr.middletier.backup.manager.*;
import com.jbr.middletier.backup.schedule.GatherSynchronizeCtrl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.jbr.middletier.backup.data.ClassificationActionType.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestGeneral extends WebTester {
    @Test
    public void TestDefaultProfile() {
        SpringApplication app = mock(SpringApplication.class);

        Assert.assertNotNull(app);
        DefaultProfileUtil.addDefaultProfile(app);
    }

    @Test
    public void TestClassificationActionType() {
        Assert.assertEquals(CA_BACKUP,ClassificationActionType.getClassificationActionType("BACKUP"));
        Assert.assertEquals(CA_DELETE,ClassificationActionType.getClassificationActionType("DELETE"));
        Assert.assertEquals(CA_FOLDER,ClassificationActionType.getClassificationActionType("FOLDER"));
        Assert.assertEquals(CA_IGNORE,ClassificationActionType.getClassificationActionType("IGNORE"));
        Assert.assertEquals(CA_WARN,ClassificationActionType.getClassificationActionType("WARN"));

        try {
            ClassificationActionType.getClassificationActionType("BLAH");
            Assert.fail();
        } catch (IllegalStateException e) {
            Assert.assertEquals("BLAH is not a valid Classification Action", e.getMessage());
        }
    }

    @Test
    public void TestDTOs() {
        GatherDataDTO gatherDataDTO = new GatherDataDTO(1);
        Assert.assertEquals(1,gatherDataDTO.getUnderlyingId());

        gatherDataDTO.setProblems();
        Assert.assertTrue(gatherDataDTO.hasProblems());

        DirectoryInfo directoryInfo = new DirectoryInfo();
        directoryInfo.setRemoved();
        Assert.assertTrue(directoryInfo.getRemoved());

        SynchronizeDTO synchronizeDTO = new SynchronizeDTO(1);
        Assert.assertEquals((Integer) 1, synchronizeDTO.getId());

        SyncDataDTO syncDataDTO = new SyncDataDTO(1);
        syncDataDTO.increment(SyncDataDTO.SyncDataCountType.FILES_DELETED);
        syncDataDTO.increment(SyncDataDTO.SyncDataCountType.DIRECTORIES_DELETED);
        syncDataDTO.increment(SyncDataDTO.SyncDataCountType.FILES_COPIED);
        syncDataDTO.increment(SyncDataDTO.SyncDataCountType.DIRECTORIES_COPIED);
        Assert.assertEquals(1,syncDataDTO.getCount(SyncDataDTO.SyncDataCountType.FILES_DELETED));
        Assert.assertEquals(1,syncDataDTO.getCount(SyncDataDTO.SyncDataCountType.DIRECTORIES_DELETED));
        Assert.assertEquals(1,syncDataDTO.getCount(SyncDataDTO.SyncDataCountType.FILES_COPIED));
        Assert.assertEquals(1,syncDataDTO.getCount(SyncDataDTO.SyncDataCountType.DIRECTORIES_COPIED));

        OkStatus okStatus = new OkStatus();
        okStatus.setStatus("Test");
        Assert.assertEquals("Test", okStatus.getStatus());

        SourceDTO sourceDTO = new SourceDTO(1, "Test");
        Assert.assertEquals((Integer)1,sourceDTO.getId());
        Assert.assertEquals("Test",sourceDTO.getPath());
        sourceDTO.setLocation(new LocationDTO());
        sourceDTO.setStatus(SourceStatusType.SST_OK);
        sourceDTO.setFilter("Test");
        sourceDTO.incrementFileCount();
        sourceDTO.incrementDirectoryCount();
        sourceDTO.increaseFileSize(10);
        sourceDTO.increaseFileSize(100);
        sourceDTO.increaseFileSize(60);

        class SourceDTO2 extends  SourceDTO {
            public SourceDTO2(SourceDTO source) {
                super(source);
            }
        }
        SourceDTO2 sourceDTO2 = new SourceDTO2(sourceDTO);
        Assert.assertEquals("Test",sourceDTO2.getFilter());

        ImportSourceDTO importSourceDTO = new ImportSourceDTO();
        importSourceDTO.setDestinationId(1);
        Assert.assertEquals((Integer)1,importSourceDTO.getDestinationId());

        FileInfo mockFileInfo = mock(FileInfo.class);
        when(mockFileInfo.getIdAndType()).thenReturn(new FileSystemObjectId(1,FileSystemObjectType.FSO_FILE));
        when(mockFileInfo.getName()).thenReturn("file");
        ActionConfirm actionConfirm = mock(ActionConfirm.class);
        when(actionConfirm.getAction()).thenReturn(ActionConfirmType.AC_IMPORT);
        when(actionConfirm.getId()).thenReturn(1);
        when(actionConfirm.getPath()).thenReturn(mockFileInfo);
        when(actionConfirm.getFlags()).thenReturn("flag");
        when(actionConfirm.getParameter()).thenReturn("n");
        when(actionConfirm.getParameterRequired()).thenReturn(false);
        when(actionConfirm.confirmed()).thenReturn(false);
        ActionConfirmDTO actionConfirmDTO = new ActionConfirmDTO(actionConfirm);
        Assert.assertEquals(ActionConfirmType.AC_IMPORT,actionConfirmDTO.getAction());
        Assert.assertEquals(1,actionConfirmDTO.getId());
        Assert.assertEquals("flag",actionConfirmDTO.getFlags());
        Assert.assertEquals("n",actionConfirmDTO.getParameter());
        Assert.assertFalse(actionConfirmDTO.getParameterRequired());
        Assert.assertFalse(actionConfirmDTO.getConfirmed());
        Assert.assertEquals("file",actionConfirmDTO.getFileName());
        Assert.assertEquals(1,actionConfirmDTO.getFileId());
    }

    @Test
    public void TestActionConfirmType() {
        Assert.assertEquals(ActionConfirmType.AC_DELETE,ActionConfirmType.getActionConfirmType("DELETE"));
        Assert.assertEquals(ActionConfirmType.AC_DELETE_DUPLICATE,ActionConfirmType.getActionConfirmType("DELETE_DUP"));
        Assert.assertEquals(ActionConfirmType.AC_IMPORT,ActionConfirmType.getActionConfirmType("IMPORT"));

        try {
            ActionConfirmType.getActionConfirmType("BLAH");
            Assert.fail();
        } catch (IllegalStateException e) {
            Assert.assertEquals("BLAH is not a valid Action Confirm type", e.getMessage());
        }
    }

    @Test
    public void TestApiError() {
        Throwable ex = mock(Throwable.class);
        when(ex.getLocalizedMessage()).thenReturn("Error");

        ApiError test = new ApiError(null, "Blah", ex);

        Assert.assertNotNull(test.getTimestamp());
        Assert.assertEquals("Blah", test.getMessage());
        Assert.assertEquals("Error", test.getDebugMessage());
    }

    @Test
    public void TestMD5() {
        MD5 md5 = new MD5((String)null);
        Assert.assertFalse(md5.isSet());

        MD5 md5b = new MD5("");
        Assert.assertFalse(md5b.isSet());

        md5b = new MD5(md5);
        Assert.assertFalse(md5.isSet());

        MD5 md5c = new MD5("Test");
        Assert.assertTrue(md5c.isSet());

        MD5 md5d = new MD5("Test2");
        Assert.assertTrue(md5d.isSet());

        MD5 md5e = new MD5("Test");
        Assert.assertTrue(md5e.isSet());

        Assert.assertTrue(md5c.compare(md5c, false));
        Assert.assertTrue(md5c.compare(md5e, false));
        Assert.assertFalse(md5c.compare(md5d, false));
        Assert.assertTrue(md5c.compare(md5b,true));
        Assert.assertTrue(md5b.compare(md5c,true));
        Assert.assertFalse(md5c.compare(md5d,true));
    }

    @Test
    public void TestFsoId() {
        FileSystemObjectId fsoId = new FileSystemObjectId(1,FileSystemObjectType.FSO_FILE);
        FileSystemObjectId fsoId2 = new FileSystemObjectId(2,FileSystemObjectType.FSO_FILE);
        FileSystemObjectId fsoId3 = new FileSystemObjectId(1,FileSystemObjectType.FSO_DIRECTORY);
        FileSystemObjectId fsoId4 = new FileSystemObjectId(1,FileSystemObjectType.FSO_FILE);
        Object testObj = "not an id";
        Assert.assertNotEquals(fsoId3.hashCode(),fsoId.hashCode());
        Assert.assertNotEquals(fsoId, fsoId2);
        Assert.assertNotEquals(testObj, fsoId);
        Assert.assertNotEquals(null, fsoId);
        Assert.assertNotEquals(fsoId, fsoId3);
        Assert.assertEquals(fsoId, fsoId4);

        @SuppressWarnings("ConstantConditions")
        boolean test = fsoId.equals(null);
        //noinspection ConstantConditions
        Assert.assertFalse(test);

        test = fsoId.equals(testObj);
        Assert.assertFalse(test);
    }

    @Test
    public void TestSourceStatus() {
        Assert.assertEquals(SourceStatusType.SST_OK, SourceStatusType.getSourceStatusType("OK"));
        Assert.assertEquals(SourceStatusType.SST_GATHERING, SourceStatusType.getSourceStatusType("GATHERING"));
        Assert.assertEquals(SourceStatusType.SST_ERROR, SourceStatusType.getSourceStatusType("ERROR"));

        try {
            SourceStatusType.getSourceStatusType("Blah");
            Assert.fail();
        } catch (IllegalStateException e) {
            Assert.assertEquals("Blah is not a valid Source Status type", e.getMessage());
        }
    }

    @Test
    public void TestSynchronizeRecord() {
        SynchronizeDTO synchronizeDTO = new SynchronizeDTO();
        synchronizeDTO.setSource(new SourceDTO());
        synchronizeDTO.setDestination(new SourceDTO());

        synchronizeDTO.getSource().setPath("Test");
        synchronizeDTO.getSource().setLocation(new LocationDTO());
        synchronizeDTO.getSource().getLocation().setId(1);
        synchronizeDTO.getSource().getLocation().setName("Test");
        synchronizeDTO.getSource().getLocation().setSize("1GB");
        synchronizeDTO.getSource().setStatus(SourceStatusType.SST_OK);
        synchronizeDTO.getDestination().setPath("Test");
        synchronizeDTO.getDestination().setLocation(new LocationDTO());
        synchronizeDTO.getDestination().getLocation().setId(1);
        synchronizeDTO.getDestination().getLocation().setName("Test");
        synchronizeDTO.getDestination().getLocation().setSize("1GB");
        synchronizeDTO.getDestination().setStatus(SourceStatusType.SST_OK);

        Synchronize synchronize = new Synchronize();
        synchronize.update(synchronizeDTO);

        Assert.assertEquals("Test -> Test", synchronize.toString());
    }

    @Test
    public void TestHierarchyResponse() {
        HierarchyResponse hierarchyResponse = new HierarchyResponse();
        Assert.assertEquals(-1,hierarchyResponse.getId());
        Assert.assertEquals(0,hierarchyResponse.getLevel());
        Assert.assertEquals("",hierarchyResponse.getDisplayName());
        Assert.assertEquals("/",hierarchyResponse.getPath());
        Assert.assertTrue(hierarchyResponse.getDirectory());
    }

    @Test
    public void TestLocationDTO() {
        Location location = new Location();
        location.setId(1);
        location.setCheckDuplicates();
        location.setName("Test");
        location.setSize("1G");

        LocationDTO locationDTO = location.getLocationDTO();
        Assert.assertEquals("Test", locationDTO.getName());
    }

    @Test
    public void TestSourceDTO() {
        Source source = new Source();
        source.setStatus(SourceStatusType.SST_OK);
        source.setPath("Test");
        source.setFilter("Te");

        Location location = new Location();
        location.setId(1);
        location.setCheckDuplicates();
        location.setName("Test");
        location.setSize("1G");

        source.setLocation(location);

        SourceDTO sourceDTO = source.getSourceDTO();
        Assert.assertNotNull(sourceDTO);
    }

    @Test
    public void WebLogTest() {
        ApplicationProperties testProperties = mock(ApplicationProperties.class);
        when(testProperties.getWebLogUrl()).thenReturn("http://test");

        Answer<Boolean> infoCheck = invocationOnMock -> {
            Assert.assertEquals("http://test", invocationOnMock.getArguments()[0]);
            Assert.assertTrue(invocationOnMock.getArguments()[1].toString().contains("\"INFO\""));
            return false;
        };
        RestTemplate testRestTemplate = mock(RestTemplate.class, infoCheck);

        RestTemplateBuilder testRestTemplateBuilder = mock(RestTemplateBuilder.class);
        when(testRestTemplateBuilder.build()).thenReturn(testRestTemplate);

        BackupManager testBackup = new BackupManager(testProperties,testRestTemplateBuilder);
        testBackup.postWebLog(BackupManager.webLogLevel.INFO, "Test Info");

        Answer<Boolean> debugCheck = invocationOnMock -> {
            Assert.assertEquals("http://test", invocationOnMock.getArguments()[0]);
            Assert.assertTrue(invocationOnMock.getArguments()[1].toString().contains("\"DEBUG\""));
            return false;
        };
        testRestTemplate = mock(RestTemplate.class, debugCheck);
        when(testRestTemplateBuilder.build()).thenReturn(testRestTemplate);

        testBackup.postWebLog(BackupManager.webLogLevel.DEBUG, "Test Info");

        Answer<Boolean> warnCheck = invocationOnMock -> {
            Assert.assertEquals("http://test", invocationOnMock.getArguments()[0]);
            Assert.assertTrue(invocationOnMock.getArguments()[1].toString().contains("\"WARN\""));
            return false;
        };
        testRestTemplate = mock(RestTemplate.class, warnCheck);
        when(testRestTemplateBuilder.build()).thenReturn(testRestTemplate);

        testBackup.postWebLog(BackupManager.webLogLevel.WARN, "Test Info");

        Answer<Boolean> errorCheck = invocationOnMock -> {
            Assert.assertEquals("http://test", invocationOnMock.getArguments()[0]);
            Assert.assertTrue(invocationOnMock.getArguments()[1].toString().contains("\"ERROR\""));
            return false;
        };
        testRestTemplate = mock(RestTemplate.class, errorCheck);
        when(testRestTemplateBuilder.build()).thenReturn(testRestTemplate);

        testBackup.postWebLog(BackupManager.webLogLevel.ERROR, "Test Info");
    }

    @Test
    public void TestCronClass() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        when(applicationProperties.getGatherEnabled()).thenReturn(true);

        ActionManager actionManager = mock(ActionManager.class);

        DriveManager driveManager = mock(DriveManager.class);

        DuplicateManager duplicateManager = mock(DuplicateManager.class);

        SynchronizeManager synchronizeManager = mock(SynchronizeManager.class);

        GatherSynchronizeCtrl gatherSynchronizeCtrl = new GatherSynchronizeCtrl(applicationProperties,
                actionManager,
                driveManager,
                duplicateManager,
                synchronizeManager);

        gatherSynchronizeCtrl.gatherCron();
        verify(actionManager, times(1)).sendActionEmail();
        verify(driveManager, times(1)).gather();
        verify(duplicateManager, times(1)).duplicateCheck();
        verify(synchronizeManager, times(1)).synchronize();
    }

    @Test
    public void TestCronClassFail() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        when(applicationProperties.getGatherEnabled()).thenReturn(true);

        ActionManager actionManager = mock(ActionManager.class);
        doThrow(new IllegalStateException()).when(actionManager).sendActionEmail();

        DriveManager driveManager = mock(DriveManager.class);

        DuplicateManager duplicateManager = mock(DuplicateManager.class);

        SynchronizeManager synchronizeManager = mock(SynchronizeManager.class);

        GatherSynchronizeCtrl gatherSynchronizeCtrl = new GatherSynchronizeCtrl(applicationProperties,
                actionManager,
                driveManager,
                duplicateManager,
                synchronizeManager);

        gatherSynchronizeCtrl.gatherCron();
        verify(actionManager, times(1)).sendActionEmail();
        verify(driveManager, times(0)).gather();
        verify(duplicateManager, times(0)).duplicateCheck();
        verify(synchronizeManager, times(0)).synchronize();
    }

    @Test
    public void TestCronClassDisabled() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        when(applicationProperties.getGatherEnabled()).thenReturn(false);

        ActionManager actionManager = mock(ActionManager.class);
        doThrow(new IllegalStateException()).when(actionManager).sendActionEmail();

        DriveManager driveManager = mock(DriveManager.class);

        DuplicateManager duplicateManager = mock(DuplicateManager.class);

        SynchronizeManager synchronizeManager = mock(SynchronizeManager.class);

        GatherSynchronizeCtrl gatherSynchronizeCtrl = new GatherSynchronizeCtrl(applicationProperties,
                actionManager,
                driveManager,
                duplicateManager,
                synchronizeManager);

        gatherSynchronizeCtrl.gatherCron();
        verify(actionManager, times(0)).sendActionEmail();
        verify(driveManager, times(0)).gather();
        verify(duplicateManager, times(0)).duplicateCheck();
        verify(synchronizeManager, times(0)).synchronize();
    }

    @Test
    public void validateFileStatusType() {
        ImportFileStatusType type = ImportFileStatusType.getFileStatusType("COMPLETE");
        Assert.assertEquals(ImportFileStatusType.IFS_COMPLETE, type);

        type = ImportFileStatusType.getFileStatusType("READ");
        Assert.assertEquals(ImportFileStatusType.IFS_READ, type);

        try {
            ImportFileStatusType.getFileStatusType("Blah");
        } catch (IllegalStateException e) {
            Assert.assertEquals("Blah is not a valid Import File Status", e.getMessage());
        }
    }

    @Test
    public void sychronizeProblem1() {
        Source syncSource = mock(Source.class);
        when(syncSource.getStatus()).thenReturn(null);
        when(syncSource.getPath()).thenReturn("Source");

        Source syncDestination = mock(Source.class);
        when(syncDestination.getStatus()).thenReturn(null);
        when(syncDestination.getPath()).thenReturn("Destination");

        Synchronize synchronize = mock(Synchronize.class);
        when(synchronize.getSource()).thenReturn(syncSource);
        when(synchronize.getDestination()).thenReturn(syncDestination);
        List<Synchronize> synchronizeList = new ArrayList<>();
        synchronizeList.add(synchronize);

        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(associatedFileDataManager.internalFindAllSynchronize()).thenReturn(synchronizeList);

        BackupManager backupManager = mock(BackupManager.class);

        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager actionManager = mock(ActionManager.class);

        FileSystem fileSystem = mock(FileSystem.class);

        SynchronizeManager synchronizeManager = new SynchronizeManager(associatedFileDataManager,
                backupManager,
                fileSystemObjectManager,
                actionManager,
                fileSystem);

        List<SyncDataDTO> syncData = synchronizeManager.synchronize();
        Assert.assertEquals(1, syncData.size());
        Assert.assertTrue(syncData.get(0).hasProblems());
        verify(backupManager, times(1)).postWebLog(BackupManager.webLogLevel.WARN,"Skipping as source not OK");
    }

    @Test
    public void sychronizeProblem2() {
        Source syncSource = mock(Source.class);
        when(syncSource.getStatus()).thenReturn(SourceStatusType.SST_ERROR);
        when(syncSource.getPath()).thenReturn("Source");

        Source syncDestination = mock(Source.class);
        when(syncDestination.getStatus()).thenReturn(null);
        when(syncDestination.getPath()).thenReturn("Destination");

        Synchronize synchronize = mock(Synchronize.class);
        when(synchronize.getSource()).thenReturn(syncSource);
        when(synchronize.getDestination()).thenReturn(syncDestination);
        List<Synchronize> synchronizeList = new ArrayList<>();
        synchronizeList.add(synchronize);

        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(associatedFileDataManager.internalFindAllSynchronize()).thenReturn(synchronizeList);

        BackupManager backupManager = mock(BackupManager.class);

        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager actionManager = mock(ActionManager.class);

        FileSystem fileSystem = mock(FileSystem.class);

        SynchronizeManager synchronizeManager = new SynchronizeManager(associatedFileDataManager,
                backupManager,
                fileSystemObjectManager,
                actionManager,
                fileSystem);

        List<SyncDataDTO> syncData = synchronizeManager.synchronize();
        Assert.assertEquals(1, syncData.size());
        Assert.assertTrue(syncData.get(0).hasProblems());
        verify(backupManager, times(1)).postWebLog(BackupManager.webLogLevel.WARN,"Skipping as source not OK");
    }

    @Test
    public void sychronizeProblem3() {
        Source syncSource = mock(Source.class);
        when(syncSource.getStatus()).thenReturn(SourceStatusType.SST_OK);
        when(syncSource.getPath()).thenReturn("Source");

        Source syncDestination = mock(Source.class);
        when(syncDestination.getStatus()).thenReturn(null);
        when(syncDestination.getPath()).thenReturn("Destination");

        Synchronize synchronize = mock(Synchronize.class);
        when(synchronize.getSource()).thenReturn(syncSource);
        when(synchronize.getDestination()).thenReturn(syncDestination);
        List<Synchronize> synchronizeList = new ArrayList<>();
        synchronizeList.add(synchronize);

        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(associatedFileDataManager.internalFindAllSynchronize()).thenReturn(synchronizeList);

        BackupManager backupManager = mock(BackupManager.class);

        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager actionManager = mock(ActionManager.class);

        FileSystem fileSystem = mock(FileSystem.class);

        SynchronizeManager synchronizeManager = new SynchronizeManager(associatedFileDataManager,
                backupManager,
                fileSystemObjectManager,
                actionManager,
                fileSystem);

        List<SyncDataDTO> syncData = synchronizeManager.synchronize();
        Assert.assertEquals(1, syncData.size());
        Assert.assertTrue(syncData.get(0).hasProblems());
        verify(backupManager, times(1)).postWebLog(BackupManager.webLogLevel.WARN,"Skipping as destination not OK");
    }

    @Test
    public void sychronizeProblem4() {
        Source syncSource = mock(Source.class);
        when(syncSource.getStatus()).thenReturn(SourceStatusType.SST_OK);
        when(syncSource.getPath()).thenReturn("Source");

        Source syncDestination = mock(Source.class);
        when(syncDestination.getStatus()).thenReturn(SourceStatusType.SST_ERROR);
        when(syncDestination.getPath()).thenReturn("Destination");

        Synchronize synchronize = mock(Synchronize.class);
        when(synchronize.getSource()).thenReturn(syncSource);
        when(synchronize.getDestination()).thenReturn(syncDestination);
        List<Synchronize> synchronizeList = new ArrayList<>();
        synchronizeList.add(synchronize);

        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(associatedFileDataManager.internalFindAllSynchronize()).thenReturn(synchronizeList);

        BackupManager backupManager = mock(BackupManager.class);

        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager actionManager = mock(ActionManager.class);

        FileSystem fileSystem = mock(FileSystem.class);

        SynchronizeManager synchronizeManager = new SynchronizeManager(associatedFileDataManager,
                backupManager,
                fileSystemObjectManager,
                actionManager,
                fileSystem);

        List<SyncDataDTO> syncData = synchronizeManager.synchronize();
        Assert.assertEquals(1, syncData.size());
        Assert.assertTrue(syncData.get(0).hasProblems());
        verify(backupManager, times(1)).postWebLog(BackupManager.webLogLevel.WARN,"Skipping as destination not OK");
    }

    @Test
    public void sychronizeProblem5() {
        Source syncSource = mock(Source.class);
        when(syncSource.getStatus()).thenReturn(SourceStatusType.SST_OK);
        when(syncSource.getPath()).thenReturn("Source");

        Source syncDestination = mock(Source.class);
        when(syncDestination.getStatus()).thenReturn(SourceStatusType.SST_OK);
        when(syncDestination.getPath()).thenReturn("Destination");

        Synchronize synchronize = mock(Synchronize.class);
        when(synchronize.getSource()).thenReturn(syncSource);
        when(synchronize.getDestination()).thenReturn(syncDestination);
        List<Synchronize> synchronizeList = new ArrayList<>();
        synchronizeList.add(synchronize);

        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(associatedFileDataManager.internalFindAllSynchronize()).thenReturn(synchronizeList);

        BackupManager backupManager = mock(BackupManager.class);

        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);
        when(fileSystemObjectManager.createDbRoot(synchronize.getSource())).thenThrow(NullPointerException.class);

        ActionManager actionManager = mock(ActionManager.class);

        FileSystem fileSystem = mock(FileSystem.class);

        SynchronizeManager synchronizeManager = new SynchronizeManager(associatedFileDataManager,
                backupManager,
                fileSystemObjectManager,
                actionManager,
                fileSystem);

        List<SyncDataDTO> syncData = synchronizeManager.synchronize();
        Assert.assertEquals(1, syncData.size());
        Assert.assertTrue(syncData.get(0).hasProblems());
    }

    @Test
    public void testDriveManagerProblems() throws IOException {
        List<Source> sources = new ArrayList<>();
        Source source = mock(Source.class);
        when(source.getStatus()).thenReturn(null);
        when(source.getIdAndType()).thenReturn(new FileSystemObjectId(1,FileSystemObjectType.FSO_SOURCE));
        when(source.getPath()).thenReturn("Test");
        sources.add(source);

        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(associatedFileDataManager.internalFindAllSource()).thenReturn(sources);

        BackupManager backupManager = mock(BackupManager.class);

        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager actionManager = mock(ActionManager.class);
        List<ActionConfirm> deletes = new ArrayList<>();
        when(actionManager.findConfirmedDeletes()).thenReturn(deletes);

        FileSystem fileSystem = mock(FileSystem.class);
        doThrow(new IOException("Failed")).when(fileSystem).createDirectory(any(Path.class));

        DriveManager driveManager = new DriveManager(associatedFileDataManager,
                backupManager,
                actionManager,
                fileSystemObjectManager,
                fileSystem);

        List<GatherDataDTO> gatherData = driveManager.gather();
        Assert.assertEquals(1, gatherData.size());
        Assert.assertTrue(gatherData.get(0).hasProblems());
        verify(associatedFileDataManager, times(1)).updateSourceStatus(source,SourceStatusType.SST_ERROR);
    }

    @Test
    public void testDriveManagerProblems2() {
        List<Source> sources = new ArrayList<>();
        Source source = mock(Source.class);
        when(source.getStatus()).thenReturn(SourceStatusType.SST_GATHERING);
        when(source.getIdAndType()).thenReturn(new FileSystemObjectId(1,FileSystemObjectType.FSO_SOURCE));
        when(source.getPath()).thenReturn("Test");
        sources.add(source);

        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(associatedFileDataManager.internalFindAllSource()).thenReturn(sources);

        BackupManager backupManager = mock(BackupManager.class);

        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager actionManager = mock(ActionManager.class);
        List<ActionConfirm> deletes = new ArrayList<>();
        when(actionManager.findConfirmedDeletes()).thenReturn(deletes);

        FileSystem fileSystem = mock(FileSystem.class);

        DriveManager driveManager = new DriveManager(associatedFileDataManager,
                backupManager,
                actionManager,
                fileSystemObjectManager,
                fileSystem);

        List<GatherDataDTO> gatherData = driveManager.gather();
        Assert.assertEquals(0, gatherData.size());
    }
}
