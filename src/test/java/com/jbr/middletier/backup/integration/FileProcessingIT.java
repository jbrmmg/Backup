package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.LocationRepository;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
import com.jbr.middletier.backup.dto.ProcessResultDTO;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.compare.RwDbTree;
import com.jbr.middletier.backup.filetree.compare.node.RwDbCompareNode;
import com.jbr.middletier.backup.filetree.compare.node.SectionNode;
import com.jbr.middletier.backup.filetree.database.*;
import com.jbr.middletier.backup.filetree.realworld.RwFile;
import com.jbr.middletier.backup.filetree.realworld.RwNode;
import com.jbr.middletier.backup.filetree.realworld.RwRoot;
import com.jbr.middletier.backup.manager.FileSystem;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testcontainers.containers.MySQLContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {FileProcessingIT.Initializer.class})
@ActiveProfiles(value="it")
public class FileProcessingIT extends FileTester {
    @SuppressWarnings("rawtypes")
    @ClassRule
    public static MySQLContainer mysqlContainer = new MySQLContainer("mysql:8.0.28")
            .withDatabaseName("integration-tests-db")
            .withUsername("sa")
            .withPassword("sa");

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + mysqlContainer.getJdbcUrl(),
                    "spring.datasource.username=" + mysqlContainer.getUsername(),
                    "spring.datasource.password=" + mysqlContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    private static class BasicDbDirectory extends DbDirectory {
        public BasicDbDirectory(DirectoryInfo directoryInfo, FileRepository fileRepository, DirectoryRepository directoryRepository) {
            super(null, directoryInfo, fileRepository, directoryRepository);
        }

        public boolean test(BasicDbDirectory another, boolean anotherEqual) {
            try {
                childAdded(nullNode);
                Assert.fail();
                return false;
            } catch (IllegalStateException e) {
                Assert.assertEquals("Database Directory children must be Database Directory or File.", e.getMessage());
            }

            Assert.assertEquals(DbNodeCompareResultType.DBC_EQUAL,compare(this));

            FileInfo fileInfo = new FileInfo();
            DbNode dbNode = new DbFile(null, fileInfo);
            Assert.assertEquals(DbNodeCompareResultType.DBC_NOT_EQUAL,compare(dbNode));

            if(anotherEqual) {
                Assert.assertEquals(DbNodeCompareResultType.DBC_EQUAL, compare(another));
            } else {
                Assert.assertEquals(DbNodeCompareResultType.DBC_NOT_EQUAL, compare(another));
            }

            return true;
        }
    }

    @Autowired
    FileSystem fileSystem;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    DirectoryRepository directoryRepository;

    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    LocationRepository locationRepository;

    @Before
    public void initialise() throws IOException {
        initialiseDirectories();
    }

    @Test
    public void basicRealWorld() throws Exception {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        RwRoot rwRoot = new RwRoot(sourceDirectory, fileSystem);

        // Check that the details were read as expected.
        Assert.assertNull(rwRoot.getName());
        int count = 0;
        for (FileTreeNode nextNode : rwRoot.getChildren()) {
            for (FileTreeNode children : nextNode.getChildren()) {
                Assert.assertEquals("Backup.dxf~", children.getName());
                count++;
            }
        }
        Assert.assertEquals(1, count);

        Assert.assertEquals("Real World (R): " + sourceDirectory + " 1", rwRoot.toString());
    }

    @Test
    public void basicDatabase() {
        Optional<Location> location = locationRepository.findById(1);
        Assert.assertTrue(location.isPresent());

        Source source = new Source();
        source.setLocation(location.get());
        source.setStatus(SourceStatusType.SST_OK);
        source.setPath("test");
        sourceRepository.save(source);

        DirectoryInfo level1 = new DirectoryInfo();
        level1.setParent(source);
        level1.setName("Documents");
        level1.clearRemoved();
        directoryRepository.save(level1);

        FileInfo file = new FileInfo();
        file.setParent(level1);
        file.setName("testFile.txt");
        file.clearRemoved();
        fileRepository.save(file);

        DbRoot dbRoot = new DbRoot(source, fileRepository, directoryRepository);

        // Check that the details were read as expected.
        Assert.assertNull(dbRoot.getName());
        int count = 0;
        for (FileTreeNode nextNode : dbRoot.getChildren()) {
            for (FileTreeNode children : nextNode.getChildren()) {
                Assert.assertEquals("testFile.txt", children.getName());
                count++;
            }
        }
        Assert.assertEquals(1, count);

        fileRepository.delete(file);
        directoryRepository.delete(level1);
        sourceRepository.delete(source);
    }

