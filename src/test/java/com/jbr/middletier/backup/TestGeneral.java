package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.config.DefaultProfileUtil;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.ApiError;
import com.jbr.middletier.backup.manager.*;
import com.jbr.middletier.backup.schedule.GatherSynchronizeCtrl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.jbr.middletier.backup.data.ClassificationActionType.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestGeneral extends WebTester {
    @Autowired
    AssociatedFileDataManager associatedFileDataManager;

    @Autowired
    ActionManager actionManager;

    @Autowired
    FileSystemObjectManager fileSystemObjectManager;

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

        SynchronizeDTO synchronizeDTO = new SynchronizeDTO();
        synchronizeDTO.setId(1);
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

        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setId(1);
        sourceDTO.setPath("Test");
        Assert.assertEquals((Integer)1,sourceDTO.getId());
        Assert.assertEquals("Test",sourceDTO.getPath());
        sourceDTO.setLocation(new LocationDTO());
        sourceDTO.setStatus("OK");
        sourceDTO.setFilter("Test");
        sourceDTO.incrementFileCount();
        sourceDTO.incrementDirectoryCount();
        sourceDTO.increaseFileSize(10);
        sourceDTO.increaseFileSize(100);
        sourceDTO.increaseFileSize(60);

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
        ActionConfirmDTO actionConfirmDTO = new ActionConfirmDTO();
        actionConfirmDTO.setAction("IMPORT");
        actionConfirmDTO.setId(1);
        actionConfirmDTO.setFlags("flag");
        actionConfirmDTO.setParameter("n");
        actionConfirmDTO.setParameterRequired(false);
        actionConfirmDTO.setConfirmed(false);
        actionConfirmDTO.setFileName("file");
        actionConfirmDTO.setFileId(1);
        Assert.assertEquals("IMPORT",actionConfirmDTO.getAction());
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
        synchronizeDTO.getSource().setStatus("OK");
        synchronizeDTO.getDestination().setPath("Test");
        synchronizeDTO.getDestination().setLocation(new LocationDTO());
        synchronizeDTO.getDestination().getLocation().setId(1);
        synchronizeDTO.getDestination().getLocation().setName("Test");
        synchronizeDTO.getDestination().getLocation().setSize("1GB");
        synchronizeDTO.getDestination().setStatus("OK");

        Synchronize synchronize = associatedFileDataManager.convertToEntity(synchronizeDTO);

        Assert.assertEquals("Test -> Test", synchronize.toString());
    }

    @Test
    public void TestHierarchyResponse() {
        HierarchyResponse hierarchyResponse = new HierarchyResponse();
        Assert.assertEquals(-1,hierarchyResponse.getId());
        Assert.assertEquals("",hierarchyResponse.getDisplayName());
        Assert.assertEquals("/",hierarchyResponse.getPath());
        Assert.assertTrue(hierarchyResponse.getDirectory());
    }

    @Test
    public void TestLocationDTO() {
        Location location = new Location();
        location.setId(1);
        location.setCheckDuplicates(true);
        location.setName("Test");
        location.setSize("1G");

        LocationDTO locationDTO = associatedFileDataManager.convertToDTO(location);
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
        location.setCheckDuplicates(true);
        location.setName("Test");
        location.setSize("1G");

        source.setLocation(location);

        SourceDTO sourceDTO = associatedFileDataManager.convertToDTO(source);
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
        when(associatedFileDataManager.findAllSynchronize()).thenReturn(synchronizeList);

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
        when(associatedFileDataManager.findAllSynchronize()).thenReturn(synchronizeList);

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
        when(associatedFileDataManager.findAllSynchronize()).thenReturn(synchronizeList);

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
        when(associatedFileDataManager.findAllSynchronize()).thenReturn(synchronizeList);

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
        when(associatedFileDataManager.findAllSynchronize()).thenReturn(synchronizeList);

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
        when(source.getMountCheck()).thenReturn(Optional.empty());
        sources.add(source);

        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(associatedFileDataManager.findAllSource()).thenReturn(sources);

        BackupManager backupManager = mock(BackupManager.class);

        FileSystemObjectManager fileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager actionManager = mock(ActionManager.class);
        List<ActionConfirm> deletes = new ArrayList<>();
        when(actionManager.findConfirmedDeletes()).thenReturn(deletes);

        FileSystem fileSystem = mock(FileSystem.class);
        when(fileSystem.validateMountCheck(Optional.empty())).thenReturn(true);
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
        when(associatedFileDataManager.findAllSource()).thenReturn(sources);

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
        Assert.assertEquals(1, gatherData.size());
        Assert.assertTrue(gatherData.get(0).hasProblems());
    }

    @Test
    public void testLocationToDTO() {
        Location location = new Location();
        location.setId(1);
        location.setName("Test");
        location.setSize("1TB");
        location.setCheckDuplicates(true);

        LocationDTO locationDTO = associatedFileDataManager.convertToDTO(location);
        Assert.assertEquals(1, locationDTO.getId().intValue());
        Assert.assertEquals("Test", locationDTO.getName());
        Assert.assertEquals("1TB", locationDTO.getSize());
        Assert.assertEquals(true, locationDTO.getCheckDuplicates());

        location = new Location();
        location.setId(1);
        location.setName("Test");
        location.setSize("1TB");

        locationDTO = associatedFileDataManager.convertToDTO(location);
        Assert.assertEquals(1, locationDTO.getId().intValue());
        Assert.assertEquals("Test", locationDTO.getName());
        Assert.assertEquals("1TB", locationDTO.getSize());
        Assert.assertNull(locationDTO.getCheckDuplicates());
    }

    @Test
    public void testLocationToEntity() {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setName("Test");
        locationDTO.setSize("1TB");
        locationDTO.setCheckDuplicates(false);

        Location location = associatedFileDataManager.convertToEntity(locationDTO);
        Assert.assertEquals(1, location.getId());
        Assert.assertEquals("Test", location.getName());
        Assert.assertEquals("1TB", location.getSize());
        Assert.assertEquals(false, location.getCheckDuplicates());

        locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setName("Test");
        locationDTO.setSize("1TB");
        locationDTO.setCheckDuplicates(true);

        location = associatedFileDataManager.convertToEntity(locationDTO);
        Assert.assertEquals(1, location.getId());
        Assert.assertEquals("Test", location.getName());
        Assert.assertEquals("1TB", location.getSize());
        Assert.assertEquals(true, location.getCheckDuplicates());
    }

    @Test
    public void testClassificationToEntity() {
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setId(1);
        classificationDTO.setIsImage(true);
        classificationDTO.setIsVideo(true);
        classificationDTO.setAction(CA_DELETE);
        classificationDTO.setOrder(1);
        classificationDTO.setRegex("Blah");
        classificationDTO.setUseMD5(false);
        classificationDTO.setIcon("fred");

        Classification classification = associatedFileDataManager.convertToEntity(classificationDTO);
        Assert.assertEquals(1,classification.getId().intValue());
        Assert.assertTrue(classification.getIsVideo());
        Assert.assertTrue(classification.getIsImage());
        Assert.assertEquals(CA_DELETE, classification.getAction());
        Assert.assertEquals(1,classification.getOrder().intValue());
        Assert.assertEquals("Blah", classification.getRegex());
        Assert.assertFalse(classification.getUseMD5());
        Assert.assertEquals("fred", classification.getIcon());
    }

    @Test
    public void testClassificationToDTO() {
        Classification classification = new Classification();
        classification.setId(1);
        classification.setIsImage(true);
        classification.setIsVideo(true);
        classification.setAction(CA_DELETE);
        classification.setOrder(1);
        classification.setRegex("Blah");
        classification.setUseMD5(false);
        classification.setIcon("fred");

        ClassificationDTO classificationDTO = associatedFileDataManager.convertToDTO(classification);
        Assert.assertEquals(1,classificationDTO.getId().intValue());
        Assert.assertTrue(classificationDTO.getIsVideo());
        Assert.assertTrue(classificationDTO.getIsImage());
        Assert.assertEquals(CA_DELETE, classificationDTO.getAction());
        Assert.assertEquals(1,classificationDTO.getOrder().intValue());
        Assert.assertEquals("Blah", classificationDTO.getRegex());
        Assert.assertFalse(classificationDTO.getUseMD5());
        Assert.assertEquals("fred", classificationDTO.getIcon());
    }

    @Test
    public void testSourceEntity() {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setSize("1TB");
        locationDTO.setName("Test");
        locationDTO.setCheckDuplicates(true);
        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setId(1);
        sourceDTO.setPath("Cheese");
        sourceDTO.setStatus("OK");
        sourceDTO.setLocation(locationDTO);
        sourceDTO.setMountCheck("Check");
        sourceDTO.setFilter("Blah");

        Source source = associatedFileDataManager.convertToEntity(sourceDTO);
        Assert.assertEquals(1,source.getIdAndType().getId().intValue());
        Assert.assertEquals("Cheese", source.getPath());
        Assert.assertEquals(SourceStatusType.SST_OK, source.getStatus());
        Assert.assertEquals(1,source.getLocation().getId());
        Assert.assertEquals("1TB", source.getLocation().getSize());
        Assert.assertEquals("Test", source.getLocation().getName());
        Assert.assertTrue(source.getMountCheck().isPresent());
        Assert.assertEquals("Check", source.getMountCheck().get().toString());
        Assert.assertEquals("Blah", source.getFilter());
    }

    @Test
    public void testSourceDTO() {
        Location location = new Location();
        location.setId(1);
        location.setSize("1TB");
        location.setName("Test");
        location.setCheckDuplicates(true);
        Source source = new Source();
        source.setId(1);
        source.setPath("Cheese");
        source.setStatus(SourceStatusType.SST_OK);
        source.setLocation(location);
        source.setMountCheck("Check");
        source.setFilter("Blah");

        SourceDTO sourceDTO = associatedFileDataManager.convertToDTO(source);
        Assert.assertEquals(1, sourceDTO.getId().intValue());
        Assert.assertEquals("Cheese", sourceDTO.getPath());
        Assert.assertEquals("OK", sourceDTO.getStatus());
        Assert.assertEquals(1, sourceDTO.getLocation().getId().intValue());
        Assert.assertEquals("1TB", sourceDTO.getLocation().getSize());
        Assert.assertEquals("Test", sourceDTO.getLocation().getName());
        Assert.assertEquals("Check", sourceDTO.getMountCheck());
        Assert.assertEquals("Blah", sourceDTO.getFilter());
    }

    @Test
    public void testImportSourceEntity() {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setSize("1TB");
        locationDTO.setName("Test");
        locationDTO.setCheckDuplicates(true);
        ImportSourceDTO sourceDTO = new ImportSourceDTO();
        sourceDTO.setId(1);
        sourceDTO.setPath("Cheese");
        sourceDTO.setStatus("OK");
        sourceDTO.setLocation(locationDTO);
        sourceDTO.setMountCheck("Check");
        sourceDTO.setFilter("Blah");
        sourceDTO.setDestinationId(1);

        ImportSource source = associatedFileDataManager.convertToEntity(sourceDTO);
        Assert.assertEquals(1,source.getIdAndType().getId().intValue());
        Assert.assertEquals("Cheese", source.getPath());
        Assert.assertEquals(SourceStatusType.SST_OK, source.getStatus());
        Assert.assertEquals(1,source.getLocation().getId());
        Assert.assertEquals("1TB", source.getLocation().getSize());
        Assert.assertEquals("Test", source.getLocation().getName());
        Assert.assertTrue(source.getMountCheck().isPresent());
        Assert.assertEquals("Check", source.getMountCheck().get().toString());
        Assert.assertEquals("Blah", source.getFilter());
        Assert.assertEquals(1, source.getDestination().getIdAndType().getId().intValue());
    }

    @Test
    public void testImportSourceDTO() {
        Location location = new Location();
        location.setId(1);
        location.setSize("1TB");
        location.setName("Test");
        location.setCheckDuplicates(true);
        Source destination = new Source();
        destination.setId(1);
        ImportSource source = new ImportSource();
        source.setId(1);
        source.setPath("Cheese");
        source.setStatus(SourceStatusType.SST_OK);
        source.setLocation(location);
        source.setMountCheck("Check");
        source.setFilter("Blah");
        source.setDestination(destination);

        ImportSourceDTO sourceDTO = associatedFileDataManager.convertToDTO(source);
        Assert.assertEquals(1,sourceDTO.getId().intValue());
        Assert.assertEquals("Cheese", sourceDTO.getPath());
        Assert.assertEquals("OK", sourceDTO.getStatus());
        Assert.assertEquals(1,sourceDTO.getLocation().getId().intValue());
        Assert.assertEquals("1TB", sourceDTO.getLocation().getSize());
        Assert.assertEquals("Test", sourceDTO.getLocation().getName());
        Assert.assertEquals("Check", sourceDTO.getMountCheck());
        Assert.assertEquals("Blah", sourceDTO.getFilter());
        Assert.assertEquals(1, sourceDTO.getDestinationId().intValue());
    }

    @Test
    public void testPreImportSourceEntity() {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setSize("1TB");
        locationDTO.setName("Test");
        locationDTO.setCheckDuplicates(true);
        PreImportSourceDTO sourceDTO = new PreImportSourceDTO();
        sourceDTO.setId(1);
        sourceDTO.setPath("Cheese");
        sourceDTO.setStatus("OK");
        sourceDTO.setLocation(locationDTO);
        sourceDTO.setMountCheck("Check");
        sourceDTO.setFilter("Blah");

        PreImportSource source = associatedFileDataManager.convertToEntity(sourceDTO);
        Assert.assertEquals(1,source.getIdAndType().getId().intValue());
        Assert.assertEquals("Cheese", source.getPath());
        Assert.assertEquals(SourceStatusType.SST_OK, source.getStatus());
        Assert.assertEquals(1,source.getLocation().getId());
        Assert.assertEquals("1TB", source.getLocation().getSize());
        Assert.assertEquals("Test", source.getLocation().getName());
        Assert.assertTrue(source.getMountCheck().isPresent());
        Assert.assertEquals("Check", source.getMountCheck().get().toString());
        Assert.assertEquals("Blah", source.getFilter());
    }

    @Test
    public void testPreImportSourceDTO() {
        Location location = new Location();
        location.setId(1);
        location.setSize("1TB");
        location.setName("Test");
        location.setCheckDuplicates(true);
        PreImportSource source = new PreImportSource();
        source.setId(1);
        source.setPath("Cheese");
        source.setStatus(SourceStatusType.SST_OK);
        source.setLocation(location);
        source.setMountCheck("Check");
        source.setFilter("Blah");

        PreImportSourceDTO sourceDTO = associatedFileDataManager.convertToDTO(source);
        Assert.assertEquals(1, sourceDTO.getId().intValue());
        Assert.assertEquals("Cheese", sourceDTO.getPath());
        Assert.assertEquals("OK", sourceDTO.getStatus());
        Assert.assertEquals(1, sourceDTO.getLocation().getId().intValue());
        Assert.assertEquals("1TB", sourceDTO.getLocation().getSize());
        Assert.assertEquals("Test", sourceDTO.getLocation().getName());
        Assert.assertEquals("Check", sourceDTO.getMountCheck());
        Assert.assertEquals("Blah", sourceDTO.getFilter());
    }

    @Test
    public void testSynchronizeEntity() {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setSize("1TB");
        locationDTO.setName("Test");
        locationDTO.setCheckDuplicates(true);
        SynchronizeDTO synchronizeDTO = new SynchronizeDTO();
        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setId(1);
        sourceDTO.setLocation(locationDTO);
        sourceDTO.setFilter("retlif");
        sourceDTO.setStatus("OK");
        sourceDTO.setPath("Side");
        sourceDTO.setMountCheck("Chis");
        SourceDTO destinationDTO = new SourceDTO();
        destinationDTO.setId(2);
        destinationDTO.setLocation(locationDTO);
        destinationDTO.setFilter("filter");
        destinationDTO.setStatus("OK");
        destinationDTO.setPath("Foot");
        destinationDTO.setMountCheck("Check");
        synchronizeDTO.setId(1);
        synchronizeDTO.setSource(sourceDTO);
        synchronizeDTO.setDestination(destinationDTO);

        Synchronize synchronize = associatedFileDataManager.convertToEntity(synchronizeDTO);
        Assert.assertEquals(1, synchronize.getId().intValue());
        Assert.assertEquals(1, synchronize.getSource().getIdAndType().getId().intValue());
        Assert.assertEquals(1, synchronize.getSource().getLocation().getId());
        Assert.assertEquals("1TB", synchronize.getSource().getLocation().getSize());
        Assert.assertEquals("Test", synchronize.getSource().getLocation().getName());
        Assert.assertTrue(synchronize.getSource().getLocation().getCheckDuplicates());
        Assert.assertEquals("retlif", synchronize.getSource().getFilter());
        Assert.assertEquals(SourceStatusType.SST_OK, synchronize.getSource().getStatus());
        Assert.assertEquals("Side", synchronize.getSource().getPath());
        Assert.assertTrue(synchronize.getSource().getMountCheck().isPresent());
        Assert.assertEquals("Chis", synchronize.getSource().getMountCheck().get().toString());
        Assert.assertEquals(2, synchronize.getDestination().getIdAndType().getId().intValue());
        Assert.assertEquals(1, synchronize.getDestination().getLocation().getId());
        Assert.assertEquals("1TB", synchronize.getDestination().getLocation().getSize());
        Assert.assertEquals("Test", synchronize.getDestination().getLocation().getName());
        Assert.assertTrue(synchronize.getDestination().getLocation().getCheckDuplicates());
        Assert.assertEquals("filter", synchronize.getDestination().getFilter());
        Assert.assertEquals(SourceStatusType.SST_OK, synchronize.getDestination().getStatus());
        Assert.assertEquals("Foot", synchronize.getDestination().getPath());
        Assert.assertTrue(synchronize.getDestination().getMountCheck().isPresent());
        Assert.assertEquals("Check", synchronize.getDestination().getMountCheck().get().toString());
    }

    @Test
    public void testSynchronizeDTO() {
        Location location = new Location();
        location.setId(1);
        location.setSize("1TB");
        location.setName("Test");
        location.setCheckDuplicates(true);
        Synchronize synchronize = new Synchronize();
        Source source = new Source();
        source.setId(1);
        source.setLocation(location);
        source.setFilter("retlif");
        source.setStatus(SourceStatusType.SST_OK);
        source.setPath("Side");
        source.setMountCheck("Chis");
        Source destination = new Source();
        destination.setId(2);
        destination.setLocation(location);
        destination.setFilter("filter");
        destination.setStatus(SourceStatusType.SST_OK);
        destination.setPath("Foot");
        destination.setMountCheck("Check");
        synchronize.setId(1);
        synchronize.setSource(source);
        synchronize.setDestination(destination);

        SynchronizeDTO synchronizeDTO = associatedFileDataManager.convertToDTO(synchronize);
        Assert.assertEquals(1, synchronizeDTO.getId().intValue());
        Assert.assertEquals(1, synchronizeDTO.getSource().getId().intValue());
        Assert.assertEquals(1, synchronizeDTO.getSource().getLocation().getId().intValue());
        Assert.assertEquals("1TB", synchronizeDTO.getSource().getLocation().getSize());
        Assert.assertEquals("Test", synchronizeDTO.getSource().getLocation().getName());
        Assert.assertTrue(synchronizeDTO.getSource().getLocation().getCheckDuplicates());
        Assert.assertEquals("retlif", synchronizeDTO.getSource().getFilter());
        Assert.assertEquals("OK", synchronizeDTO.getSource().getStatus());
        Assert.assertEquals("Side", synchronizeDTO.getSource().getPath());
        Assert.assertEquals("Chis", synchronizeDTO.getSource().getMountCheck());
        Assert.assertEquals(2, synchronizeDTO.getDestination().getId().intValue());
        Assert.assertEquals(1, synchronizeDTO.getDestination().getLocation().getId().intValue());
        Assert.assertEquals("1TB", synchronizeDTO.getDestination().getLocation().getSize());
        Assert.assertEquals("Test", synchronizeDTO.getDestination().getLocation().getName());
        Assert.assertTrue(synchronizeDTO.getDestination().getLocation().getCheckDuplicates());
        Assert.assertEquals("filter", synchronizeDTO.getDestination().getFilter());
        Assert.assertEquals("OK", synchronizeDTO.getDestination().getStatus());
        Assert.assertEquals("Foot", synchronizeDTO.getDestination().getPath());
        Assert.assertEquals("Check", synchronizeDTO.getDestination().getMountCheck());
    }

    @Test
    public void testFileInfoDto() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

        Classification classification = new Classification();
        classification.setId(1);
        classification.setIsImage(true);
        classification.setIsVideo(true);
        classification.setAction(CA_DELETE);
        classification.setOrder(1);
        classification.setRegex("Blah");
        classification.setUseMD5(false);
        classification.setIcon("fred");
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(1);
        fileInfo.setName("TestFile.txt");
        fileInfo.setSize(380);
        fileInfo.setMD5(new MD5("XYZI"));
        fileInfo.setParent(null);
        fileInfo.setClassification(classification);
        fileInfo.setDate(LocalDateTime.parse("2022-02-27 22:23",formatter));
        fileInfo.setParentId(new FileSystemObjectId(2, FileSystemObjectType.FSO_DIRECTORY));

        FileInfoDTO fileInfoDTO = fileSystemObjectManager.convertToDTO(fileInfo);
        Assert.assertEquals("FILE", fileInfoDTO.getType());
        Assert.assertEquals("TestFile.txt", fileInfoDTO.getFilename());
        Assert.assertEquals(LocalDateTime.parse("2022-02-27 22:23",formatter), fileInfoDTO.getDate());
        Assert.assertEquals(380, fileInfoDTO.getSize().intValue());
        Assert.assertEquals("XYZI", fileInfoDTO.getMd5());
        Assert.assertEquals(2, fileInfoDTO.getParentId().intValue());
        Assert.assertEquals("DIRY", fileInfoDTO.getParentType());
    }

    @Test
    public void testActionConfirmDTO() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

        Classification classification = new Classification();
        classification.setId(3);
        classification.setIsImage(true);
        classification.setIsVideo(false);
        classification.setAction(CA_BACKUP);
        classification.setOrder(1);
        classification.setRegex("Blah");
        classification.setUseMD5(false);
        classification.setIcon("fred");
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(4);
        fileInfo.setName("TestFile2.txt");
        fileInfo.setSize(2423);
        fileInfo.setMD5(new MD5("VJODSF"));
        fileInfo.setParent(null);
        fileInfo.setClassification(classification);
        fileInfo.setDate(LocalDateTime.parse("2022-02-27 22:23",formatter));
        fileInfo.setParentId(new FileSystemObjectId(4, FileSystemObjectType.FSO_PRE_IMPORT_SOURCE));
        ActionConfirm actionConfirm = new ActionConfirm();
        actionConfirm.setAction(ActionConfirmType.AC_IMPORT);
        actionConfirm.setConfirmed(true);
        actionConfirm.setFlags("T");
        actionConfirm.setFileInfo(fileInfo);
        actionConfirm.setParameterRequired(false);
        actionConfirm.setParameter("X");

        ActionConfirmDTO actionConfirmDTO = actionManager.convertToDTO(actionConfirm);
        Assert.assertEquals("IMPORT", actionConfirmDTO.getAction());
        Assert.assertTrue(actionConfirmDTO.getConfirmed());
        Assert.assertEquals("T",actionConfirmDTO.getFlags());
        Assert.assertFalse(actionConfirmDTO.getParameterRequired());
        Assert.assertEquals("X",actionConfirmDTO.getParameter());
        Assert.assertTrue(actionConfirmDTO.getIsImage());
        Assert.assertFalse(actionConfirmDTO.getIsVideo());
        Assert.assertEquals(4, actionConfirmDTO.getFileId());
        Assert.assertEquals("TestFile2.txt", actionConfirmDTO.getFileName());
        Assert.assertEquals(2423, actionConfirmDTO.getFileSize().longValue());
        Assert.assertEquals("2022-02-27 22:23", formatter.format(actionConfirmDTO.getFileDate()));
    }
}
