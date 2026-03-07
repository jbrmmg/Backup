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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = MiddleTier.class)
public class TestFileTreeClasses {
    @Autowired
    FileSystem fileSystem;

    @Test
    public void basicTestAdded() {
        BasicTestNode testNode = new BasicTestNode();
        assertTrue(testNode.test());
        assertEquals(FileTreeNode.CompareStatusType.ADDED, testNode.getStatus());
    }

    @Test
    public void basicTestUpdated() {
        BasicTestNode testNode = new BasicTestNode();

        testNode.test2();
        assertEquals(FileTreeNode.CompareStatusType.UPDATED, testNode.getStatus());
    }

    @Test
    public void basicTestEqual() {
        BasicTestNode testNode = new BasicTestNode();

        testNode.test3();
        assertEquals(FileTreeNode.CompareStatusType.EQUAL, testNode.getStatus());
    }

    @Test
    public void basicTestChild() {
        BasicTestNode testNode = new BasicTestNode();

        testNode.test4();
        assertTrue(testNode.getNamedChild("Hello").isPresent());
        assertTrue(testNode.getNamedChild("Hello").get().getName().isPresent());
        assertEquals("Hello", testNode.getNamedChild("Hello").get().getName().get());
        assertFalse(testNode.getNamedChild("Hello2").isPresent());
        assertEquals("Hello 0", testNode.getNamedChild("Hello").get().toString());
    }

    @Test
    public void basicRootTests() {
        BasicTestRootNode testRootNode = new BasicTestRootNode();
        assertEquals("Root: 0", testRootNode.toString());

        BasicRwFile testRwFile = new BasicRwFile();
        assertTrue(testRwFile.test());
        assertEquals("RW (file): test", testRwFile.toString());

        BasicRwDirectory testRwDirectory = new BasicRwDirectory();
        assertTrue(testRwDirectory.test());
        assertEquals("RW (dir): Test 0", testRwDirectory.toString());
    }

    @Test
    public void basicSectionTest() {
        try {
            new RwRoot("does not exist", fileSystem);
            fail();
        } catch (IOException e) {
            assertEquals("does not exist", e.getMessage());
        }

        BasicSection testRwDbSection = new BasicSection(SectionNode.SectionNodeType.FILE_FOR_INSERT);
        testRwDbSection.test();

        try {
            new BasicSection(null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("Cannot initialise a Rw DB Section with null.", e.getMessage());
        }

        BasicDbFile testDbFile = new BasicDbFile();
        assertTrue(testDbFile.test());
    }

    @Test
    public void basicFileCompareTest() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

        FileInfo fileInfo = new FileInfo();
        fileInfo.setMd5(new MD5("12345678901234567890123456789012"));
        fileInfo.setClassification(null);
        fileInfo.setName("Test");
        fileInfo.setSize(291);
        fileInfo.setDate(LocalDateTime.parse("2022-07-01 13:23:19",formatter));
        DbFile dbFile = new DbFile(null, fileInfo);
        assertNull(dbFile.getClassification());
        assertNotNull(dbFile.getFSO());

        assertTrue(dbFile.compare(dbFile));
        assertFalse(dbFile.compare(null));

        DbFile dbFile2 = new DbFile(null, fileInfo);
        assertTrue(dbFile.compare(dbFile2));

        FileInfo fileInfo2 = new FileInfo();
        fileInfo2.setMd5(new MD5("12345678901234567890123456789012"));
        fileInfo2.setClassification(null);
        fileInfo2.setName("Test");
        fileInfo2.setSize(291);
        fileInfo2.setDate(LocalDateTime.parse("2022-07-01 13:23:09",formatter));

        dbFile2 = new DbFile(null, fileInfo2);
        assertTrue(dbFile.compare(dbFile2));

        fileInfo2.setDate(LocalDateTime.parse("2022-07-01 13:23:19",formatter));
        assertTrue(dbFile.compare(dbFile2));

        fileInfo2.setMd5(new MD5("C714A0B2E792EB102F706DC2424B0083"));
        assertFalse(dbFile.compare(dbFile2));

        fileInfo2.setMd5(new MD5("12345678901234567890123456789012"));
        fileInfo2.setName("Test2");
        assertFalse(dbFile.compare(dbFile2));

        fileInfo2.setName("Test");
        fileInfo2.setSize(293);
        assertFalse(dbFile.compare(dbFile2));

        fileInfo2.setSize(291);
        fileInfo2.setDate(LocalDateTime.parse("2022-07-01 13:23:09",formatter));
        fileInfo2.setMd5(new MD5("C714A0B2E792EB102F706DC2424B0083"));
        assertFalse(dbFile.compare(dbFile2));
    }

