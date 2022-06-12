package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.MD5;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;
import com.jbr.middletier.backup.filetree.compare.node.SectionNode;
import com.jbr.middletier.backup.filetree.database.DbDirectory;
import com.jbr.middletier.backup.filetree.database.DbFile;
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

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static com.jbr.middletier.backup.filetree.database.DbNodeCompareResultType.*;

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
            try {
                childAdded(new BasicTestNode());
                Assert.fail();
            } catch (IllegalStateException e) {
                Assert.assertEquals("Real World Directory children must be Real World Directory or File.",e.getMessage());
            }

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

        BasicTestRootNode testRootNode = new BasicTestRootNode();
        Assert.assertEquals("Root: 0", testRootNode.toString());

        BasicRwFile testRwFile = new BasicRwFile();
        Assert.assertTrue(testRwFile.test());
        Assert.assertEquals("RW (file): test", testRwFile.toString());

        BasicRwDirectory testRwDirectory = new BasicRwDirectory();
        Assert.assertTrue(testRwDirectory.test());
        Assert.assertEquals("RW (dir): Test 0", testRwDirectory.toString());

        try {
            RwRoot rwRoot = new RwRoot("does not exist", backupManager);
            Assert.fail();
        } catch (IOException e) {
            Assert.assertEquals("does not exist", e.getMessage());
        }

        BasicSection testRwDbSection = new BasicSection(SectionNode.SectionNodeType.FILE_FOR_INSERT);
        Assert.assertTrue(testRwDbSection.test());

        try {
            testRwDbSection = new BasicSection(SectionNode.SectionNodeType.UNKNOWN);
            Assert.fail();
        } catch (IllegalStateException e) {
            Assert.assertEquals("Cannot initialise a Rw DB Section object as unknown.", e.getMessage());
        }

        BasicDbFile testDbFile = new BasicDbFile();
        Assert.assertTrue(testDbFile.test());

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
}