    @Test
    public void compareRwDb1() throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm");

        Optional<Location> location = locationRepository.findById(1);
        Assert.assertTrue(location.isPresent());

        Source source = new Source();
        source.setLocation(location.get());
        source.setStatus(SourceStatusType.SST_OK);
        source.setPath("test");
        sourceRepository.save(source);

        DirectoryInfo level1 = new DirectoryInfo();
        level1.setParent(source);
        level1.setName("Documents");
        level1.clearRemoved();
        //1998-04-10-11-43        12
        directoryRepository.save(level1);

        FileInfo file = new FileInfo();
        file.setParent(level1);
        file.setName("Backup.dxf~");
        file.clearRemoved();
        file.setSize(12);
        file.setDate(LocalDateTime.parse("1998-04-10-11-43",formatter));
        fileRepository.save(file);

        DbRoot dbRoot = new DbRoot(source, fileRepository, directoryRepository);

        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        RwRoot rwRoot = new RwRoot(sourceDirectory, fileSystem);

        // Compare
        RwDbTree rwDbTree = new RwDbTree(rwRoot, dbRoot);
        rwDbTree.compare();

        List<FileTreeNode> nodes = rwDbTree.getOrderedNodeList();

        Assert.assertEquals(4, nodes.size());
        for (FileTreeNode nextNode : nodes) {
            Assert.assertTrue(nextNode instanceof SectionNode);
            SectionNode sectionNode = (SectionNode) nextNode;
            Assert.assertNull(sectionNode.getName());
        }

        Assert.assertNull(rwDbTree.getName());