    private DbCompareNode testFileFile(ClassificationActionType actionType, boolean result) {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        Classification classification = mock(Classification.class);
        when(classification.getAction()).thenReturn(actionType);

        DbFile mockFile = mock(DbFile.class);
        DbFile mockFile2 = mock(DbFile.class);

        when(mockFile.getName()).thenReturn(Optional.of("test"));
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.compare(mockFile2)).thenReturn(result);
        when(mockFile.getClassification()).thenReturn(actionType == null ? null : classification);
        when(mockFile.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        when(mockFile2.getName()).thenReturn(Optional.of("test1"));
        when(mockFile2.isDirectory()).thenReturn(false);
        when(mockFile2.compare(mockFile)).thenReturn(result);
        when(mockFile2.getClassification()).thenReturn(actionType == null ? null : classification);
        when(mockFile2.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile2,"children",new ArrayList<>());


        return new DbCompareNode(null, mockFile, mockFile2);
    }

    @Test
    public void testFileFileEqualIgnore() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_IGNORE, true);
        assertEquals("REMOVE IGNORE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    public void testFileFileEqualDelete() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_DELETE, true);
        assertEquals("REMOVE REMOVE_SOURCE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    public void testFileFileEqualFolder() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_FOLDER, true);
        assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.NONE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileFileEqualBackup() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_BACKUP, true);
        assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.NONE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileFileEqualNull() {
        DbCompareNode compare = testFileFile(null, true);
        assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.NONE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileFileEqualWarn() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_WARN, true);
        assertEquals("REMOVE WARN FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualWarn() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_WARN, false);
        assertEquals("REMOVE WARN FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualIgnore() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_IGNORE, false);
        assertEquals("REMOVE IGNORE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualDelete() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_DELETE, false);
        assertEquals("REMOVE REMOVE_SOURCE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualFolder() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_FOLDER, false);
        assertEquals("COPY NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualBackup() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_BACKUP, false);
        assertEquals("COPY NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileFileNotEqualNull() {
        DbCompareNode compare = testFileFile(null, false);
        assertEquals("COPY NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    private DbCompareNode testFileDirectory(ClassificationActionType actionType) {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        Classification classification = mock(Classification.class);
        when(classification.getAction()).thenReturn(actionType);

        DbFile mockFile = mock(DbFile.class);
        DbDirectory mockDirectory = mock(DbDirectory.class);

        when(mockFile.getName()).thenReturn(Optional.of("test"));
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.compare(mockDirectory)).thenReturn(false);
        when(mockFile.getClassification()).thenReturn(actionType == null ? null : classification);
        when(mockFile.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        when(mockDirectory.getName()).thenReturn(Optional.of("test1"));
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.compare(mockFile)).thenReturn(false);
        when(mockDirectory.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory,"children",new ArrayList<>());


        return new DbCompareNode(null, mockFile, mockDirectory);
    }

    @Test
    public void testFileDirectoryWarn() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_WARN);
        assertEquals("REMOVE WARN FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    public void testFileDirectoryIgnore() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_IGNORE);
        assertEquals("REMOVE IGNORE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    public void testFileDirectoryDelete() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_DELETE);
        assertEquals("REMOVE REMOVE_SOURCE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    public void testFileDirectoryFolder() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_FOLDER);
        assertEquals("RECREATE_AS_FILE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.RECREATE_AS_FILE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileDirectoryBackup() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_BACKUP);
        assertEquals("RECREATE_AS_FILE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.RECREATE_AS_FILE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileDirectoryNull() {
        DbCompareNode compare = testFileDirectory(null);
        assertEquals("RECREATE_AS_FILE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.RECREATE_AS_FILE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    private DbCompareNode testFileNull(ClassificationActionType actionType) {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        Classification classification = mock(Classification.class);
        when(classification.getAction()).thenReturn(actionType);

        DbFile mockFile = mock(DbFile.class);

        when(mockFile.getName()).thenReturn(Optional.of("test"));
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.compare(null)).thenReturn(false);
        when(mockFile.getClassification()).thenReturn(actionType == null ? null : classification);
        when(mockFile.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        return new DbCompareNode(null, true, mockFile);
    }

    @Test
    public void testFileNullWarn() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_WARN);
        assertEquals("COPY WARN FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    public void testFileNullIgnore() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_IGNORE);
        assertEquals("COPY IGNORE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    public void testFileNullDelete() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_DELETE);
        assertEquals("COPY REMOVE_SOURCE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    public void testFileNullFolder() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_FOLDER);
        assertEquals("COPY NONE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileNullBackup() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_BACKUP);
        assertEquals("COPY NONE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testFileNullNull() {
        DbCompareNode compare = testFileNull(null);
        assertEquals("COPY NONE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testDirectoryDirectory() {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        DbDirectory mockDirectory = mock(DbDirectory.class);
        DbDirectory mockDirectory2 = mock(DbDirectory.class);

        when(mockDirectory.getName()).thenReturn(Optional.of("test"));
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.compare(mockDirectory2)).thenReturn(true);
        when(mockDirectory.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory,"children",new ArrayList<>());

        when(mockDirectory2.getName()).thenReturn(Optional.of("test1"));
        when(mockDirectory2.isDirectory()).thenReturn(true);
        when(mockDirectory2.compare(mockDirectory)).thenReturn(true);
        when(mockDirectory2.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory2,"children",new ArrayList<>());

        DbCompareNode compare = new DbCompareNode(null, mockDirectory, mockDirectory2);
        assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.NONE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testDirectoryNull() {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        DbDirectory mockDirectory = mock(DbDirectory.class);

        when(mockDirectory.getName()).thenReturn(Optional.of("test"));
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.compare(null)).thenReturn(false);
        when(mockDirectory.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory,"children",new ArrayList<>());

        DbCompareNode compare = new DbCompareNode(null, true, mockDirectory);
        assertEquals("COPY NONE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testDirectoryFile() {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        DbDirectory mockDirectory = mock(DbDirectory.class);
        DbFile mockFile = mock(DbFile.class);

        when(mockDirectory.getName()).thenReturn(Optional.of("test"));
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.compare(mockFile)).thenReturn(false);
        when(mockDirectory.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory,"children",new ArrayList<>());

        when(mockFile.getName()).thenReturn(Optional.of("test1"));
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.compare(mockDirectory)).thenReturn(false);
        when(mockFile.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        DbCompareNode compare = new DbCompareNode(null, mockDirectory, mockFile);
        assertEquals("RECREATE_AS_DIRECTORY NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.RECREATE_AS_DIRECTORY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testNullFile() {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        DbFile mockFile = mock(DbFile.class);

        when(mockFile.getName()).thenReturn(Optional.of("test"));
        when(mockFile.isDirectory()).thenReturn(true);
        when(mockFile.compare(null)).thenReturn(false);
        when(mockFile.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        DbCompareNode compare = new DbCompareNode(null, false, mockFile);
        assertEquals("REMOVE NONE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void testNullDirectory() {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        DbDirectory mockDirectory = mock(DbDirectory.class);

        when(mockDirectory.getName()).thenReturn(Optional.of("test"));
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.compare(null)).thenReturn(false);
        when(mockDirectory.getFSO()).thenReturn(mockFSO);
        ReflectionTestUtils.setField(mockDirectory,"children",new ArrayList<>());

        DbCompareNode compare = new DbCompareNode(null, false, mockDirectory);
        assertEquals("REMOVE NONE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    public void compareFileTest2() {
        Classification classification = mock(Classification.class);
        when(classification.getAction()).thenReturn(ClassificationActionType.CA_BACKUP);

        DbFile mockFile = mock(DbFile.class);
        when(mockFile.getName()).thenReturn(Optional.of("test"));
        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.compare(mockFile)).thenReturn(true);
        when(mockFile.getClassification()).thenReturn(classification);
        ReflectionTestUtils.setField(mockFile,"children",new ArrayList<>());

        DbFile mockFile2 = mock(DbFile.class);
        when(mockFile2.getName()).thenReturn(Optional.of("test1"));
        when(mockFile2.isDirectory()).thenReturn(false);
        when(mockFile2.compare(mockFile)).thenReturn(true);
        when(mockFile2.getClassification()).thenReturn(classification);
        ReflectionTestUtils.setField(mockFile2,"children",new ArrayList<>());

        DbFile mockFile3 = mock(DbFile.class);
        when(mockFile3.getName()).thenReturn(Optional.of("test2"));
        when(mockFile3.isDirectory()).thenReturn(false);
        when(mockFile3.compare(mockFile)).thenReturn(true);
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
        assertFalse(test.getName().isPresent());

        test.compare();
        assertEquals(FileTreeNode.CompareStatusType.EQUAL, test.getStatus());

        List<FileTreeNode> result = test.getOrderedNodeList();
        assertNotNull(result);
        assertEquals(4, result.size());

        when(mockFile.compare(mockFile)).thenReturn(false);
        list1.add(mockFile2);
        list2.add(mockFile3);

        test.compare();
        result = test.getOrderedNodeList();
        assertNotNull(result);
        assertEquals(7, result.size());
    }

    @Test
    public void dbTreeRemovedDirectory() {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.testDirectoryRemoved());
    }

    @Test
    public void dbTreeRemovedNoSourceFailure () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.testRemovedNoSourceFailure());
    }

    @Test
    public void dbTreeDirectoryAdded () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.testDirectoryAdded());
    }

    @Test
    public void dbTreeDeleteFileRemoveFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteFileRemoveFile());
    }

    @Test
    public void dbTreeAddedNoDestinationFailure () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.testAddedNoDestinationFailure());
    }

    @Test
    public void dbTreeDeleteFileRecreateAsDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteFileRecreateAsDirectory());
    }

    @Test
    public void dbTreeDeleteFileRemovedDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteFileRemoveDirectory());
    }

    @Test
    public void dbTreeDeleteFileCopyFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteFileCopyFile());
    }

    @Test
    public void dbTreeDeleteDirectoryRemoveDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteDirectoryRemoveDirectory());
    }

    @Test
    public void dbTreeDeleteDirectoryCopyDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteDirectoryCopyDirectory());
    }

    @Test
    public void dbTreeDeleteDirectoryRemoveFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteDirectoryRemoveFile());
    }

    @Test
    public void dbTreeDeleteDirectoryRecreateAsFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteDirectoryRecreateAsFile());
    }

    @Test
    public void dbTreeInsertDirectoryCopyDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertDirectoryCopyDirectory());
    }

    @Test
    public void dbTreeInsertDirectoryRecreateAsDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertDirectoryRecreateAsDirectory());
    }

    @Test
    public void dbTreeInsertDirectoryRemoveDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertDirectoryRemoveDirectory());
    }

    @Test
    public void dbTreeInsertDirectoryCopyFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertDirectoryCopyFile());
    }

    @Test
    public void dbTreeInsertFileRecreateAsFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertFileRecreateAsFile());
    }

    @Test
    public void dbTreeInsertFileCopyDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertFileCopyDirectory());
    }

    @Test
    public void dbTreeInsertFileRemoveFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertFileRemoveFile());
    }
}
