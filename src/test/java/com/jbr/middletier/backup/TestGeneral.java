package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.config.DefaultProfileUtil;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.ApiError;
import com.jbr.middletier.backup.manager.BackupManager;
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
            Assert.assertEquals("Blah is not a valid Source Status Type", e.getMessage());
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
}
