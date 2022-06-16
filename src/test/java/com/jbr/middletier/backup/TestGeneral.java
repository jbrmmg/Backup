package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.DefaultProfileUtil;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.ApiError;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static com.jbr.middletier.backup.data.ClassificationActionType.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestGeneral extends WebTester {
    @Test
    public void TestDefaultProfile() {
        SpringApplication app = mock(SpringApplication.class);

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

        gatherDataDTO.setSourceId(2);
        Assert.assertEquals(2,gatherDataDTO.getSourceId());

        gatherDataDTO.setProblems();
        Assert.assertTrue(gatherDataDTO.getProblems());

        DirectoryInfo directoryInfo = new DirectoryInfo();
        directoryInfo.setRemoved();
        Assert.assertTrue(directoryInfo.getRemoved());

        SynchronizeDTO synchronizeDTO = new SynchronizeDTO(1);
        Assert.assertEquals((Integer) 1, synchronizeDTO.getId());

        SyncDataDTO syncDataDTO = new SyncDataDTO();
        syncDataDTO.incrementFilesDeleted();
        syncDataDTO.incrementDirectoriesDeleted();
        syncDataDTO.incrementFilesCopied();
        syncDataDTO.incrementDirectoriesCopied();
        Assert.assertEquals(1,syncDataDTO.getDirectoriesDeleted());
        Assert.assertEquals(1,syncDataDTO.getFilesDeleted());
        Assert.assertEquals(1,syncDataDTO.getDirectoriesCopied());
        Assert.assertEquals(1,syncDataDTO.getFilesCopied());

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
        String nullString = null;
        MD5 md5 = new MD5(nullString);
        Assert.assertFalse(md5.isSet());

        MD5 md5b = new MD5(md5);
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
    }

    @Test
    public void TestFsoId() {
        FileSystemObjectId fsoId = new FileSystemObjectId(1,FileSystemObjectType.FSO_FILE);
        FileSystemObjectId fsoId2 = new FileSystemObjectId(2,FileSystemObjectType.FSO_FILE);
        FileSystemObjectId fsoId3 = new FileSystemObjectId(1,FileSystemObjectType.FSO_DIRECTORY);
        FileSystemObjectId fsoId4 = new FileSystemObjectId(1,FileSystemObjectType.FSO_FILE);
        Assert.assertNotEquals(fsoId3.hashCode(),fsoId.hashCode());
        Assert.assertNotEquals(fsoId, fsoId2);
        Assert.assertNotEquals("2", fsoId);
        Assert.assertNotEquals(null, fsoId);
        Assert.assertNotEquals(fsoId, fsoId3);
        Assert.assertEquals(fsoId, fsoId);
        Assert.assertEquals(fsoId, fsoId4);
    }

    @Test
    public void TestHierarchyResponse() {
        HierarchyResponse hierarchyResponse = new HierarchyResponse();
        Assert.assertEquals(-1,hierarchyResponse.getId());
        Assert.assertEquals(0,hierarchyResponse.getLevel());
        Assert.assertEquals("",hierarchyResponse.getDisplayName());
        Assert.assertEquals("/",hierarchyResponse.getPath());
        Assert.assertEquals(true,hierarchyResponse.getDirectory());
    }
}
