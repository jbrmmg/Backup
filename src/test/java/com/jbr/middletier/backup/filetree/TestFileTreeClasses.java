package com.jbr.middletier.backup.filetree;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.filetree.compare.DbTree;
import com.jbr.middletier.backup.filetree.compare.node.DbCompareNode;
import com.jbr.middletier.backup.filetree.compare.node.SectionNode;
import com.jbr.middletier.backup.filetree.database.*;
import com.jbr.middletier.backup.filetree.helpers.*;
import com.jbr.middletier.backup.filetree.realworld.RwRoot;
import com.jbr.middletier.backup.manager.FileSystem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
//import java.util.Date;
import java.util.List;

import static com.jbr.middletier.backup.filetree.database.DbNodeCompareResultType.*;
import static com.jbr.middletier.backup.filetree.database.DbNodeCompareResultType.DBC_NOT_EQUAL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestFileTreeClasses {
    @Autowired
    FileSystem fileSystem;

    @Test
    public void basicTestAdded() {
        BasicTestNode testNode = new BasicTestNode();
        Assert.assertTrue(testNode.test());
        Assert.assertEquals(FileTreeNode.CompareStatusType.ADDED, testNode.getStatus());
    }

    @Test
    public void basicTestUpdated() {
        BasicTestNode testNode = new BasicTestNode();

        testNode.test2();
        Assert.assertEquals(FileTreeNode.CompareStatusType.UPDATED, testNode.getStatus());
    }

    @Test
    public void basicTestEqual() {
        BasicTestNode testNode = new BasicTestNode();

        testNode.test3();
        Assert.assertEquals(FileTreeNode.CompareStatusType.EQUAL, testNode.getStatus());
    }

    @Test
    public void basicTestChild() {
        BasicTestNode testNode = new BasicTestNode();

        testNode.test4();
        Assert.assertEquals("Hello", testNode.getNamedChild("Hello").getName());
        Assert.assertNull(testNode.getNamedChild("Hello2"));
        Assert.assertEquals("Hello 0", testNode.getNamedChild("Hello").toString());
    }

    @Test
    public void basicRootTests() {
        BasicTestRootNode testRootNode = new BasicTestRootNode();
        Assert.assertEquals("Root: 0", testRootNode.toString());

        BasicRwFile testRwFile = new BasicRwFile();
        Assert.assertTrue(testRwFile.test());
        Assert.assertEquals("RW (file): test", testRwFile.toString());

        BasicRwDirectory testRwDirectory = new BasicRwDirectory();
        Assert.assertTrue(testRwDirectory.test());
        Assert.assertEquals("RW (dir): Test 0", testRwDirectory.toString());
    }

    @Test
    public void basicSectionTest() {
        try {
            new RwRoot("does not exist", fileSystem);
            Assert.fail();
        } catch (IOException e) {
            Assert.assertEquals("does not exist", e.getMessage());
        }

        BasicSection testRwDbSection = new BasicSection(SectionNode.SectionNodeType.FILE_FOR_INSERT);
        Assert.assertTrue(testRwDbSection.test());

        try {
            new BasicSection(null);
            Assert.fail();
        } catch (NullPointerException e) {
            Assert.assertEquals("Cannot initialise a Rw DB Section with null.", e.getMessage());
        }

        BasicDbFile testDbFile = new BasicDbFile();
        Assert.assertTrue(testDbFile.test());
    }

    @Test
    public void basicFileCompareTest() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

        FileInfo fileInfo = new FileInfo();
        fileInfo.setMD5(new MD5("MATCH"));
        fileInfo.setClassification(null);
        fileInfo.setName("Test");
        fileInfo.setSize(291);
        fileInfo.setDate(LocalDateTime.parse("2022-07-01 13:23:19",formatter));
        DbFile dbFile = new DbFile(null, fileInfo);
        Assert.assertNull(dbFile.getClassification());
        Assert.assertNotNull(dbFile.getFSO());

        Assert.assertEquals(DBC_EQUAL, dbFile.compare(dbFile));
        Assert.assertEquals(DBC_NOT_EQUAL, dbFile.compare(null));

        DbFile dbFile2 = new DbFile(null, fileInfo);
        Assert.assertEquals(DBC_EQUAL, dbFile.compare(dbFile2));

        FileInfo fileInfo2 = new FileInfo();
        fileInfo2.setMD5(new MD5("MATCH"));
        fileInfo2.setClassification(null);
        fileInfo2.setName("Test");
        fileInfo2.setSize(291);
        fileInfo2.setDate(LocalDateTime.parse("2022-07-01 13:23:09",formatter));

        dbFile2 = new DbFile(null, fileInfo2);
        Assert.assertEquals(DBC_EQUAL_EXCEPT_DATE, dbFile.compare(dbFile2));

        fileInfo2.setDate(LocalDateTime.parse("2022-07-01 13:23:19",formatter));
        Assert.assertEquals(DBC_EQUAL, dbFile.compare(dbFile2));

        fileInfo2.setMD5(new MD5("NOMATCH"));
        Assert.assertEquals(DBC_NOT_EQUAL, dbFile.compare(dbFile2));

        fileInfo2.setMD5(new MD5("MATCH"));
        fileInfo2.setName("Test2");
        Assert.assertEquals(DBC_NOT_EQUAL, dbFile.compare(dbFile2));

        fileInfo2.setName("Test");
        fileInfo2.setSize(293);
        Assert.assertEquals(DBC_NOT_EQUAL, dbFile.compare(dbFile2));

        fileInfo2.setSize(291);
        fileInfo2.setDate(LocalDateTime.parse("2022-07-01 13:23:09",formatter));
        fileInfo2.setMD5(new MD5("NOMATCH"));
        Assert.assertEquals(DBC_NOT_EQUAL, dbFile.compare(dbFile2));
    }

    private DbCompareNode testFileFile(ClassificationActionType actionType, DbNodeCompareResultType result) {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        Classification classification = mock(Classification.class);
        when(classification.getAction()).thenReturn(actionType);

        DbFile mockFile = mock(DbFile.class);
        DbFile mockFile2 = mock(DbFile.class);

        when(mockFile.getName()).thenReturn("test");
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.compare(mockFile2)).thenReturn(result);
        when(mockFile.getClassification()).thenReturn(actionType == null ? null : classification);
        when(mockFile.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        when(mockFile2.getName()).thenReturn("test1");
        when(mockFile2.isDirectory()).thenReturn(false);
        when(mockFile2.compare(mockFile)).thenReturn(result);
        when(mockFile2.getClassification()).thenReturn(actionType == null ? null : classification);
        when(mockFile2.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile2,"children",new ArrayList<>());


        return new DbCompareNode(null, mockFile, mockFile2);
    }

    @Test
    public void testFileFileEqualWarn() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_WARN, DBC_EQUAL);
        Assert.assertEquals("REMOVE WARN FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    public void testFileFileEqualIgnore() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_IGNORE, DBC_EQUAL);
        Assert.assertEquals("REMOVE IGNORE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    public void testFileFileEqualDelete() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_DELETE, DBC_EQUAL);
        Assert.assertEquals("REMOVE REMOVE_SOURCE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    public void testFileFileEqualFolder() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_FOLDER, DBC_EQUAL);
        Assert.assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.NONE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileFileEqualBackup() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_BACKUP, DBC_EQUAL);
        Assert.assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.NONE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileFileEqualNull() {
        DbCompareNode compare = testFileFile(null, DBC_EQUAL);
        Assert.assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.NONE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileFileExceptDateWarn() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_WARN, DBC_EQUAL_EXCEPT_DATE);
        Assert.assertEquals("REMOVE WARN FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    public void testFileFileExceptDateIgnore() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_IGNORE, DBC_EQUAL_EXCEPT_DATE);
        Assert.assertEquals("REMOVE IGNORE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    public void testFileFileExceptDateDelete() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_DELETE, DBC_EQUAL_EXCEPT_DATE);
        Assert.assertEquals("REMOVE REMOVE_SOURCE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    public void testFileFileExceptDateFolder() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_FOLDER, DBC_EQUAL_EXCEPT_DATE);
        Assert.assertEquals("COPY DATE_UPDATE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.DATE_UPDATE, compare.getSubActionType());
    }

    @Test
    public void testFileFileExceptDateBackup() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_BACKUP, DBC_EQUAL_EXCEPT_DATE);
        Assert.assertEquals("COPY DATE_UPDATE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.DATE_UPDATE, compare.getSubActionType());
    }

    @Test
    public void testFileFileExceptDateNull() {
        DbCompareNode compare = testFileFile(null, DBC_EQUAL_EXCEPT_DATE);
        Assert.assertEquals("COPY DATE_UPDATE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.DATE_UPDATE, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualWarn() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_WARN, DBC_NOT_EQUAL);
        Assert.assertEquals("REMOVE WARN FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualIgnore() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_IGNORE, DBC_NOT_EQUAL);
        Assert.assertEquals("REMOVE IGNORE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualDelete() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_DELETE, DBC_NOT_EQUAL);
        Assert.assertEquals("REMOVE REMOVE_SOURCE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualFolder() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_FOLDER, DBC_NOT_EQUAL);
        Assert.assertEquals("COPY NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualBackup() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_BACKUP, DBC_NOT_EQUAL);
        Assert.assertEquals("COPY NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualNull() {
        DbCompareNode compare = testFileFile(null, DBC_NOT_EQUAL);
        Assert.assertEquals("COPY NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    private DbCompareNode testFileDirectory(ClassificationActionType actionType) {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        Classification classification = mock(Classification.class);
        when(classification.getAction()).thenReturn(actionType);

        DbFile mockFile = mock(DbFile.class);
        DbDirectory mockDirectory = mock(DbDirectory.class);

        when(mockFile.getName()).thenReturn("test");
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.compare(mockDirectory)).thenReturn(DBC_NOT_EQUAL);
        when(mockFile.getClassification()).thenReturn(actionType == null ? null : classification);
        when(mockFile.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        when(mockDirectory.getName()).thenReturn("test1");
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.compare(mockFile)).thenReturn(DBC_NOT_EQUAL);
        when(mockDirectory.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory,"children",new ArrayList<>());


        return new DbCompareNode(null, mockFile, mockDirectory);
    }

    @Test
    public void testFileDirectoryWarn() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_WARN);
        Assert.assertEquals("REMOVE WARN FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    public void testFileDirectoryIgnore() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_IGNORE);
        Assert.assertEquals("REMOVE IGNORE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    public void testFileDirectoryDelete() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_DELETE);
        Assert.assertEquals("REMOVE REMOVE_SOURCE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    public void testFileDirectoryFolder() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_FOLDER);
        Assert.assertEquals("RECREATE_AS_FILE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.RECREATE_AS_FILE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileDirectoryBackup() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_BACKUP);
        Assert.assertEquals("RECREATE_AS_FILE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.RECREATE_AS_FILE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileDirectoryNull() {
        DbCompareNode compare = testFileDirectory(null);
        Assert.assertEquals("RECREATE_AS_FILE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.RECREATE_AS_FILE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    private DbCompareNode testFileNull(ClassificationActionType actionType) {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        Classification classification = mock(Classification.class);
        when(classification.getAction()).thenReturn(actionType);

        DbFile mockFile = mock(DbFile.class);

        when(mockFile.getName()).thenReturn("test");
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.compare(null)).thenReturn(DBC_NOT_EQUAL);
        when(mockFile.getClassification()).thenReturn(actionType == null ? null : classification);
        when(mockFile.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        return new DbCompareNode(null, true, mockFile);
    }

    @Test
    public void testFileNullWarn() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_WARN);
        Assert.assertEquals("COPY WARN FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    public void testFileNullIgnore() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_IGNORE);
        Assert.assertEquals("COPY IGNORE FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    public void testFileNullDelete() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_DELETE);
        Assert.assertEquals("COPY REMOVE_SOURCE FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    public void testFileNullFolder() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_FOLDER);
        Assert.assertEquals("COPY NONE FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileNullBackup() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_BACKUP);
        Assert.assertEquals("COPY NONE FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileNullNull() {
        DbCompareNode compare = testFileNull(null);
        Assert.assertEquals("COPY NONE FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testDirectoryDirectory() {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        DbDirectory mockDirectory = mock(DbDirectory.class);
        DbDirectory mockDirectory2 = mock(DbDirectory.class);

        when(mockDirectory.getName()).thenReturn("test");
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.compare(mockDirectory2)).thenReturn(DBC_EQUAL);
        when(mockDirectory.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory,"children",new ArrayList<>());

        when(mockDirectory2.getName()).thenReturn("test1");
        when(mockDirectory2.isDirectory()).thenReturn(true);
        when(mockDirectory2.compare(mockDirectory)).thenReturn(DBC_EQUAL);
        when(mockDirectory2.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory2,"children",new ArrayList<>());

        DbCompareNode compare = new DbCompareNode(null, mockDirectory, mockDirectory2);
        Assert.assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.NONE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testDirectoryNull() {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        DbDirectory mockDirectory = mock(DbDirectory.class);

        when(mockDirectory.getName()).thenReturn("test");
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.compare(null)).thenReturn(DBC_NOT_EQUAL);
        when(mockDirectory.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory,"children",new ArrayList<>());

        DbCompareNode compare = new DbCompareNode(null, true, mockDirectory);
        Assert.assertEquals("COPY NONE FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testDirectoryFile() {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        DbDirectory mockDirectory = mock(DbDirectory.class);
        DbFile mockFile = mock(DbFile.class);

        when(mockDirectory.getName()).thenReturn("test");
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.compare(mockFile)).thenReturn(DBC_NOT_EQUAL);
        when(mockDirectory.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory,"children",new ArrayList<>());

        when(mockFile.getName()).thenReturn("test1");
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.compare(mockDirectory)).thenReturn(DBC_NOT_EQUAL);
        when(mockFile.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        DbCompareNode compare = new DbCompareNode(null, mockDirectory, mockFile);
        Assert.assertEquals("RECREATE_AS_DIRECTORY NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.RECREATE_AS_DIRECTORY, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testNullFile() {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        DbFile mockFile = mock(DbFile.class);

        when(mockFile.getName()).thenReturn("test");
        when(mockFile.isDirectory()).thenReturn(true);
        when(mockFile.compare(null)).thenReturn(DBC_NOT_EQUAL);
        when(mockFile.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        DbCompareNode compare = new DbCompareNode(null, false, mockFile);
        Assert.assertEquals("REMOVE NONE FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testNullDirectory() {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        DbDirectory mockDirectory = mock(DbDirectory.class);

        when(mockDirectory.getName()).thenReturn("test");
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.compare(null)).thenReturn(DBC_NOT_EQUAL);
        when(mockDirectory.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory,"children",new ArrayList<>());

        DbCompareNode compare = new DbCompareNode(null, false, mockDirectory);
        Assert.assertEquals("REMOVE NONE FSO_FILE>1", compare.toString());
        Assert.assertNull(compare.getName());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void compareFileTest2() {
        Classification classification = mock(Classification.class);
        when(classification.getAction()).thenReturn(ClassificationActionType.CA_BACKUP);

        DbFile mockFile = mock(DbFile.class);
        when(mockFile.getName()).thenReturn("test");
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.compare(mockFile)).thenReturn(DBC_EQUAL);
        when(mockFile.getClassification()).thenReturn(classification);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        DbFile mockFile2 = mock(DbFile.class);
        when(mockFile2.getName()).thenReturn("test1");
        when(mockFile2.isDirectory()).thenReturn(false);
        when(mockFile2.compare(mockFile)).thenReturn(DBC_EQUAL);
        when(mockFile2.getClassification()).thenReturn(classification);
        ReflectionTestUtils.setField(mockFile2,"children",new ArrayList<>());

        DbFile mockFile3 = mock(DbFile.class);
        when(mockFile3.getName()).thenReturn("test2");
        when(mockFile3.isDirectory()).thenReturn(false);
        when(mockFile3.compare(mockFile)).thenReturn(DBC_EQUAL);
        when(mockFile3.getClassification()).thenReturn(classification);
        ReflectionTestUtils.setField(mockFile3,"children",new ArrayList<>());

        List<FileTreeNode> list1 = new ArrayList<>();
        list1.add(mockFile);

        List<FileTreeNode> list2 = new ArrayList<>();
        list2.add(mockFile);

        DbRoot mockSource = mock(DbRoot.class);
        ReflectionTestUtils.setField(mockSource,"children",list1);
        when(mockSource.getChildren()).thenReturn(list1);

        DbRoot mockDestination = mock(DbRoot.class);
        ReflectionTestUtils.setField(mockDestination,"children",list2);
        when(mockDestination.getChildren()).thenReturn(list2);

        DbTree test = new DbTree(mockSource, mockDestination);
        Assert.assertNull(test.getName());

        test.compare();
        Assert.assertEquals(FileTreeNode.CompareStatusType.EQUAL, test.getStatus());

        List<FileTreeNode> result = test.getOrderedNodeList();
        Assert.assertNotNull(result);
        Assert.assertEquals(4, result.size());

        when(mockFile.compare(mockFile)).thenReturn(DBC_NOT_EQUAL);
        list1.add(mockFile2);
        list2.add(mockFile3);

        test.compare();
        result = test.getOrderedNodeList();
        Assert.assertNotNull(result);
        Assert.assertEquals(7, result.size());
    }

    @Test
    public void dbTreeRemovedDirectory() {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.testDirectoryRemoved());
    }

    @Test
    public void dbTreeRemovedNoSourceFailure () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.testRemovedNoSourceFailure());
    }

    @Test
    public void dbTreeDirectoryAdded () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.testDirectoryAdded());
    }

    @Test
    public void dbTreeDeleteFileRemoveFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.deleteFileRemoveFile());
    }

    @Test
    public void dbTreeAddedNoDestinationFailure () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.testAddedNoDestinationFailure());
    }

    @Test
    public void dbTreeDeleteFileRecreateAsDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.deleteFileRecreateAsDirectory());
    }

    @Test
    public void dbTreeDeleteFileRemovedDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.deleteFileRemoveDirectory());
    }

    @Test
    public void dbTreeDeleteFileCopyFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.deleteFileCopyFile());
    }

    @Test
    public void dbTreeDeleteDirectoryRemoveDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.deleteDirectoryRemoveDirectory());
    }

    @Test
    public void dbTreeDeleteDirectoryCopyDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.deleteDirectoryCopyDirectory());
    }

    @Test
    public void dbTreeDeleteDirectoryRemoveFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.deleteDirectoryRemoveFile());
    }

    @Test
    public void dbTreeDeleteDirectoryRecreateAsFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.deleteDirectoryRecreateAsFile());
    }

    @Test
    public void dbTreeInsertDirectoryCopyDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.insertDirectoryCopyDirectory());
    }

    @Test
    public void dbTreeInsertDirectoryRecreateAsDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.insertDirectoryRecreateAsDirectory());
    }

    @Test
    public void dbTreeInsertDirectoryRemoveDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.insertDirectoryRemoveDirectory());
    }

    @Test
    public void dbTreeInsertDirectoryCopyFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.insertDirectoryCopyFile());
    }

    @Test
    public void dbTreeInsertFileRecreateAsFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.insertFileRecreateAsFile());
    }

    @Test
    public void dbTreeInsertFileCopyDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.insertFileCopyDirectory());
    }

    @Test
    public void dbTreeInsertFileRemoveFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.insertFileRemoveFile());
    }
}
