package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.config.DefaultProfileUtil;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.ApiError;
import com.jbr.middletier.backup.manager.*;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.schedule.GatherSynchronizeCtrl;
import com.jbr.middletier.backup.util.DebugPhysicalNamingStrategyImpl;
import com.jbr.middletier.backup.util.FileSearch;
import com.jbr.middletier.backup.util.ImageSize;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import org.hibernate.boot.model.naming.Identifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static com.jbr.middletier.backup.data.ClassificationActionType.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = MiddleTier.class)
public class TestGeneral extends WebTester {
    @Autowired
    AssociatedFileDataManager associatedFileDataManager;

    @Autowired
    ActionManager actionManager;

    @Autowired
    FileSystemObjectManager fileSystemObjectManager;

    @Test
    void TestDefaultProfile() {
        SpringApplication app = mock(SpringApplication.class);

        assertNotNull(app);
        DefaultProfileUtil.addDefaultProfile(app);
    }

    @Test
    void TestClassificationActionType() {
        assertEquals(CA_BACKUP,ClassificationActionType.getClassificationActionType("BACKUP"));
        assertEquals(CA_DELETE,ClassificationActionType.getClassificationActionType("DELETE"));
        assertEquals(CA_FOLDER,ClassificationActionType.getClassificationActionType("FOLDER"));
        assertEquals(CA_IGNORE,ClassificationActionType.getClassificationActionType("IGNORE"));
        assertEquals(CA_WARN,ClassificationActionType.getClassificationActionType("WARN"));

        try {
            ClassificationActionType.getClassificationActionType("BLAH");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("BLAH is not a valid Classification Action", e.getMessage());
        }
    }

    @Test
    void TestDTOs() {
        GatherDataDTO gatherDataDTO = new GatherDataDTO(1);
        assertEquals(1,gatherDataDTO.getUnderlyingId());

        gatherDataDTO.setProblems();
        assertTrue(gatherDataDTO.hasProblems());

        SynchronizeDTO synchronizeDTO = new SynchronizeDTO();
        synchronizeDTO.setId(1);
        assertEquals((Integer) 1, synchronizeDTO.getId());

        SyncDataDTO syncDataDTO = new SyncDataDTO(1);
        syncDataDTO.increment(SyncDataDTO.SyncDataCountType.FILES_DELETED);
        syncDataDTO.increment(SyncDataDTO.SyncDataCountType.DIRECTORIES_DELETED);
        syncDataDTO.increment(SyncDataDTO.SyncDataCountType.FILES_COPIED);
        syncDataDTO.increment(SyncDataDTO.SyncDataCountType.DIRECTORIES_COPIED);
        assertEquals(1,syncDataDTO.getCount(SyncDataDTO.SyncDataCountType.FILES_DELETED));
        assertEquals(1,syncDataDTO.getCount(SyncDataDTO.SyncDataCountType.DIRECTORIES_DELETED));
        assertEquals(1,syncDataDTO.getCount(SyncDataDTO.SyncDataCountType.FILES_COPIED));
        assertEquals(1,syncDataDTO.getCount(SyncDataDTO.SyncDataCountType.DIRECTORIES_COPIED));

        OkStatus okStatus = new OkStatus();
        okStatus.setStatus("Test");
        assertEquals("Test", okStatus.getStatus());

        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setId(1);
        sourceDTO.setPath("Test");
        assertEquals((Integer)1,sourceDTO.getId());
        assertEquals("Test",sourceDTO.getPath());
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
        assertEquals((Integer)1,importSourceDTO.getDestinationId());

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
        assertEquals("IMPORT",actionConfirmDTO.getAction());
        assertEquals(1,actionConfirmDTO.getId());
        assertEquals("flag",actionConfirmDTO.getFlags());
        assertEquals("n",actionConfirmDTO.getParameter());
        assertFalse(actionConfirmDTO.getParameterRequired());
        assertFalse(actionConfirmDTO.getConfirmed());
        assertEquals("file",actionConfirmDTO.getFileName());
        assertEquals(1,actionConfirmDTO.getFileId());
    }

    @Test
    void TestActionConfirmType() {
        assertEquals(ActionConfirmType.AC_DELETE,ActionConfirmType.getActionConfirmType("DELETE"));
        assertEquals(ActionConfirmType.AC_DELETE_DUPLICATE,ActionConfirmType.getActionConfirmType("DELETE_DUP"));
        assertEquals(ActionConfirmType.AC_IMPORT,ActionConfirmType.getActionConfirmType("IMPORT"));

        try {
            ActionConfirmType.getActionConfirmType("BLAH");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("BLAH is not a valid Action Confirm type", e.getMessage());
        }
    }

    @Test
    void TestApiError() {
        Throwable ex = mock(Throwable.class);
        when(ex.getLocalizedMessage()).thenReturn("Error");

        ApiError test = new ApiError(null, "Blah", ex);

        assertNotNull(test.getTimestamp());
        assertEquals("Blah", test.getMessage());
        assertEquals("Error", test.getDebugMessage());
    }

