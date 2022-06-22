package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;
import com.jbr.middletier.backup.filetree.compare.DbTree;
import com.jbr.middletier.backup.filetree.compare.node.DbCompareNode;
import com.jbr.middletier.backup.filetree.compare.node.SectionNode;
import com.jbr.middletier.backup.filetree.database.DbDirectory;
import com.jbr.middletier.backup.filetree.database.DbFile;
import com.jbr.middletier.backup.filetree.database.DbNode;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import com.jbr.middletier.backup.filetree.realworld.RwDirectory;
import com.jbr.middletier.backup.filetree.realworld.RwFile;
import com.jbr.middletier.backup.filetree.realworld.RwRoot;
import com.jbr.middletier.backup.manager.BackupManager;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.jbr.middletier.backup.filetree.database.DbNodeCompareResultType.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFileTree {
    // The following classes help test the inner workings of the classes.
    private static class BasicTestNode extends FileTreeNode {
        private String name;

        protected BasicTestNode() {
            super(null);
            this.status = CompareStatusType.ADDED;
            this.name = null;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        protected void childAdded(FileTreeNode newChild) {

        }

        public boolean test() {
            // Test the null node.
            Assert.assertNull(nullNode.getName());

            try {
                nullNode.addChild(null);
                Assert.fail();
            } catch (IllegalStateException e) {
                Assert.assertEquals("Null node - cannot add children", e.getMessage());
            }

            return true;
        }

        public boolean test2() {
            BasicTestNode testNode = new BasicTestNode();
            testNode.status = CompareStatusType.ADDED;
            this.children.add(testNode);
            return true;
        }

        public boolean test3() {
            BasicTestNode testNode = new BasicTestNode();
            testNode.status = CompareStatusType.EQUAL;
            this.children.clear();
            this.children.add(testNode);
            return true;
        }

        public boolean test4() {
            BasicTestNode testNode = new BasicTestNode();
            testNode.status = CompareStatusType.EQUAL;
            testNode.name = "Hello";
            this.children.clear();
            this.children.add(testNode);
            return true;
        }
    }

    private static class BasicTestRootNode extends RootFileTreeNode {

        @Override
        public String getName() {
            return null;
        }

        @Override
        protected void childAdded(FileTreeNode newChild) {

        }
    }

    private static class BasicRwFile extends RwFile {
        public BasicRwFile() {
            super(null, new File("test").toPath());
        }

        public boolean test() {
            try {
                childAdded(null);
                Assert.fail();
            } catch(IllegalStateException e) {
                Assert.assertEquals("Cannot add child nodes to a file node.", e.getMessage());
            }

            return true;
        }

        @Override
        public String getName() {
            return "test";
        }
    }

    private static class BasicRwDirectory extends RwDirectory {
        public BasicRwDirectory() {
            super(null, new File("test").toPath());
        }

        @Override
        public String getName() {
            return "Test";
        }

        public boolean test() {
            childAdded(new BasicRwDirectory());
            childAdded(new BasicRwFile());
            BasicTestNode testNode = new BasicTestNode();
            try {
                childAdded(testNode);
                Assert.fail();
            } catch (IllegalStateException e) {
                Assert.assertEquals("Real World Directory children must be Real World Directory or File.",e.getMessage());
            }

            return true;
        }
    }

    private static class BasicDbTree extends DbTree {
        public BasicDbTree() {
            super(null, null);
        }

        public boolean test() {
            DbDirectory mockDbDirectory = mock(DbDirectory.class);
            when(mockDbDirectory.isDirectory()).thenReturn(true);

            FileTreeNode test = createCompareNode(CompareStatusType.REMOVED, null, mockDbDirectory, null);
            Assert.assertNotNull(test);

            try {
                createCompareNode(CompareStatusType.REMOVED, null, null, null);
                Assert.fail();
            } catch(IllegalStateException e) {
                Assert.assertEquals("Status is removed, but no source provided", e.getMessage());
            }

            test = createCompareNode(CompareStatusType.ADDED, null, null, mockDbDirectory);
            Assert.assertNotNull(test);

            try {
                createCompareNode(CompareStatusType.ADDED, null, null, null);
                Assert.fail();
            } catch(IllegalStateException e) {
                Assert.assertEquals("Status is added, but no destination provided", e.getMessage());
            }

            DbCompareNode node = mock(DbCompareNode.class);
            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
            when(node.isDirectory()).thenReturn(false);

            List<FileTreeNode> list = new ArrayList<>();

            findDeleteFiles(node, list);
            Assert.assertEquals(1, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_DIRECTORY);
            when(node.isDirectory()).thenReturn(true);
            findDeleteFiles(node, list);
            Assert.assertEquals(1, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
            when(node.isDirectory()).thenReturn(true);
            findDeleteFiles(node, list);
            Assert.assertEquals(0, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
            when(node.isDirectory()).thenReturn(false);
            findDeleteFiles(node, list);
            Assert.assertEquals(0, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
            when(node.isDirectory()).thenReturn(true);
            findDeleteDirectories(node, list);
            Assert.assertEquals(1, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
            when(node.isDirectory()).thenReturn(true);
            findDeleteDirectories(node, list);
            Assert.assertEquals(0, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
            when(node.isDirectory()).thenReturn(false);
            findDeleteDirectories(node, list);
            Assert.assertEquals(0, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_FILE);
            when(node.isDirectory()).thenReturn(false);
            findDeleteDirectories(node, list);
            Assert.assertEquals(1, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
            when(node.isDirectory()).thenReturn(true);
            findInsertDirectories(node, list);
            Assert.assertEquals(1, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_DIRECTORY);
            when(node.isDirectory()).thenReturn(false);
            findInsertDirectories(node, list);
            Assert.assertEquals(1, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
            when(node.isDirectory()).thenReturn(true);
            findInsertDirectories(node, list);
            Assert.assertEquals(0, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
            when(node.isDirectory()).thenReturn(false);
            findInsertDirectories(node, list);
            Assert.assertEquals(0, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
            when(node.isDirectory()).thenReturn(false);
            findInsertFiles(node, list);
            Assert.assertEquals(1, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_FILE);
            when(node.isDirectory()).thenReturn(false);
            findInsertFiles(node, list);
            Assert.assertEquals(1, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
            when(node.isDirectory()).thenReturn(true);
            findInsertFiles(node, list);
            Assert.assertEquals(0, list.size());
            list.clear();

            when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
            when(node.isDirectory()).thenReturn(false);
            findInsertFiles(node, list);
            Assert.assertEquals(0, list.size());
            list.clear();

            return true;
        }
    }

    private static class BasicSection extends SectionNode {

        public BasicSection(SectionNodeType section) {
            super(section);
        }

        public boolean test() {
            childAdded(null);
            return true;
        }
    }

    private static class BasicDbFile extends DbFile {

        public BasicDbFile() {
            super(null, new FileInfo());
        }

        public boolean test() {
            try {
                childAdded(null);
                Assert.fail();
            } catch (IllegalStateException e) {
                Assert.assertEquals("Cannot add child nodes to a file database node.", e.getMessage());
            }

            return true;
        }
    }

    @Autowired
    BackupManager backupManager;

    @Test
    public void basicTests() {
        BasicTestNode testNode = new BasicTestNode();
        Assert.assertTrue(testNode.test());
        Assert.assertEquals(FileTreeNode.CompareStatusType.ADDED, testNode.getStatus());

        Assert.assertTrue(testNode.test2());
        Assert.assertEquals(FileTreeNode.CompareStatusType.UPDATED, testNode.getStatus());

        Assert.assertTrue(testNode.test3());
        Assert.assertEquals(FileTreeNode.CompareStatusType.EQUAL, testNode.getStatus());

        Assert.assertTrue(testNode.test4());
        Assert.assertEquals("Hello", testNode.getNamedChild("Hello").getName());
        Assert.assertNull(testNode.getNamedChild("Hello2"));
        Assert.assertEquals("Hello 0", testNode.getNamedChild("Hello").toString());
    }

    @Test
    public void basicTests2() {
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
    public void basicTests3() {
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
    public void basicTests4() {
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
    public void compareDbTests() {
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
    public void compareDbTests2() {
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
    public void compareDbTests3() {
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
    public void dbCompareTest() {
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

        BasicDbTree basicDbTree = new BasicDbTree();
        Assert.assertTrue(basicDbTree.test());
    }
}