        fileRepository.deleteAll();
        directoryRepository.deleteAll();
        sourceRepository.deleteAll();
    }

    @Test
    public void compareRwDb2() throws Exception {
        Optional<Location> location = locationRepository.findById(1);
        Assert.assertTrue(location.isPresent());

        Source source = new Source();
        source.setLocation(location.get());
        source.setStatus(SourceStatusType.SST_OK);
        source.setPath("test");
        sourceRepository.save(source);

        DirectoryInfo level1 = new DirectoryInfo();
        level1.setParent(source);
        level1.setName("Documents");
        level1.clearRemoved();
        directoryRepository.save(level1);

        DbRoot dbRoot = new DbRoot(source, fileRepository, directoryRepository);

        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        RwRoot rwRoot = new RwRoot(sourceDirectory, fileSystem);

        // Compare
        RwDbTree rwDbTree = new RwDbTree(rwRoot, dbRoot);
        rwDbTree.compare();

        List<FileTreeNode> nodes = rwDbTree.getOrderedNodeList();

        Assert.assertEquals(5, nodes.size());
        int sectionCount = 0;
        for (FileTreeNode nextNode : nodes) {
            if (nextNode instanceof SectionNode) {
                SectionNode sectionNode = (SectionNode) nextNode;
                Assert.assertNull(sectionNode.getName());
                sectionCount++;
            } else if (nextNode instanceof RwDbCompareNode) {
                RwDbCompareNode compareNode = (RwDbCompareNode) nextNode;
                Assert.assertNull(compareNode.getName());
                Assert.assertEquals(RwDbCompareNode.ActionType.INSERT, compareNode.getActionType());
                Assert.assertFalse(compareNode.isDirectory());
            } else {
                Assert.fail();
            }
        }

        Assert.assertNull(rwDbTree.getName());
        Assert.assertEquals(4, sectionCount);

        fileRepository.deleteAll();
        directoryRepository.deleteAll();
        sourceRepository.deleteAll();
    }

    @Test
    public void compareRwDb3() throws Exception {
        Optional<Location> location = locationRepository.findById(1);
        Assert.assertTrue(location.isPresent());

        Source source = new Source();
        source.setLocation(location.get());
        source.setStatus(SourceStatusType.SST_OK);
        source.setPath("test");
        sourceRepository.save(source);

        DirectoryInfo level1 = new DirectoryInfo();
        level1.setParent(source);
        level1.setName("Documents");
        level1.clearRemoved();
        directoryRepository.save(level1);

        DirectoryInfo file = new DirectoryInfo();
        file.setParent(level1);
        file.setName("Backup.dxf~");
        file.clearRemoved();
        directoryRepository.save(file);

        DbRoot dbRoot = new DbRoot(source, fileRepository, directoryRepository);

        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        RwRoot rwRoot = new RwRoot(sourceDirectory, fileSystem);

        // Compare
        RwDbTree rwDbTree = new RwDbTree(rwRoot, dbRoot);
        rwDbTree.compare();

        List<FileTreeNode> nodes = rwDbTree.getOrderedNodeList();

        Assert.assertEquals(6, nodes.size());
        int sectionCount = 0;
        int compareCount = 0;
        for (FileTreeNode nextNode : nodes) {
            if (nextNode instanceof SectionNode) {
                SectionNode sectionNode = (SectionNode) nextNode;
                Assert.assertNull(sectionNode.getName());
                sectionCount++;
            } else if (nextNode instanceof RwDbCompareNode) {
                RwDbCompareNode compareNode = (RwDbCompareNode) nextNode;
                Assert.assertEquals(RwDbCompareNode.ActionType.RECREATE_AS_FILE, compareNode.getActionType());
                Assert.assertFalse(compareNode.isDirectory());
                compareCount++;
            } else {
                Assert.fail();
            }
        }

        Assert.assertNull(rwDbTree.getName());
        Assert.assertEquals(4, sectionCount);
        Assert.assertEquals(2, compareCount);

        Assert.assertNull(rwDbTree.getName());

        directoryRepository.delete(file);
        directoryRepository.delete(level1);
        sourceRepository.deleteAll();
    }

    @Test
    public void compareRwDb4() throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm");

        Optional<Location> location = locationRepository.findById(1);
        Assert.assertTrue(location.isPresent());

        Source source = new Source();
        source.setLocation(location.get());
        source.setStatus(SourceStatusType.SST_OK);
        source.setPath("test");
        sourceRepository.save(source);

        DirectoryInfo level1 = new DirectoryInfo();
        level1.setParent(source);
        level1.setName("Documents");
        level1.clearRemoved();
        directoryRepository.save(level1);

        FileInfo file = new FileInfo();
        file.setParent(level1);
        file.setName("Backup");
        file.clearRemoved();
        fileRepository.save(file);

        FileInfo file2 = new FileInfo();
        file2.setParent(level1);
        file2.setName("Text1.txt");
        file2.clearRemoved();
        file2.setSize(12);
        file2.setDate(LocalDateTime.parse("1998-04-10-11-43",formatter));
        fileRepository.save(file2);

        DbRoot dbRoot = new DbRoot(source, fileRepository, directoryRepository);

        List<StructureDescription> sourceDescription = getTestStructure("test4");
        copyFiles(sourceDescription, sourceDirectory);

        RwRoot rwRoot = new RwRoot(sourceDirectory, fileSystem);

        // Compare
        RwDbTree rwDbTree = new RwDbTree(rwRoot, dbRoot);
        rwDbTree.compare();

        List<FileTreeNode> nodes = rwDbTree.getOrderedNodeList();

        Assert.assertEquals(7, nodes.size());
        int sectionCount = 0;
        int compareDirectoryCount = 0;
        int compareFileCount = 0;
        for (FileTreeNode nextNode : nodes) {
            if (nextNode instanceof SectionNode) {
                SectionNode sectionNode = (SectionNode) nextNode;
                Assert.assertNull(sectionNode.getName());
                sectionCount++;
            } else if (nextNode instanceof RwDbCompareNode) {
                RwDbCompareNode compareNode = (RwDbCompareNode) nextNode;
                if (compareNode.isDirectory()) {
                    Assert.assertEquals(RwDbCompareNode.ActionType.RECREATE_AS_DIRECTORY, compareNode.getActionType());
                    compareDirectoryCount++;
                } else {
                    Assert.assertEquals(RwDbCompareNode.ActionType.INSERT, compareNode.getActionType());
                    compareFileCount++;
                }
            } else {
                Assert.fail();
            }
        }

        Assert.assertNull(rwDbTree.getName());
        Assert.assertEquals(4, sectionCount);
        Assert.assertEquals(2, compareDirectoryCount);
        Assert.assertEquals(1, compareFileCount);

        Assert.assertNull(rwDbTree.getName());

        fileRepository.delete(file);
        fileRepository.delete(file2);
        directoryRepository.delete(level1);
        sourceRepository.deleteAll();
    }

    @Test
    public void compareRwDb5() throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm");

        Optional<Location> location = locationRepository.findById(1);
        Assert.assertTrue(location.isPresent());

        Source source = new Source();
        source.setLocation(location.get());
        source.setStatus(SourceStatusType.SST_OK);
        source.setPath("test");
        sourceRepository.save(source);

        DirectoryInfo level1 = new DirectoryInfo();
        level1.setParent(source);
        level1.setName("Documents");
        level1.clearRemoved();
        directoryRepository.save(level1);

        FileInfo file = new FileInfo();
        file.setParent(level1);
        file.setName("Deleted.txt");
        file.clearRemoved();
        fileRepository.save(file);

        FileInfo file2 = new FileInfo();
        file2.setParent(level1);
        file2.setName("Backup.dxf~");
        file2.clearRemoved();
        file2.setSize(12);
        file2.setDate(LocalDateTime.parse("1998-04-10-11-43",formatter));
        fileRepository.save(file2);

        DbRoot dbRoot = new DbRoot(source, fileRepository, directoryRepository);

        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        RwRoot rwRoot = new RwRoot(sourceDirectory, fileSystem);

        // Compare
        RwDbTree rwDbTree = new RwDbTree(rwRoot, dbRoot);
        rwDbTree.compare();

        List<FileTreeNode> nodes = rwDbTree.getOrderedNodeList();

        Assert.assertEquals(5, nodes.size());
        int sectionCount = 0;
        int compareCount = 0;
        for (FileTreeNode nextNode : nodes) {
            if (nextNode instanceof SectionNode) {
                SectionNode sectionNode = (SectionNode) nextNode;
                Assert.assertNull(sectionNode.getName());
                sectionCount++;
            } else if (nextNode instanceof RwDbCompareNode) {
                RwDbCompareNode compareNode = (RwDbCompareNode) nextNode;
                Assert.assertEquals(RwDbCompareNode.ActionType.DELETE, compareNode.getActionType());
                Assert.assertFalse(compareNode.isDirectory());
                compareCount++;
            } else {
                Assert.fail();
            }
        }

        Assert.assertNull(rwDbTree.getName());
        Assert.assertEquals(4, sectionCount);
        Assert.assertEquals(1, compareCount);

        Assert.assertNull(rwDbTree.getName());

        fileRepository.delete(file2);
        fileRepository.delete(file);
        directoryRepository.delete(level1);
        sourceRepository.deleteAll();
    }

    @Test
    public void compareRwDb6() throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm");

        Optional<Location> location = locationRepository.findById(1);
        Assert.assertTrue(location.isPresent());

        Source source = new Source();
        source.setLocation(location.get());
        source.setStatus(SourceStatusType.SST_OK);
        source.setPath("test");
        sourceRepository.save(source);

        DirectoryInfo level1 = new DirectoryInfo();
        level1.setParent(source);
        level1.setName("Documents");
        level1.clearRemoved();
        directoryRepository.save(level1);

        DirectoryInfo extraDirectory = new DirectoryInfo();
        extraDirectory.setParent(level1);
        extraDirectory.setName("Deleted");
        extraDirectory.clearRemoved();
        directoryRepository.save(extraDirectory);

        FileInfo file2 = new FileInfo();
        file2.setParent(level1);
        file2.setName("Backup.dxf~");
        file2.clearRemoved();
        file2.setSize(12);
        file2.setDate(LocalDateTime.parse("1998-04-10-11-43",formatter));
        fileRepository.save(file2);

        DbRoot dbRoot = new DbRoot(source, fileRepository, directoryRepository);

        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        RwRoot rwRoot = new RwRoot(sourceDirectory, fileSystem);

        // Compare
        RwDbTree rwDbTree = new RwDbTree(rwRoot, dbRoot);
        rwDbTree.compare();

        List<FileTreeNode> nodes = rwDbTree.getOrderedNodeList();

        Assert.assertEquals(5, nodes.size());
        int sectionCount = 0;
        int compareCount = 0;
        for (FileTreeNode nextNode : nodes) {
            if (nextNode instanceof SectionNode) {
                SectionNode sectionNode = (SectionNode) nextNode;
                Assert.assertNull(sectionNode.getName());
                sectionCount++;
            } else if (nextNode instanceof RwDbCompareNode) {
                RwDbCompareNode compareNode = (RwDbCompareNode) nextNode;
                Assert.assertEquals(RwDbCompareNode.ActionType.DELETE, compareNode.getActionType());
                Assert.assertTrue(compareNode.isDirectory());
                compareCount++;
            } else {
                Assert.fail();
            }
        }

        Assert.assertNull(rwDbTree.getName());
        Assert.assertEquals(4, sectionCount);
        Assert.assertEquals(1, compareCount);

        Assert.assertNull(rwDbTree.getName());

        fileRepository.delete(file2);
        directoryRepository.delete(extraDirectory);
        directoryRepository.delete(level1);
        sourceRepository.deleteAll();
    }

    @Test
    public void compareRwDb7() throws Exception {
        Optional<Location> location = locationRepository.findById(1);
        Assert.assertTrue(location.isPresent());

        Source source = new Source();
        source.setLocation(location.get());
        source.setStatus(SourceStatusType.SST_OK);
        source.setPath("test");
        sourceRepository.save(source);

        DbRoot dbRoot = new DbRoot(source, fileRepository, directoryRepository);

        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        RwRoot rwRoot = new RwRoot(sourceDirectory, fileSystem);

        // Compare
        RwDbTree rwDbTree = new RwDbTree(rwRoot, dbRoot);
        rwDbTree.compare();

        List<FileTreeNode> nodes = rwDbTree.getOrderedNodeList();

        Assert.assertEquals(6, nodes.size());
        int sectionCount = 0;
        int compareDirectoryCount = 0;
        int compareFileCount = 0;
        for (FileTreeNode nextNode : nodes) {
            if (nextNode instanceof SectionNode) {
                SectionNode sectionNode = (SectionNode) nextNode;
                Assert.assertNull(sectionNode.getName());
                sectionCount++;
            } else if (nextNode instanceof RwDbCompareNode) {
                RwDbCompareNode compareNode = (RwDbCompareNode) nextNode;
                if (compareNode.isDirectory()) {
                    Assert.assertEquals(RwDbCompareNode.ActionType.INSERT, compareNode.getActionType());
                    compareDirectoryCount++;
                } else {
                    Assert.assertEquals(RwDbCompareNode.ActionType.INSERT, compareNode.getActionType());
                    compareFileCount++;
                }
            } else {
                Assert.fail();
            }
        }

        Assert.assertNull(rwDbTree.getName());
        Assert.assertEquals(4, sectionCount);
        Assert.assertEquals(1, compareFileCount);
        Assert.assertEquals(1, compareDirectoryCount);

        Assert.assertNull(rwDbTree.getName());

        sourceRepository.deleteAll();
    }

    @Test
    public void checkFilter() throws Exception {
        List<StructureDescription> sourceDescription = getTestStructure("test5");
        copyFiles(sourceDescription, sourceDirectory);

        RwRoot rwRoot = new RwRoot(sourceDirectory, fileSystem);

        int childCount = 0;
        for(FileTreeNode nextChild : rwRoot.getChildren()) {
            Assert.assertTrue(nextChild instanceof RwNode);
            childCount++;
        }
        Assert.assertEquals(3, childCount);

        rwRoot.removeFilteredChildren(null);
        rwRoot.removeFilteredChildren("");
        childCount = 0;
        for(FileTreeNode nextChild : rwRoot.getChildren()) {
            Assert.assertTrue(nextChild instanceof RwNode);
            childCount++;
        }
        Assert.assertEquals(3, childCount);

        rwRoot.removeFilteredChildren("\\d{4}$");
        childCount = 0;
        for(FileTreeNode nextChild : rwRoot.getChildren()) {
            Assert.assertTrue(nextChild instanceof RwNode);
            childCount++;
        }
        Assert.assertEquals(1, childCount);
    }

    @Test
    public void checkDbDirectoryException() {
        DirectoryInfo tempDirectory = new DirectoryInfo();
        tempDirectory.setParent(null);
        tempDirectory.setName("Documents");
        tempDirectory.clearRemoved();
        directoryRepository.save(tempDirectory);

        DirectoryInfo tempDirectory2 = new DirectoryInfo();
        tempDirectory2.setParent(null);
        tempDirectory2.setName("Documents");
        tempDirectory2.clearRemoved();
        directoryRepository.save(tempDirectory2);

        BasicDbDirectory testDbDirectory = new BasicDbDirectory(tempDirectory, fileRepository, directoryRepository);
        BasicDbDirectory another = new BasicDbDirectory(tempDirectory2, fileRepository, directoryRepository);
        Assert.assertTrue(testDbDirectory.test(another,true));

        tempDirectory2.setName("Documents2");
        Assert.assertTrue(testDbDirectory.test(another,false));

        Assert.assertNotNull(testDbDirectory.getFSO());

        directoryRepository.deleteAll();
    }

    @Test
    public void checkCompareIO() throws IOException {
        Path mockPath = mock(Path.class);
        FileSystemProvider fsProvider = mock(FileSystemProvider.class);
        doThrow(new IOException("Fail to delete")).when(fsProvider).deleteIfExists(mockPath);

        java.nio.file.FileSystem mockFS = mock(java.nio.file.FileSystem.class);
        when(mockFS.provider()).thenReturn(fsProvider);

        when(mockPath.getFileSystem()).thenReturn(mockFS);

        File mockFile = mock(File.class);
        when(mockFile.toPath()).thenReturn(mockPath);

        RwFile mockRwFile = mock(RwFile.class);
        when(mockRwFile.getFile()).thenReturn(mockFile);

        FileInfo fileInfo = new FileInfo();
        DbFile dbFile = new DbFile(null, fileInfo);
        RwDbCompareNode testNode = new RwDbCompareNode(null, mockRwFile, dbFile);
        Assert.assertNotNull(testNode);
    }

    @Test
    public void checkDirectoryNotEmpty() throws IOException, ParseException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        Assert.assertFalse(fileSystem.directoryIsEmpty(new File(sourceDirectory).toPath()));
    }

    @Test
    public void checkDirectoryNotEmpty2() throws IOException, ParseException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        File testFile = new File(sourceDirectory + "/does not exist.txt");
        Assert.assertFalse(fileSystem.directoryIsEmpty(testFile.toPath()));
    }

    @Test
    public void checkDeleteDoesNotExist() throws IOException, ParseException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        File nonExistFile = mock(File.class);
        when(nonExistFile.exists()).thenReturn(false);

        ProcessResultDTO result = mock(ProcessResultDTO.class);

        fileSystem.deleteFile(nonExistFile,result);
        verify(nonExistFile, times(1)).exists();
        verify(result, times(0)).setProblems();
    }

    @Test
    public void checkDeleteFileWithDirectory() throws IOException, ParseException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        File directory = mock(File.class);
        when(directory.exists()).thenReturn(true);
        when(directory.isDirectory()).thenReturn(true);

        ProcessResultDTO result = mock(ProcessResultDTO.class);

        fileSystem.deleteFile(directory,result);
        verify(directory, times(1)).exists();
        verify(directory, times(1)).isDirectory();
        verify(result, times(0)).setProblems();
    }

    @Test
    public void checkDeleteDirDoesNotExist() throws IOException, ParseException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        File nonExistFile = mock(File.class);
        when(nonExistFile.exists()).thenReturn(false);

        ProcessResultDTO result = mock(ProcessResultDTO.class);

        fileSystem.deleteDirectory(nonExistFile,result);
        verify(nonExistFile, times(1)).exists();
        verify(result, times(0)).setProblems();
    }

    @Test
    public void checkDeleteDirectoryWithFile() throws IOException, ParseException {
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        File directory = mock(File.class);
        when(directory.exists()).thenReturn(true);
        when(directory.isDirectory()).thenReturn(false);

        ProcessResultDTO result = mock(ProcessResultDTO.class);

        fileSystem.deleteDirectory(directory,result);
        verify(directory, times(1)).exists();
        verify(directory, times(1)).isDirectory();
        verify(result, times(0)).setProblems();
    }
}