    @Test
    void TestMD5() {
        // First check the invalid checks.
        try {
            new MD5((String) null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot create MD5 with null or empty string", e.getMessage());
        }

        try {
            new MD5("");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot create MD5 with null or empty string", e.getMessage());
        }

        try {
            new MD5("1234567890123456789012345678901");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("You must create MD5 with 32 characters", e.getMessage());
        }

        try {
            new MD5("1234567890123456789012345678901G");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("MD5 must only contain HEX digits (0-9 or A-F)", e.getMessage());
        }

        MD5 md5c = new MD5("12345678901234567890123456789012");

        MD5 md5d = new MD5("12345678901234567890123456789013");

        MD5 md5e = new MD5("12345678901234567890123456789012");

        assertEquals(md5c, md5e);
        assertEquals(md5e, md5c);
        assertNotEquals(md5c, md5d);
        assertNotEquals(md5d, md5c);
    }

    @Test
    void TestFsoId() {
        FileSystemObjectId fsoId = new FileSystemObjectId(1,FileSystemObjectType.FSO_FILE);
        FileSystemObjectId fsoId2 = new FileSystemObjectId(2,FileSystemObjectType.FSO_FILE);
        FileSystemObjectId fsoId3 = new FileSystemObjectId(1,FileSystemObjectType.FSO_DIRECTORY);
        FileSystemObjectId fsoId4 = new FileSystemObjectId(1,FileSystemObjectType.FSO_FILE);
        Object testObj = "not an id";
        assertNotEquals(fsoId3.hashCode(),fsoId.hashCode());
        assertNotEquals(fsoId, fsoId2);
        assertNotEquals(testObj, fsoId);
        assertNotEquals(null, fsoId);
        assertNotEquals(fsoId, fsoId3);
        assertEquals(fsoId, fsoId4);

        @SuppressWarnings("ConstantConditions")
        boolean test = fsoId.equals(null);
        //noinspection ConstantConditions
        assertFalse(test);

        //noinspection EqualsBetweenInconvertibleTypes
        test = fsoId.equals(testObj);
        assertFalse(test);
    }

    @Test
    void TestSourceStatus() {
        assertEquals(SourceStatusType.SST_OK, SourceStatusType.getSourceStatusType("OK"));
        assertEquals(SourceStatusType.SST_GATHERING, SourceStatusType.getSourceStatusType("GATHERING"));
        assertEquals(SourceStatusType.SST_ERROR, SourceStatusType.getSourceStatusType("ERROR"));

        try {
            SourceStatusType.getSourceStatusType("Blah");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Blah is not a valid Source Status type", e.getMessage());
        }
    }

    @Test
    void TestSynchronizeRecord() {
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

        assertEquals("Test -> Test", synchronize.toString());
    }

    @Test
    void TestHierarchyResponse() {
        HierarchyResponse hierarchyResponse = new HierarchyResponse();
        assertEquals(-1,hierarchyResponse.getId());
        assertEquals("",hierarchyResponse.getDisplayName());
        assertEquals("/",hierarchyResponse.getPath());
        assertTrue(hierarchyResponse.getDirectory());
    }

    @Test
    void TestLocationDTO() {
        Location location = new Location();
        location.setId(1);
        location.setCheckDuplicates(true);
        location.setName("Test");
        location.setSize("1G");

        LocationDTO locationDTO = associatedFileDataManager.convertToDTO(location);
        assertEquals("Test", locationDTO.getName());
    }

    @Test
    void TestSourceDTO() {
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
        assertNotNull(sourceDTO);
    }

    @Test
    void TestCronClass() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        when(applicationProperties.getGatherEnabled()).thenReturn(true);

        ActionManager mockActionManager = mock(ActionManager.class);

        DriveManager driveManager = mock(DriveManager.class);

        DuplicateManager duplicateManager = mock(DuplicateManager.class);

        SynchronizeManager synchronizeManager = mock(SynchronizeManager.class);

        DbLoggingManager dbLoggingManager = mock(DbLoggingManager.class);

        GatherSynchronizeCtrl gatherSynchronizeCtrl = new GatherSynchronizeCtrl(applicationProperties,
                mockActionManager,
                driveManager,
                duplicateManager,
                synchronizeManager,
                dbLoggingManager);

        gatherSynchronizeCtrl.gatherCron();
        verify(mockActionManager, times(1)).sendActionEmail();
        verify(driveManager, times(1)).gather(null);
        verify(duplicateManager, times(1)).duplicateCheck();
        verify(synchronizeManager, times(1)).synchronize(null);
    }

    @Test
    void TestCronClassFail() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        when(applicationProperties.getGatherEnabled()).thenReturn(true);

        ActionManager mockActionManager = mock(ActionManager.class);
        doThrow(new IllegalStateException()).when(mockActionManager).sendActionEmail();

        DriveManager driveManager = mock(DriveManager.class);

        DuplicateManager duplicateManager = mock(DuplicateManager.class);

        SynchronizeManager synchronizeManager = mock(SynchronizeManager.class);

        DbLoggingManager dbLoggingManager = mock(DbLoggingManager.class);

        GatherSynchronizeCtrl gatherSynchronizeCtrl = new GatherSynchronizeCtrl(applicationProperties,
                mockActionManager,
                driveManager,
                duplicateManager,
                synchronizeManager,
                dbLoggingManager);

