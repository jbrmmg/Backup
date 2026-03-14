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
class TestFileTreeClasses {
    @Autowired
    FileSystem fileSystem;

    @Test
    void basicTestAdded() {
        BasicTestNode testNode = new BasicTestNode();
        assertTrue(testNode.test());
        assertEquals(FileTreeNode.CompareStatusType.ADDED, testNode.getStatus());
    }

    @Test
    void basicTestUpdated() {
        BasicTestNode testNode = new BasicTestNode();

        testNode.test2();
        assertEquals(FileTreeNode.CompareStatusType.UPDATED, testNode.getStatus());
    }

    @Test
    void basicTestEqual() {
        BasicTestNode testNode = new BasicTestNode();

        testNode.test3();
        assertEquals(FileTreeNode.CompareStatusType.EQUAL, testNode.getStatus());
    }

    @Test
    void basicTestChild() {
        BasicTestNode testNode = new BasicTestNode();

        testNode.test4();
        assertTrue(testNode.getNamedChild("Hello").isPresent());
        assertTrue(testNode.getNamedChild("Hello").get().getName().isPresent());
        assertEquals("Hello", testNode.getNamedChild("Hello").get().getName().get());
        assertFalse(testNode.getNamedChild("Hello2").isPresent());
        assertEquals("Hello 0", testNode.getNamedChild("Hello").get().toString());
    }

    @Test
    void basicRootTests() {
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
    void basicSectionTest() {
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
    void basicFileCompareTest() {
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
    void testFileFileEqualIgnore() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_IGNORE, true);
        assertEquals("REMOVE IGNORE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    void testFileFileEqualDelete() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_DELETE, true);
        assertEquals("REMOVE REMOVE_SOURCE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    void testFileFileEqualFolder() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_FOLDER, true);
        assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.NONE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    void testFileFileEqualBackup() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_BACKUP, true);
        assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.NONE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    void testFileFileEqualNull() {
        DbCompareNode compare = testFileFile(null, true);
        assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.NONE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    void testFileFileEqualWarn() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_WARN, true);
        assertEquals("REMOVE WARN FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    void testFileFileNotEqualWarn() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_WARN, false);
        assertEquals("REMOVE WARN FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    void testFileFileNotEqualIgnore() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_IGNORE, false);
        assertEquals("REMOVE IGNORE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    void testFileFileNotEqualDelete() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_DELETE, false);
        assertEquals("REMOVE REMOVE_SOURCE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    void testFileFileNotEqualFolder() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_FOLDER, false);
        assertEquals("COPY NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    void testFileFileNotEqualBackup() {
        DbCompareNode compare = testFileFile(ClassificationActionType.CA_BACKUP, false);
        assertEquals("COPY NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    void testFileFileNotEqualNull() {
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
    void testFileDirectoryWarn() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_WARN);
        assertEquals("REMOVE WARN FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    void testFileDirectoryIgnore() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_IGNORE);
        assertEquals("REMOVE IGNORE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    void testFileDirectoryDelete() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_DELETE);
        assertEquals("REMOVE REMOVE_SOURCE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.REMOVE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    void testFileDirectoryFolder() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_FOLDER);
        assertEquals("RECREATE_AS_FILE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.RECREATE_AS_FILE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    void testFileDirectoryBackup() {
        DbCompareNode compare = testFileDirectory(ClassificationActionType.CA_BACKUP);
        assertEquals("RECREATE_AS_FILE NONE FSO_FILE>1 FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.RECREATE_AS_FILE, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    void testFileDirectoryNull() {
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
    void testFileNullWarn() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_WARN);
        assertEquals("COPY WARN FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.WARN, compare.getSubActionType());
    }

    @Test
    void testFileNullIgnore() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_IGNORE);
        assertEquals("COPY IGNORE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.IGNORE, compare.getSubActionType());
    }

    @Test
    void testFileNullDelete() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_DELETE);
        assertEquals("COPY REMOVE_SOURCE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE, compare.getSubActionType());
    }

    @Test
    void testFileNullFolder() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_FOLDER);
        assertEquals("COPY NONE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    void testFileNullBackup() {
        DbCompareNode compare = testFileNull(ClassificationActionType.CA_BACKUP);
        assertEquals("COPY NONE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    void testFileNullNull() {
        DbCompareNode compare = testFileNull(null);
        assertEquals("COPY NONE FSO_FILE>1", compare.toString());
        assertFalse(compare.getName().isPresent());
        assertEquals(DbCompareNode.ActionType.COPY, compare.getActionType());
        assertEquals(DbCompareNode.SubActionType.NONE, compare.getSubActionType());
    }

    @Test
    void testDirectoryDirectory() {
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
    void testDirectoryNull() {
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
    void testDirectoryFile() {
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
    void testNullFile() {
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
    void testNullDirectory() {
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
    void compareFileTest2() {
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
    void dbTreeRemovedDirectory() {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.testDirectoryRemoved());
    }

    @Test
    void dbTreeRemovedNoSourceFailure () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.testRemovedNoSourceFailure());
    }

    @Test
    void dbTreeDirectoryAdded () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.testDirectoryAdded());
    }

    @Test
    void dbTreeDeleteFileRemoveFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteFileRemoveFile());
    }

    @Test
    void dbTreeAddedNoDestinationFailure () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.testAddedNoDestinationFailure());
    }

    @Test
    void dbTreeDeleteFileRecreateAsDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteFileRecreateAsDirectory());
    }

    @Test
    void dbTreeDeleteFileRemovedDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteFileRemoveDirectory());
    }

    @Test
    void dbTreeDeleteFileCopyFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteFileCopyFile());
    }

    @Test
    void dbTreeDeleteDirectoryRemoveDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteDirectoryRemoveDirectory());
    }

    @Test
    void dbTreeDeleteDirectoryCopyDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteDirectoryCopyDirectory());
    }

    @Test
    void dbTreeDeleteDirectoryRemoveFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteDirectoryRemoveFile());
    }

    @Test
    void dbTreeDeleteDirectoryRecreateAsFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.deleteDirectoryRecreateAsFile());
    }

    @Test
    void dbTreeInsertDirectoryCopyDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertDirectoryCopyDirectory());
    }

    @Test
    void dbTreeInsertDirectoryRecreateAsDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertDirectoryRecreateAsDirectory());
    }

    @Test
    void dbTreeInsertDirectoryRemoveDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertDirectoryRemoveDirectory());
    }

    @Test
    void dbTreeInsertDirectoryCopyFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertDirectoryCopyFile());
    }

    @Test
    void dbTreeInsertFileRecreateAsFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertFileRecreateAsFile());
    }

    @Test
    void dbTreeInsertFileCopyDirectory () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertFileCopyDirectory());
    }

    @Test
    void dbTreeInsertFileRemoveFile () {
        BasicDbTree basicDbTree = new BasicDbTree();
        assertTrue(basicDbTree.insertFileRemoveFile());
    }
}
