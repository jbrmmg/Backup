package com.jbr.middletier.backup.filetree;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.filetree.compare.DbTree;
import com.jbr.middletier.backup.filetree.compare.node.DbCompareNode;
import com.jbr.middletier.backup.filetree.compare.node.SectionNode;
import com.jbr.middletier.backup.filetree.database.DbDirectory;
import com.jbr.middletier.backup.filetree.database.DbFile;
import com.jbr.middletier.backup.filetree.database.DbNode;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import com.jbr.middletier.backup.filetree.helpers.*;
import com.jbr.middletier.backup.filetree.realworld.RwRoot;
import com.jbr.middletier.backup.manager.BackupManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
    BackupManager backupManager;

    @Test
    @DisplayName("Tree Node - Basic Test")
    public void basicTests() {
        BasicTestNode testNode = new BasicTestNode();
        Assert.assertTrue(testNode.test());
        Assert.assertEquals(FileTreeNode.CompareStatusType.ADDED, testNode.getStatus());

        testNode.test2();
        Assert.assertEquals(FileTreeNode.CompareStatusType.UPDATED, testNode.getStatus());

        testNode.test3();
        Assert.assertEquals(FileTreeNode.CompareStatusType.EQUAL, testNode.getStatus());

        testNode.test4();
        Assert.assertEquals("Hello", testNode.getNamedChild("Hello").getName());
        Assert.assertNull(testNode.getNamedChild("Hello2"));
        Assert.assertEquals("Hello 0", testNode.getNamedChild("Hello").toString());
    }

    @Test
    @DisplayName("Tree Root Node - Basic Test")
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
    @DisplayName("Tree Node - Section test")
    public void basicSectionTest() {
        try {
            new RwRoot("does not exist", backupManager);
            Assert.fail();
        } catch (IOException e) {
            Assert.assertEquals("does not exist", e.getMessage());
        }

        BasicSection testRwDbSection = new BasicSection(SectionNode.SectionNodeType.FILE_FOR_INSERT);
        Assert.assertTrue(testRwDbSection.test());

        try {
            new BasicSection(SectionNode.SectionNodeType.UNKNOWN);
            Assert.fail();
        } catch (IllegalStateException e) {
            Assert.assertEquals("Cannot initialise a Rw DB Section object as unknown.", e.getMessage());
        }

        BasicDbFile testDbFile = new BasicDbFile();
        Assert.assertTrue(testDbFile.test());
    }

    @Test
    @DisplayName("DB File - Compare test")
    public void basicFileCompareTest() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setMD5(new MD5("MATCH"));
        fileInfo.setClassification(null);
        fileInfo.setName("Test");
        fileInfo.setSize(291);
        fileInfo.setDate(new Date(10));
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
        fileInfo2.setDate(new Date(20));

        dbFile2 = new DbFile(null, fileInfo2);
        Assert.assertEquals(DBC_EQUAL_EXCEPT_DATE, dbFile.compare(dbFile2));

        fileInfo2.setDate(new Date(10));
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
        fileInfo2.setDate(new Date(20));
        fileInfo2.setMD5(new MD5("NOMATCH"));
        Assert.assertEquals(DBC_NOT_EQUAL, dbFile.compare(dbFile2));
    }

    @Test
    @DisplayName("DB Compare - Compare test")
    public void basicDbCompareTest() {
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        // Directories that are equal.
        DbNode mockSource = mock(DbDirectory.class);
        when(mockSource.isDirectory()).thenReturn(true);
        when(mockSource.getFSO()).thenReturn(mockFSO);

        DbNode mockDestination = mock(DbDirectory.class);
        when(mockDestination.isDirectory()).thenReturn(true);
        when(mockDestination.getFSO()).thenReturn(mockFSO);
        when(mockSource.compare(mockDestination)).thenReturn(DBC_EQUAL);

        DbCompareNode compareNode = new DbCompareNode(null, mockSource, mockDestination);
        Assert.assertEquals("NONE NONE FSO_FILE>1 FSO_FILE>1", compareNode.toString());
        Assert.assertNull(compareNode.getName());
        Assert.assertEquals(DbCompareNode.ActionType.NONE, compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compareNode.getSubActionType());

        compareNode = new DbCompareNode(null, null, mockDestination);
        Assert.assertEquals("REMOVE NONE FSO_FILE>1", compareNode.toString());
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE, compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compareNode.getSubActionType());

        compareNode = new DbCompareNode(null, mockSource, null);
        Assert.assertEquals("COPY NONE FSO_FILE>1", compareNode.toString());
        Assert.assertEquals(DbCompareNode.ActionType.COPY, compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compareNode.getSubActionType());
    }

    @Test
    @DisplayName("DB Compare - Change file to directory")
    public void compareFileToDirectory() {
        // Check the changing from file to directory, and vice versa
        FileSystemObject mockFSO = mock(FileSystemObject.class);
        when(mockFSO.getIdAndType()).thenReturn(new FileSystemObjectId(1, FileSystemObjectType.FSO_FILE));

        DbNode mockSource = mock(DbDirectory.class);
        when(mockSource.isDirectory()).thenReturn(true);
        when(mockSource.getFSO()).thenReturn(mockFSO);

        DbNode mockDestination = mock(DbDirectory.class);
        when(mockDestination.isDirectory()).thenReturn(true);
        when(mockDestination.getFSO()).thenReturn(mockFSO);
        when(mockSource.compare(mockDestination)).thenReturn(DBC_EQUAL);

        Classification classification = mock(Classification.class);
        when(classification.getAction()).thenReturn(ClassificationActionType.CA_FOLDER);

        DbFile mockSourceFile = mock(DbFile.class);
        when(mockSourceFile.isDirectory()).thenReturn(false);
        when(mockSourceFile.getClassification()).thenReturn(classification);
        DbCompareNode compareNode = new DbCompareNode(null, mockSourceFile, mockDestination);
        Assert.assertEquals(DbCompareNode.ActionType.RECREATE_AS_FILE, compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compareNode.getSubActionType());

        compareNode = new DbCompareNode(null, mockSourceFile, mockDestination);
        Assert.assertEquals(DbCompareNode.ActionType.RECREATE_AS_FILE, compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compareNode.getSubActionType());

        compareNode = new DbCompareNode(null, mockSourceFile, mockDestination);
        Assert.assertEquals(DbCompareNode.ActionType.RECREATE_AS_FILE, compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compareNode.getSubActionType());

        mockDestination = mock(DbFile.class);
        when(mockDestination.isDirectory()).thenReturn(false);
        compareNode = new DbCompareNode(null, mockSource, mockDestination);
        Assert.assertEquals(DbCompareNode.ActionType.RECREATE_AS_DIRECTORY, compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE, compareNode.getSubActionType());
    }

    @Test
    @DisplayName("DB Compare - Further testing (1)")
    public void compareFileTest1() {
        Classification classification = mock(Classification.class);
        when(classification.getAction()).thenReturn(ClassificationActionType.CA_FOLDER);

        DbNode mockDestination = mock(DbFile.class);
        when(mockDestination.isDirectory()).thenReturn(false);

        DbFile mockSourceFile = mock(DbFile.class);
        when(mockSourceFile.isDirectory()).thenReturn(false);
        when(mockSourceFile.getClassification()).thenReturn(classification);

        // Check file comparisons.
        when(mockSourceFile.compare(mockDestination)).thenReturn(DBC_NOT_EQUAL);
        when(classification.getAction()).thenReturn(ClassificationActionType.CA_BACKUP);
        DbCompareNode compareNode = new DbCompareNode(null, mockSourceFile, mockDestination);
        Assert.assertEquals(DbCompareNode.ActionType.COPY,compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.NONE,compareNode.getSubActionType());

        when(classification.getAction()).thenReturn(ClassificationActionType.CA_WARN);
        compareNode = new DbCompareNode(null, mockSourceFile, mockDestination);
        Assert.assertEquals(DbCompareNode.ActionType.COPY,compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.WARN,compareNode.getSubActionType());

        when(classification.getAction()).thenReturn(ClassificationActionType.CA_IGNORE);
        compareNode = new DbCompareNode(null, mockSourceFile, mockDestination);
        Assert.assertEquals(DbCompareNode.ActionType.COPY,compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.IGNORE,compareNode.getSubActionType());

        when(classification.getAction()).thenReturn(ClassificationActionType.CA_DELETE);
        compareNode = new DbCompareNode(null, mockSourceFile, mockDestination);
        Assert.assertEquals(DbCompareNode.ActionType.COPY,compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.REMOVE_SOURCE,compareNode.getSubActionType());

        when(mockSourceFile.compare(mockDestination)).thenReturn(DBC_EQUAL_EXCEPT_DATE);
        when(classification.getAction()).thenReturn(ClassificationActionType.CA_BACKUP);
        compareNode = new DbCompareNode(null, mockSourceFile, mockDestination);
        Assert.assertEquals(DbCompareNode.ActionType.COPY,compareNode.getActionType());
        Assert.assertEquals(DbCompareNode.SubActionType.DATE_UPDATE,compareNode.getSubActionType());

        compareNode = new DbCompareNode(null, false, mockSourceFile);
        Assert.assertEquals(DbCompareNode.ActionType.REMOVE,compareNode.getActionType());

        compareNode = new DbCompareNode(null, true, mockSourceFile);
        Assert.assertEquals(DbCompareNode.ActionType.COPY,compareNode.getActionType());
    }

    @Test
    @DisplayName("DB Compare - Further testing (2)")
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
    @DisplayName("DB Tree - test db tree")
    public void dbTreeTest() {
        BasicDbTree basicDbTree = new BasicDbTree();
        basicDbTree.test();
    }
}