        gatherSynchronizeCtrl.gatherCron();
        verify(mockActionManager, times(1)).sendActionEmail();
        verify(driveManager, times(0)).gather(null);
        verify(duplicateManager, times(0)).duplicateCheck();
        verify(synchronizeManager, times(0)).synchronize(null);
    }

    @Test
    void TestCronClassDisabled() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        when(applicationProperties.getGatherEnabled()).thenReturn(false);

        ActionManager mockActionManager = mock(ActionManager.class);
        doThrow(new IllegalStateException()).when(mockActionManager).sendActionEmail();

        DriveManager driveManager = mock(DriveManager.class);

        DuplicateManager duplicateManager = mock(DuplicateManager.class);

        SynchronizeManager synchronizeManager = mock(SynchronizeManager.class);

        DbLoggingManager dbLoggingManager = mock(DbLoggingManager.class);

        GatherSynchronizeCtrl gatherSynchronizeCtrl = new GatherSynchronizeCtrl(applicationProperties,
                mockActionManager,
                driveManager,
                duplicateManager,
                synchronizeManager,
                dbLoggingManager);

        gatherSynchronizeCtrl.gatherCron();
        verify(mockActionManager, times(0)).sendActionEmail();
        verify(driveManager, times(0)).gather(null);
        verify(duplicateManager, times(0)).duplicateCheck();
        verify(synchronizeManager, times(0)).synchronize(null);
    }

    @Test
    void validateFileStatusType() {
        ImportFileStatusType type = ImportFileStatusType.getFileStatusType("COMPLETE");
        assertEquals(ImportFileStatusType.IFS_COMPLETE, type);

        type = ImportFileStatusType.getFileStatusType("READ");
        assertEquals(ImportFileStatusType.IFS_READ, type);

        try {
            ImportFileStatusType.getFileStatusType("Blah");
        } catch (IllegalStateException e) {
            assertEquals("Blah is not a valid Import File Status", e.getMessage());
        }
    }

    @Test
    void synchronizeProblem1() {
        FileSystemObjectId idAndType = mock(FileSystemObjectId.class);
        when(idAndType.getId()).thenReturn(1);

        Source syncSource = mock(Source.class);
        when(syncSource.getStatus()).thenReturn(null);
        when(syncSource.getPath()).thenReturn("Source");
        when(syncSource.getIdAndType()).thenReturn(idAndType);

        Source syncDestination = mock(Source.class);
        when(syncDestination.getStatus()).thenReturn(null);
        when(syncDestination.getPath()).thenReturn("Destination");

        Synchronize synchronize = mock(Synchronize.class);
        when(synchronize.getSource()).thenReturn(syncSource);
        when(synchronize.getDestination()).thenReturn(syncDestination);
        List<Synchronize> synchronizeList = new ArrayList<>();
        synchronizeList.add(synchronize);

        AssociatedFileDataManager mockAssociatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(mockAssociatedFileDataManager.findAllSynchronize()).thenReturn(synchronizeList);

        DbLoggingManager dbLoggingManager = mock(DbLoggingManager.class);

        FileSystemObjectManager mockFileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager mockActionManager = mock(ActionManager.class);

        FileSystem fileSystem = mock(FileSystem.class);

        SynchronizeManager synchronizeManager = new SynchronizeManager(mockAssociatedFileDataManager,
                dbLoggingManager,
                mockFileSystemObjectManager,
                mockActionManager,
                fileSystem);

        List<SyncDataDTO> syncData = synchronizeManager.synchronize(null);
        assertEquals(1, syncData.size());
        assertTrue(syncData.get(0).hasProblems());
        verify(dbLoggingManager, times(1)).warn("Skipping as source not OK",1,null);
    }

    @Test
    void synchronizeProblem2() {
        FileSystemObjectId idAndType = mock(FileSystemObjectId.class);
        when(idAndType.getId()).thenReturn(3);

        Source syncSource = mock(Source.class);
        when(syncSource.getStatus()).thenReturn(SourceStatusType.SST_ERROR);
        when(syncSource.getPath()).thenReturn("Source");
        when(syncSource.getIdAndType()).thenReturn(idAndType);

        Source syncDestination = mock(Source.class);
        when(syncDestination.getStatus()).thenReturn(null);
        when(syncDestination.getPath()).thenReturn("Destination");

        Synchronize synchronize = mock(Synchronize.class);
        when(synchronize.getSource()).thenReturn(syncSource);
        when(synchronize.getDestination()).thenReturn(syncDestination);
        List<Synchronize> synchronizeList = new ArrayList<>();
        synchronizeList.add(synchronize);

        AssociatedFileDataManager mockAssociatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(mockAssociatedFileDataManager.findAllSynchronize()).thenReturn(synchronizeList);

        DbLoggingManager dbLoggingManager = mock(DbLoggingManager.class);

        FileSystemObjectManager mockFileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager mockActionManager = mock(ActionManager.class);

        FileSystem fileSystem = mock(FileSystem.class);

        SynchronizeManager synchronizeManager = new SynchronizeManager(mockAssociatedFileDataManager,
                dbLoggingManager,
                mockFileSystemObjectManager,
                mockActionManager,
                fileSystem);

        List<SyncDataDTO> syncData = synchronizeManager.synchronize(null);
        assertEquals(1, syncData.size());
        assertTrue(syncData.get(0).hasProblems());
        verify(dbLoggingManager, times(1)).warn("Skipping as source not OK",3,null);
    }

    @Test
    void synchronizeProblem3() {
        FileSystemObjectId idAndType = mock(FileSystemObjectId.class);
        when(idAndType.getId()).thenReturn(5);

        Source syncSource = mock(Source.class);
        when(syncSource.getStatus()).thenReturn(SourceStatusType.SST_OK);
        when(syncSource.getPath()).thenReturn("Source");
        when(syncSource.getIdAndType()).thenReturn(idAndType);

        Source syncDestination = mock(Source.class);
        when(syncDestination.getStatus()).thenReturn(null);
        when(syncDestination.getPath()).thenReturn("Destination");
        when(syncDestination.getIdAndType()).thenReturn(idAndType);

        Synchronize synchronize = mock(Synchronize.class);
        when(synchronize.getSource()).thenReturn(syncSource);
        when(synchronize.getDestination()).thenReturn(syncDestination);
        List<Synchronize> synchronizeList = new ArrayList<>();
        synchronizeList.add(synchronize);

        AssociatedFileDataManager mockAssociatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(mockAssociatedFileDataManager.findAllSynchronize()).thenReturn(synchronizeList);

        DbLoggingManager dbLoggingManager = mock(DbLoggingManager.class);

        FileSystemObjectManager mockFileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager mockActionManager = mock(ActionManager.class);

        FileSystem fileSystem = mock(FileSystem.class);

        SynchronizeManager synchronizeManager = new SynchronizeManager(mockAssociatedFileDataManager,
                dbLoggingManager,
                mockFileSystemObjectManager,
                mockActionManager,
                fileSystem);

        List<SyncDataDTO> syncData = synchronizeManager.synchronize(null);
        assertEquals(1, syncData.size());
        assertTrue(syncData.get(0).hasProblems());
        verify(dbLoggingManager, times(1)).warn("Skipping as destination not OK",5,null);
    }

    @Test
    void synchronizeProblem4() {
        FileSystemObjectId idAndType = mock(FileSystemObjectId.class);
        when(idAndType.getId()).thenReturn(6);

        Source syncSource = mock(Source.class);
        when(syncSource.getStatus()).thenReturn(SourceStatusType.SST_OK);
        when(syncSource.getPath()).thenReturn("Source");
        when(syncSource.getIdAndType()).thenReturn(idAndType);

        Source syncDestination = mock(Source.class);
        when(syncDestination.getStatus()).thenReturn(SourceStatusType.SST_ERROR);
        when(syncDestination.getPath()).thenReturn("Destination");
        when(syncDestination.getIdAndType()).thenReturn(idAndType);

        Synchronize synchronize = mock(Synchronize.class);
        when(synchronize.getSource()).thenReturn(syncSource);
        when(synchronize.getDestination()).thenReturn(syncDestination);
        List<Synchronize> synchronizeList = new ArrayList<>();
        synchronizeList.add(synchronize);

        AssociatedFileDataManager mockAssociatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(mockAssociatedFileDataManager.findAllSynchronize()).thenReturn(synchronizeList);

        DbLoggingManager dbLoggingManager = mock(DbLoggingManager.class);

        FileSystemObjectManager mockFileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager mockActionManager = mock(ActionManager.class);

        FileSystem fileSystem = mock(FileSystem.class);

        SynchronizeManager synchronizeManager = new SynchronizeManager(mockAssociatedFileDataManager,
                dbLoggingManager,
                mockFileSystemObjectManager,
                mockActionManager,
                fileSystem);

        List<SyncDataDTO> syncData = synchronizeManager.synchronize(null);
        assertEquals(1, syncData.size());
        assertTrue(syncData.get(0).hasProblems());
        verify(dbLoggingManager, times(1)).warn("Skipping as destination not OK",6,null);
    }

    @Test
    void synchronizeProblem5() {
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

        AssociatedFileDataManager mockAssociatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(mockAssociatedFileDataManager.findAllSynchronize()).thenReturn(synchronizeList);

        DbLoggingManager dbLoggingManager = mock(DbLoggingManager.class);

        FileSystemObjectManager mockFileSystemObjectManager = mock(FileSystemObjectManager.class);
        when(mockFileSystemObjectManager.createDbRoot(synchronize.getSource())).thenThrow(NullPointerException.class);

        ActionManager mockActionManager = mock(ActionManager.class);

        FileSystem fileSystem = mock(FileSystem.class);

        SynchronizeManager synchronizeManager = new SynchronizeManager(mockAssociatedFileDataManager,
                dbLoggingManager,
                mockFileSystemObjectManager,
                mockActionManager,
                fileSystem);

        List<SyncDataDTO> syncData = synchronizeManager.synchronize(null);
        assertEquals(1, syncData.size());
        assertTrue(syncData.get(0).hasProblems());
    }

    @Test
    void testDriveManagerProblems() throws IOException {
        List<Source> sources = new ArrayList<>();
        Source source = mock(Source.class);
        when(source.getStatus()).thenReturn(null);
        when(source.getIdAndType()).thenReturn(new FileSystemObjectId(1,FileSystemObjectType.FSO_SOURCE));
        when(source.getPath()).thenReturn("Test");
        when(source.getMountCheck()).thenReturn(Optional.empty());
        sources.add(source);

        AssociatedFileDataManager mockAssociatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(mockAssociatedFileDataManager.findAllSource()).thenReturn(sources);

        DbLoggingManager dbLoggingManager = mock(DbLoggingManager.class);

        FileSystemObjectManager mockFileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager mockActionManager = mock(ActionManager.class);
        List<ActionConfirm> deletes = new ArrayList<>();
        when(mockActionManager.findConfirmedDeletes()).thenReturn(deletes);

        FileSystem fileSystem = mock(FileSystem.class);
        when(fileSystem.validateMountCheck(Optional.empty())).thenReturn(true);
        doThrow(new IOException("Failed")).when(fileSystem).createDirectory(any(Path.class));

        DriveManager driveManager = new DriveManager(mockAssociatedFileDataManager,
                dbLoggingManager,
                mockActionManager,
                mockFileSystemObjectManager,
                fileSystem);

        List<GatherDataDTO> gatherData = driveManager.gather(null);
        assertEquals(1, gatherData.size());
        assertTrue(gatherData.get(0).hasProblems());
        verify(mockAssociatedFileDataManager, times(1)).updateSourceStatus(source,SourceStatusType.SST_ERROR);
    }

    @Test
    void testDriveManagerProblems2() {
        List<Source> sources = new ArrayList<>();
        Source source = mock(Source.class);
        when(source.getStatus()).thenReturn(SourceStatusType.SST_GATHERING);
        when(source.getIdAndType()).thenReturn(new FileSystemObjectId(1,FileSystemObjectType.FSO_SOURCE));
        when(source.getPath()).thenReturn("Test");
        sources.add(source);

        AssociatedFileDataManager mockAssociatedFileDataManager = mock(AssociatedFileDataManager.class);
        when(mockAssociatedFileDataManager.findAllSource()).thenReturn(sources);

        DbLoggingManager dbLoggingManager = mock(DbLoggingManager.class);

        FileSystemObjectManager mockFileSystemObjectManager = mock(FileSystemObjectManager.class);

        ActionManager mockActionManager = mock(ActionManager.class);
        List<ActionConfirm> deletes = new ArrayList<>();
        when(mockActionManager.findConfirmedDeletes()).thenReturn(deletes);

        FileSystem fileSystem = mock(FileSystem.class);

        DriveManager driveManager = new DriveManager(mockAssociatedFileDataManager,
                dbLoggingManager,
                mockActionManager,
                mockFileSystemObjectManager,
                fileSystem);

        List<GatherDataDTO> gatherData = driveManager.gather(null);
        assertEquals(1, gatherData.size());
        assertTrue(gatherData.get(0).hasProblems());
    }

    @Test
    void testLocationToDTO() {
        Location location = new Location();
        location.setId(1);
        location.setName("Test");
        location.setSize("1TB");
        location.setCheckDuplicates(true);

        LocationDTO locationDTO = associatedFileDataManager.convertToDTO(location);
        assertEquals(1, locationDTO.getId().intValue());
        assertEquals("Test", locationDTO.getName());
        assertEquals("1TB", locationDTO.getSize());
        assertEquals(true, locationDTO.getCheckDuplicates());

        location = new Location();
        location.setId(1);
        location.setName("Test");
        location.setSize("1TB");

        locationDTO = associatedFileDataManager.convertToDTO(location);
        assertEquals(1, locationDTO.getId().intValue());
        assertEquals("Test", locationDTO.getName());
        assertEquals("1TB", locationDTO.getSize());
        assertNull(locationDTO.getCheckDuplicates());
    }

    @Test
    void testLocationToEntity() {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setName("Test");
        locationDTO.setSize("1TB");
        locationDTO.setCheckDuplicates(false);

        Location location = associatedFileDataManager.convertToEntity(locationDTO);
        assertEquals(1, location.getId());
        assertEquals("Test", location.getName());
        assertEquals("1TB", location.getSize());
        assertEquals(false, location.getCheckDuplicates());

        locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setName("Test");
        locationDTO.setSize("1TB");
        locationDTO.setCheckDuplicates(true);

        location = associatedFileDataManager.convertToEntity(locationDTO);
        assertEquals(1, location.getId());
        assertEquals("Test", location.getName());
        assertEquals("1TB", location.getSize());
        assertEquals(true, location.getCheckDuplicates());
    }

    @Test
    void testClassificationToEntity() {
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setId(1);
        classificationDTO.setIsImage(true);
        classificationDTO.setIsVideo(true);
        classificationDTO.setAction(CA_DELETE);
        classificationDTO.setOrder(1);
        classificationDTO.setRegex("Blah");
        classificationDTO.setIcon("fred");

        Classification classification = associatedFileDataManager.convertToEntity(classificationDTO);
        assertEquals(1,classification.getId().intValue());
        assertTrue(classification.getIsVideo());
        assertTrue(classification.getIsImage());
        assertEquals(CA_DELETE, classification.getAction());
        assertEquals(1,classification.getOrder().intValue());
        assertEquals("Blah", classification.getRegex());
        assertEquals("fred", classification.getIcon());
    }

    @Test
    void testClassificationToDTO() {
        Classification classification = new Classification();
        classification.setId(1);
        classification.setIsImage(true);
        classification.setIsVideo(true);
        classification.setAction(CA_DELETE);
        classification.setOrder(1);
        classification.setRegex("Blah");
        classification.setIcon("fred");

        ClassificationDTO classificationDTO = associatedFileDataManager.convertToDTO(classification);
        assertEquals(1,classificationDTO.getId().intValue());
        assertTrue(classificationDTO.getIsVideo());
        assertTrue(classificationDTO.getIsImage());
        assertEquals(CA_DELETE, classificationDTO.getAction());
        assertEquals(1,classificationDTO.getOrder().intValue());
        assertEquals("Blah", classificationDTO.getRegex());
        assertEquals("fred", classificationDTO.getIcon());
    }

    @Test
    void testSourceEntity() {
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
        assertEquals(1,source.getIdAndType().getId().intValue());
        assertEquals("Cheese", source.getPath());
        assertEquals(SourceStatusType.SST_OK, source.getStatus());
        assertEquals(1,source.getLocation().getId());
        assertEquals("1TB", source.getLocation().getSize());
        assertEquals("Test", source.getLocation().getName());
        assertTrue(source.getMountCheck().isPresent());
        assertEquals("Check", source.getMountCheck().get().toString());
        assertEquals("Blah", source.getFilter());
    }

    @Test
    void testSourceDTO() {
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
        assertEquals(1, sourceDTO.getId().intValue());
        assertEquals("Cheese", sourceDTO.getPath());
        assertEquals("OK", sourceDTO.getStatus());
        assertEquals(1, sourceDTO.getLocation().getId().intValue());
        assertEquals("1TB", sourceDTO.getLocation().getSize());
        assertEquals("Test", sourceDTO.getLocation().getName());
        assertEquals("Check", sourceDTO.getMountCheck());
        assertEquals("Blah", sourceDTO.getFilter());
    }

    @Test
    void testImportSourceEntity() {
        ImportSourceDTO sourceDTO = getImportSourceDTO();

        ImportSource source = associatedFileDataManager.convertToEntity(sourceDTO);
        assertEquals(1,source.getIdAndType().getId().intValue());
        assertEquals("Cheese", source.getPath());
        assertEquals(SourceStatusType.SST_OK, source.getStatus());
        assertEquals(1,source.getLocation().getId());
        assertEquals("1TB", source.getLocation().getSize());
        assertEquals("Test", source.getLocation().getName());
        assertTrue(source.getMountCheck().isPresent());
        assertEquals("Check", source.getMountCheck().get().toString());
        assertEquals("Blah", source.getFilter());
        assertEquals(1, source.getDestination().getIdAndType().getId().intValue());
    }

    @NotNull
    private static ImportSourceDTO getImportSourceDTO() {
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
        return sourceDTO;
    }

    @Test
    void testImportSourceDTO() {
        ImportSource source = getImportSource();

        ImportSourceDTO sourceDTO = associatedFileDataManager.convertToDTO(source);
        assertEquals(1,sourceDTO.getId().intValue());
        assertEquals("Cheese", sourceDTO.getPath());
        assertEquals("OK", sourceDTO.getStatus());
        assertEquals(1,sourceDTO.getLocation().getId().intValue());
        assertEquals("1TB", sourceDTO.getLocation().getSize());
        assertEquals("Test", sourceDTO.getLocation().getName());
        assertEquals("Check", sourceDTO.getMountCheck());
        assertEquals("Blah", sourceDTO.getFilter());
        assertEquals(1, sourceDTO.getDestinationId().intValue());
    }

    @NotNull
    private static ImportSource getImportSource() {
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
        return source;
    }

    @Test
    void testPreImportSourceEntity() {
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
        assertEquals(1,source.getIdAndType().getId().intValue());
        assertEquals("Cheese", source.getPath());
        assertEquals(SourceStatusType.SST_OK, source.getStatus());
        assertEquals(1,source.getLocation().getId());
        assertEquals("1TB", source.getLocation().getSize());
        assertEquals("Test", source.getLocation().getName());
        assertTrue(source.getMountCheck().isPresent());
        assertEquals("Check", source.getMountCheck().get().toString());
        assertEquals("Blah", source.getFilter());
    }

    @Test
    void testPreImportSourceDTO() {
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
        assertEquals(1, sourceDTO.getId().intValue());
        assertEquals("Cheese", sourceDTO.getPath());
        assertEquals("OK", sourceDTO.getStatus());
        assertEquals(1, sourceDTO.getLocation().getId().intValue());
        assertEquals("1TB", sourceDTO.getLocation().getSize());
        assertEquals("Test", sourceDTO.getLocation().getName());
        assertEquals("Check", sourceDTO.getMountCheck());
        assertEquals("Blah", sourceDTO.getFilter());
    }

    @Test
    void testPostImportSourceEntity() {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setSize("1TB");
        locationDTO.setName("Test");
        locationDTO.setCheckDuplicates(true);
        PostImportSourceDTO sourceDTO = new PostImportSourceDTO();
        sourceDTO.setId(1);
        sourceDTO.setPath("Cheese");
        sourceDTO.setStatus("OK");
        sourceDTO.setLocation(locationDTO);
        sourceDTO.setMountCheck("Check");
        sourceDTO.setFilter("Blah");

        PostImportSource source = associatedFileDataManager.convertToEntity(sourceDTO);
        assertEquals(1,source.getIdAndType().getId().intValue());
        assertEquals("Cheese", source.getPath());
        assertEquals(SourceStatusType.SST_OK, source.getStatus());
        assertEquals(1,source.getLocation().getId());
        assertEquals("1TB", source.getLocation().getSize());
        assertEquals("Test", source.getLocation().getName());
        assertTrue(source.getMountCheck().isPresent());
        assertEquals("Check", source.getMountCheck().get().toString());
        assertEquals("Blah", source.getFilter());
    }

    @Test
    void testPostImportSourceDTO() {
        Location location = new Location();
        location.setId(1);
        location.setSize("1TB");
        location.setName("Test");
        location.setCheckDuplicates(true);
        PostImportSource source = new PostImportSource();
        source.setId(1);
        source.setPath("Cheese");
        source.setStatus(SourceStatusType.SST_OK);
        source.setLocation(location);
        source.setMountCheck("Check");
        source.setFilter("Blah");

        PostImportSourceDTO sourceDTO = associatedFileDataManager.convertToDTO(source);
        assertEquals(1, sourceDTO.getId().intValue());
        assertEquals("Cheese", sourceDTO.getPath());
        assertEquals("OK", sourceDTO.getStatus());
        assertEquals(1, sourceDTO.getLocation().getId().intValue());
        assertEquals("1TB", sourceDTO.getLocation().getSize());
        assertEquals("Test", sourceDTO.getLocation().getName());
        assertEquals("Check", sourceDTO.getMountCheck());
        assertEquals("Blah", sourceDTO.getFilter());
    }

    @Test
    void testSynchronizeEntity() {
        SynchronizeDTO synchronizeDTO = getSynchronizeDTO();

        Synchronize synchronize = associatedFileDataManager.convertToEntity(synchronizeDTO);
        assertEquals(1, synchronize.getId().intValue());
        assertEquals(1, synchronize.getSource().getIdAndType().getId().intValue());
        assertEquals(1, synchronize.getSource().getLocation().getId());
        assertEquals("1TB", synchronize.getSource().getLocation().getSize());
        assertEquals("Test", synchronize.getSource().getLocation().getName());
        assertTrue(synchronize.getSource().getLocation().getCheckDuplicates());
        assertEquals("notFilter", synchronize.getSource().getFilter());
        assertEquals(SourceStatusType.SST_OK, synchronize.getSource().getStatus());
        assertEquals("Side", synchronize.getSource().getPath());
        assertTrue(synchronize.getSource().getMountCheck().isPresent());
        assertEquals("Chis", synchronize.getSource().getMountCheck().get().toString());
        assertEquals(2, synchronize.getDestination().getIdAndType().getId().intValue());
        assertEquals(1, synchronize.getDestination().getLocation().getId());
        assertEquals("1TB", synchronize.getDestination().getLocation().getSize());
        assertEquals("Test", synchronize.getDestination().getLocation().getName());
        assertTrue(synchronize.getDestination().getLocation().getCheckDuplicates());
        assertEquals("filter", synchronize.getDestination().getFilter());
        assertEquals(SourceStatusType.SST_OK, synchronize.getDestination().getStatus());
        assertEquals("Foot", synchronize.getDestination().getPath());
        assertTrue(synchronize.getDestination().getMountCheck().isPresent());
        assertEquals("Check", synchronize.getDestination().getMountCheck().get().toString());
    }

    @NotNull
    private static SynchronizeDTO getSynchronizeDTO() {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1);
        locationDTO.setSize("1TB");
        locationDTO.setName("Test");
        locationDTO.setCheckDuplicates(true);
        SynchronizeDTO synchronizeDTO = new SynchronizeDTO();
        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setId(1);
        sourceDTO.setLocation(locationDTO);
        sourceDTO.setFilter("notFilter");
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
        return synchronizeDTO;
    }

    @Test
    void testSynchronizeDTO() {
        Location location = new Location();
        location.setId(1);
        location.setSize("1TB");
        location.setName("Test");
        location.setCheckDuplicates(true);
        Synchronize synchronize = new Synchronize();
        Source source = new Source();
        source.setId(1);
        source.setLocation(location);
        source.setFilter("notFilter");
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
        assertEquals(1, synchronizeDTO.getId().intValue());
        assertEquals(1, synchronizeDTO.getSource().getId().intValue());
        assertEquals(1, synchronizeDTO.getSource().getLocation().getId().intValue());
        assertEquals("1TB", synchronizeDTO.getSource().getLocation().getSize());
        assertEquals("Test", synchronizeDTO.getSource().getLocation().getName());
        assertTrue(synchronizeDTO.getSource().getLocation().getCheckDuplicates());
        assertEquals("notFilter", synchronizeDTO.getSource().getFilter());
        assertEquals("OK", synchronizeDTO.getSource().getStatus());
        assertEquals("Side", synchronizeDTO.getSource().getPath());
        assertEquals("Chis", synchronizeDTO.getSource().getMountCheck());
        assertEquals(2, synchronizeDTO.getDestination().getId().intValue());
        assertEquals(1, synchronizeDTO.getDestination().getLocation().getId().intValue());
        assertEquals("1TB", synchronizeDTO.getDestination().getLocation().getSize());
        assertEquals("Test", synchronizeDTO.getDestination().getLocation().getName());
        assertTrue(synchronizeDTO.getDestination().getLocation().getCheckDuplicates());
        assertEquals("filter", synchronizeDTO.getDestination().getFilter());
        assertEquals("OK", synchronizeDTO.getDestination().getStatus());
        assertEquals("Foot", synchronizeDTO.getDestination().getPath());
        assertEquals("Check", synchronizeDTO.getDestination().getMountCheck());
    }

    @Test
    void testFileInfoDto() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

        Classification classification = new Classification();
        classification.setId(1);
        classification.setIsImage(true);
        classification.setIsVideo(true);
        classification.setAction(CA_DELETE);
        classification.setOrder(1);
        classification.setRegex("Blah");
        classification.setIcon("fred");
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(1);
        fileInfo.setName("TestFile.txt");
        fileInfo.setSize(380);
        fileInfo.setMd5(new MD5("12345678901234567890123456789012"));
        fileInfo.setParent(null);
        fileInfo.setClassification(classification);
        fileInfo.setDate(LocalDateTime.parse("2022-02-27 22:23",formatter));
        fileInfo.setParentId(new FileSystemObjectId(2, FileSystemObjectType.FSO_DIRECTORY));
        LocalDateTime testDateTime = LocalDateTime.of(2023,12,3,3,15,2,0);
        fileInfo.setExpiry(testDateTime);

        FileInfoDTO fileInfoDTO = fileSystemObjectManager.convertToDTO(fileInfo);
        assertEquals("FILE", fileInfoDTO.getType());
        assertEquals("TestFile.txt", fileInfoDTO.getFilename());
        assertEquals(LocalDateTime.parse("2022-02-27 22:23",formatter), fileInfoDTO.getDate());
        assertEquals(380, fileInfoDTO.getSize().intValue());
        assertEquals("12345678901234567890123456789012", fileInfoDTO.getMd5());
        assertEquals(2, fileInfoDTO.getParentId().intValue());
        assertEquals("DIRY", fileInfoDTO.getParentType());
        assertEquals(testDateTime,fileInfoDTO.getExpiry());

        fileInfoDTO.setMd5(null);
        assertNull(fileInfoDTO.getMd5());
    }

    @Test
    void testActionConfirmDTO() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

        Classification classification = new Classification();
        classification.setId(3);
        classification.setIsImage(true);
        classification.setIsVideo(false);
        classification.setAction(CA_BACKUP);
        classification.setOrder(1);
        classification.setRegex("Blah");
        classification.setIcon("fred");
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(4);
        fileInfo.setName("TestFile2.txt");
        fileInfo.setSize(2423);
        fileInfo.setMd5(new MD5("12345678901234567890123456789012"));
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
        assertEquals("IMPORT", actionConfirmDTO.getAction());
        assertTrue(actionConfirmDTO.getConfirmed());
        assertEquals("T",actionConfirmDTO.getFlags());
        assertFalse(actionConfirmDTO.getParameterRequired());
        assertEquals("X",actionConfirmDTO.getParameter());
        assertTrue(actionConfirmDTO.isImage());
        assertFalse(actionConfirmDTO.isVideo());
        assertEquals(4, actionConfirmDTO.getFileId());
        assertEquals("TestFile2.txt", actionConfirmDTO.getFileName());
        assertEquals(2423, actionConfirmDTO.getSize().longValue());
        assertEquals("2022-02-27 22:23", formatter.format(actionConfirmDTO.getDate()));
    }

    @Test
    void testFileDTO() {
        Classification classification = new Classification();
        classification.setId(1);
        classification.setIsImage(true);
        classification.setIsVideo(false);
        classification.setIcon("icon");

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(1);
        fileInfo.setName("Test");
        fileInfo.setDate(LocalDateTime.now());
        fileInfo.setSize(10);
        fileInfo.setMd5(new MD5("12345678901234567890123456789012"));
        fileInfo.setClassification(classification);

        FileDTO fileDTO = new FileDTO(fileInfo,"full", "path", "location");
        fileDTO.setId(2);
        fileDTO.setName("Test Blah");
        fileDTO.setFullFilename("full name");
        fileDTO.setSize(11);
        fileDTO.setDate(LocalDateTime.now());
        fileDTO.setImage(false);
        fileDTO.setVideo(false);
        fileDTO.setIcon("icon");
        fileDTO.setPath("path field");
        fileDTO.setLocationName("location");

        assertEquals(2,fileDTO.getId());
        assertEquals("Test Blah",fileDTO.getName());
        assertEquals("full name",fileDTO.getFullFilename());
        assertEquals(11,fileDTO.getSize());
        assertFalse(fileDTO.isImage());
        assertFalse(fileDTO.isVideo());
        assertEquals("icon",fileDTO.getIcon());
        assertEquals("path field",fileDTO.getPath());
        assertEquals("location",fileDTO.getLocationName());
        assertTrue(fileDTO.getMd5Optional().isPresent());
        assertEquals("12345678901234567890123456789012",fileDTO.getMd5Optional().get().toString());

        fileInfo = new FileInfo();
        fileInfo.setId(1);
        fileInfo.setName("Test");
        fileInfo.setDate(LocalDateTime.now());
        fileInfo.setSize(10);
        fileInfo.setMd5(null);
        fileInfo.setClassification(classification);

        fileDTO = new FileDTO(fileInfo,"full", "path", "location");
        fileDTO.setId(2);
        fileDTO.setName("Test Blah");
        fileDTO.setFullFilename("full name");
        fileDTO.setSize(11);
        fileDTO.setDate(LocalDateTime.now());
        fileDTO.setImage(false);
        fileDTO.setVideo(false);
        fileDTO.setIcon("icon");
        fileDTO.setPath("path field");
        fileDTO.setLocationName("location");

        assertEquals(2,fileDTO.getId());
        assertEquals("Test Blah",fileDTO.getName());
        assertEquals("full name",fileDTO.getFullFilename());
        assertEquals(11,fileDTO.getSize());
        assertFalse(fileDTO.isImage());
        assertFalse(fileDTO.isVideo());
        assertEquals("icon",fileDTO.getIcon());
        assertEquals("path field",fileDTO.getPath());
        assertEquals("location",fileDTO.getLocationName());
        assertFalse(fileDTO.getMd5Optional().isPresent());

        fileDTO.setMd5(new MD5("12345678901234567890123456789012"));
        assertTrue(fileDTO.getMd5Optional().isPresent());
        assertEquals("12345678901234567890123456789012",fileDTO.getMd5Optional().get().toString());
    }

    @Test
    void testDbLog() {
        DbLog dbLog = new DbLog();
        dbLog.setId(1);
        dbLog.setDate(LocalDateTime.now());
        dbLog.setType(DbLogType.DLT_DEBUG);
        dbLog.setMessage("Test");

        assertEquals(1,dbLog.getId().intValue());
        assertEquals(DbLogType.DLT_DEBUG,dbLog.getType());
        assertEquals("Test",dbLog.getMessage());
    }

    @Test
    void testFileSystemImageData() {
        Map<String,String> metadata = getMetadata("2022:01:21 11:04:10");

        FileSystemImageData fileSystemImageData = new FileSystemImageData(metadata);
        assertTrue(fileSystemImageData.isValid());
        ImageSize size = fileSystemImageData.getImageSize();
        assertEquals(10,size.height());
        assertEquals(10,size.width());
        assertEquals("21-January-2022 11:04 image/jpeg",fileSystemImageData.toString());
    }

    @NotNull
    private static Map<String,String> getMetadata(String value) {
        Map<String,String> metadata = new HashMap<>();
        metadata.put("date/time original",value + "+00:00");
        metadata.put("image size","10x10");
        metadata.put("gps position","51 deg 27' 22.32\" N, 2 deg 37' 32.52\" W");
        if(value.trim().toLowerCase().startsWith("invalid")) {
            metadata.put("mime type", "something/else");
        } else {
            metadata.put("mime type", "image/jpeg");
        }

        return metadata;
    }

    @Test
    void testFileSystemImageDataInvalid() {
        Map<String,String> metadata = getMetadata("invalid");

        FileSystemImageData fileSystemImageData = new FileSystemImageData(metadata);
        assertFalse(fileSystemImageData.isValid());
    }

    @Test
    void testFileSystemImageDataMp4() {
        Map<String,String> metadata = new HashMap<>();
        metadata.put("creation date","2022:01:21 11:04:12");
        metadata.put("image size","10x10");
        metadata.put("gps position","51 deg 27' 22.32\" N, 2 deg 37' 32.52\" W");
        metadata.put("mime type", "video/mp4");


        FileSystemImageData fileSystemImageData = new FileSystemImageData(metadata);
        assertTrue(fileSystemImageData.isValid());
        assertEquals("21-January-2022 11:04 video/mp4",fileSystemImageData.toString());
    }

    @Test
    void testPhysicalNamingStrategy() {
        Identifier id = new Identifier("string", true);
        Identifier id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalCatalogName(id,null);
        assertEquals(id,id2);
        assertNotEquals(id.toString(),id2.toString());

        id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalCatalogName(null,null);
        assertNull(id2);

        id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalSchemaName(id,null);
        assertEquals(id,id2);
        assertNotEquals(id.toString(),id2.toString());

        id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalSchemaName(null,null);
        assertNull(id2);

        id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalTableName(id,null);
        assertEquals(id,id2);
        assertNotEquals(id.toString(),id2.toString());

        id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalTableName(null,null);
        assertNull(id2);

        id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalSequenceName(id,null);
        assertEquals(id,id2);
        assertNotEquals(id.toString(),id2.toString());

        id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalSequenceName(null,null);
        assertNull(id2);

        id = new Identifier("filter",false);
        id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalColumnName(id,null);
        assertEquals("`filter`",id2.toString());

        id = new Identifier("order",false);
        id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalColumnName(id,null);
        assertEquals("`order`",id2.toString());

        id = new Identifier("classificationid",false);
        id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalColumnName(id,null);
        assertEquals("classification_id",id2.toString());

        id = new Identifier("fileid",false);
        id2 = DebugPhysicalNamingStrategyImpl.INSTANCE.toPhysicalColumnName(id,null);
        assertEquals("file_id",id2.toString());
    }

    @Test
    void testFileExpiryDTO() {
        LocalDateTime testTime = LocalDateTime.of(2023,12,1,11,49,32,1);
        FileExpiryDTO expiry = new FileExpiryDTO();
        expiry.setId(209);
        expiry.setExpiry(testTime);
        assertEquals(209,(long)expiry.getId());
        assertEquals(testTime,expiry.getExpiry());
    }

    @Test
    void testFileLabel() {
        FileLabelId id = new FileLabelId();
        id.setLabelId(10);
        id.setFileId(390);
        assertEquals("390-10", id.toString());
        String idString = "390-10";
        assertEquals(idString.hashCode(),id.hashCode());
        //noinspection RedundantCast
        assertEquals(id, (Object)id);
        assertNotEquals(null, id);
        //noinspection EqualsBetweenInconvertibleTypes
        boolean check = id.equals(idString);
        assertFalse(check);

        FileLabelId id2 = new FileLabelId();
        id2.setLabelId(10);
        id2.setFileId(390);
        assertEquals(id,id2);

        assertEquals(10,(long)id2.getLabelId());
        assertEquals(390,(long)id2.getFileId());

        FileLabel label = new FileLabel();
        label.setId(id);
        assertEquals(id2,label.getId());

        FileLabelDTO fileLabelDTO = new FileLabelDTO();
        fileLabelDTO.setFileId(10);
        assertEquals(10,(long)fileLabelDTO.getFileId());
        assertEquals(0,fileLabelDTO.getLabels().size());

        LabelDTO labelDTO = new LabelDTO();
        labelDTO.setId(10);
        labelDTO.setName("here");
        assertEquals(10,(long)labelDTO.getId());
        assertEquals("here", labelDTO.getName());

    }

    @Test
    void testLabel() {
        LabelDTO label = new LabelDTO();
        label.setName("blah");
        label.setId(102);
        assertEquals("blah",label.getName());
        assertEquals(102,(long)label.getId());

        Label label2 = new Label();
        label2.setId(212);
        label2.setName("fred");
        assertEquals(212,(long)label2.getId());
        assertEquals("fred",label2.getName());
    }

    @Test
    void testSelectPrint() {
        SelectedPrintDTO print = new SelectedPrintDTO();
        print.setFileName("IMG.JPG");
        print.setSizeName("2x2");
        print.setBorder(false);
        print.setBlackWhite(false);
        print.setSizeId(12);
        print.setFileId(102);
        assertEquals("IMG.JPG",print.getFileName());
        assertEquals("2x2",print.getSizeName());
        assertFalse(print.getBorder());
        assertFalse(print.getBlackWhite());
        assertEquals(12,print.getSizeId());
        assertEquals(102,print.getFileId());
    }

    @Test
    void testPrintSize() {
        PrintSize size = new PrintSize();
        size.setId(10);
        size.setName("4x3");
        size.setHeight(4.0);
        size.setWidth(3.0);
        size.setPanoramic(true);
        size.setRetro(true);
        assertEquals(10,(long)size.getId());
        assertEquals("4x3",size.getName());
        assertEquals(4.0,size.getHeight(),0.01);
        assertEquals(3.0,size.getWidth(),0.01);
        assertTrue(size.getPanoramic());
        assertTrue(size.getRetro());

        PrintSizeDTO sizeDTO = new PrintSizeDTO();
        sizeDTO.setId(10);
        sizeDTO.setName("4x3");
        sizeDTO.setHeight(4.0);
        sizeDTO.setWidth(3.0);
        sizeDTO.setPanoramic(true);
        sizeDTO.setRetro(true);
        assertEquals(10,(long)sizeDTO.getId());
        assertEquals("4x3",sizeDTO.getName());
        assertEquals(4.0,sizeDTO.getHeight(),0.01);
        assertEquals(3.0,sizeDTO.getWidth(),0.01);
        assertTrue(sizeDTO.getPanoramic());
        assertTrue(sizeDTO.getRetro());
    }

    @Test
    void testPrintId() {
        PrintId id = new PrintId();
        id.setSizeId(21);
        id.setFileId(1);
        assertEquals(21,(long)id.getSizeId());
        assertEquals(1,(long)id.getFileId());
        assertEquals("1-21",id.toString());
    }

    @Test
    void testMetaData() {
        MetaData metaData = new MetaData();
        metaData.setId(10);
        metaData.setDuration(12.2);
        metaData.setImage(true);
        metaData.setVideo(false);
        metaData.setLatitude(10.2);
        metaData.setLongitude(22.2);
        metaData.setImageHeight(213);
        metaData.setImageWidth(214);
        metaData.setDate(LocalDateTime.of(2024,10,21,2, 30,12));
        assertEquals(10, (long)metaData.getId());
        assertEquals(12.2, metaData.getDuration(),0.01);
        assertTrue(metaData.getImage());
        assertFalse(metaData.getVideo());
        assertEquals(10.2, metaData.getLatitude(), 0.01);
        assertEquals(22.2, metaData.getLongitude(), 0.01);
        assertEquals(213, (long)metaData.getImageHeight());
        assertEquals(214, (long)metaData.getImageWidth());
        assertEquals(LocalDateTime.of(2024,10,21,2, 30,12), metaData.getDate());

        MetaDataDTO testDTO = new MetaDataDTO();
        testDTO.setDuration(12.2);
        testDTO.setImage(true);
        testDTO.setVideo(false);
        testDTO.setLatitude(10.2);
        testDTO.setLongitude(22.2);
        testDTO.setImageHeight(213);
        testDTO.setImageWidth(214);
        testDTO.setDate(LocalDateTime.of(2024,10,21,2, 30,12));
        assertEquals(12.2, testDTO.getDuration(),0.01);
        assertTrue(testDTO.getImage());
        assertFalse(testDTO.getVideo());
        assertEquals(10.2, testDTO.getLatitude(), 0.01);
        assertEquals(22.2, testDTO.getLongitude(), 0.01);
        assertEquals(213, (long)testDTO.getImageHeight());
        assertEquals(214, (long)testDTO.getImageWidth());
        assertEquals(LocalDateTime.of(2024,10,21,2, 30,12), testDTO.getDate());

        testDTO = new MetaDataDTO(metaData);
        assertEquals(12.2, testDTO.getDuration(),0.01);
        assertTrue(testDTO.getImage());
        assertFalse(testDTO.getVideo());
        assertEquals(10.2, testDTO.getLatitude(), 0.01);
        assertEquals(22.2, testDTO.getLongitude(), 0.01);
        assertEquals(213, (long)testDTO.getImageHeight());
        assertEquals(214, (long)testDTO.getImageWidth());
        assertEquals(LocalDateTime.of(2024,10,21,2, 30,12), testDTO.getDate());
    }

    @Test
    void testGetImageFromVideo() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.setVidToImageLocation("target/");
        applicationProperties.setVidToImageCommand("cp %%INPUT%% %%OUTPUT%%");

        FileSystem fileSystem = new FileSystem(null,applicationProperties);

        File testFile = new File("src/test/resources/synchronise/20171224_152453.mp4");
        File resultFile;

        try {
            resultFile = fileSystem.getImageFileFromVideoFile(testFile);
        } catch (Exception e) {
            fail();
            return;
        }

        // Check the name of the file.
        assertEquals("76be016232b7fd49a1151dd8f4b33d97.jpg", resultFile.getName());

        // File should be created at target/76be016232b7fd49a1151dd8f4b33d97.jpg
        if(!resultFile.exists()) {
            fail("File should have been created at target/76be016232b7fd49a1151dd8f4b33d97.jpg");
            return;
        }

        // Calling again should not require the command to run.
        applicationProperties.setVidToImageCommand("cpxnon %%INPUT%% %%OUTPUT%%");

        try {
            resultFile = fileSystem.getImageFileFromVideoFile(testFile);
        } catch (Exception e) {
            fail();
            return;
        }

        assertEquals("76be016232b7fd49a1151dd8f4b33d97.jpg", resultFile.getName());

        // Delete the file.
        try {
            Files.delete(resultFile.toPath());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    void testFileSearch() {
        FileSearch search = new FileSearch("76be016232b7fd49a1151dd8f4b33d97");
        assertEquals(FileSearch.SearchType.MD5,search.getSearchType());

        search = new FileSearch("2025-03-21 12:09:12");
        assertEquals(FileSearch.SearchType.DATETIME,search.getSearchType());
        assertEquals(LocalDateTime.of(2025,3,21,12,9,12),search.getDateTime());

        search = new FileSearch("2025-03-21 12:09");
        assertEquals(FileSearch.SearchType.DATETIME,search.getSearchType());

        search = new FileSearch("FilesSearch.jpg");
        assertEquals(FileSearch.SearchType.NAME,search.getSearchType());
        assertEquals("FilesSearch.jpg", search.getSearch());

        search = new FileSearch("1313");
        assertEquals(FileSearch.SearchType.SIZE,search.getSearchType());
    }

    @Test
    void testPreImportFileDTO() {
        PreImportFileDTO preImportFileDTO = new PreImportFileDTO(true);
        assertEquals(TrafficLightType.TL_UNKNOWN, preImportFileDTO.getStepStatus().getStepStatus(FileProcessingStepType.FPS_COPY_FILE_TO_IMPORT));
        assertTrue(preImportFileDTO.isStopMarker());

        preImportFileDTO = new PreImportFileDTO(false);
        assertEquals(TrafficLightType.TL_UNKNOWN, preImportFileDTO.getStepStatus().getStepStatus(FileProcessingStepType.FPS_COPY_FILE_TO_IMPORT));
        assertFalse(preImportFileDTO.isStopMarker());
        assertFalse(preImportFileDTO.isInDatabase());
        assertFalse(preImportFileDTO.isInImport());
        assertFalse(preImportFileDTO.isInPostImport());

        assertFalse(preImportFileDTO.updatedSince(LocalDateTime.of(2012,12,23,0,0,0)));

        preImportFileDTO.setUpdateTime(LocalDateTime.of(2012,12,23,0,0,0));
        assertFalse(preImportFileDTO.updatedSince(LocalDateTime.of(2012,12,23,0,0,0)));
        assertTrue(preImportFileDTO.updatedSince(LocalDateTime.of(2012,12,22,0,0,0)));

        preImportFileDTO.setImportMd5(null);
        assertFalse(preImportFileDTO.getImportMd5Optional().isPresent());

        preImportFileDTO.setImportMd5(new MD5("8D4F46976377897DFADF214D0526CF56"));
        assertTrue(preImportFileDTO.getImportMd5Optional().isPresent());
    }
}
