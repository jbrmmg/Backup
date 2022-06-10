package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.WebTester;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.LocationRepository;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;
import com.jbr.middletier.backup.filetree.compare.RwDbTree;
import com.jbr.middletier.backup.filetree.compare.node.RwDbSectionNode;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import com.jbr.middletier.backup.filetree.realworld.RwRoot;
import com.jbr.middletier.backup.manager.BackupManager;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import javax.validation.constraints.AssertTrue;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {FileProcessingIT.Initializer.class})
@ActiveProfiles(value="it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileProcessingIT extends FileTester {
    private static final Logger LOG = LoggerFactory.getLogger(SyncApiIT.class);

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

    @Autowired
    BackupManager backupManager;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    DirectoryRepository directoryRepository;

    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    LocationRepository locationRepository;

    @Test
    @Order(1)
    public void basicRealWorld() throws Exception {
        initialiseDirectories();

        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        RwRoot rwRoot = new RwRoot(sourceDirectory, backupManager);

        // Check that the details were read as expected.
        Assert.assertNull(rwRoot.getName());
        int count = 0;
        for(FileTreeNode nextNode: rwRoot.getChildren()) {
            for(FileTreeNode children: nextNode.getChildren()) {
                Assert.assertEquals("Backup.dxf~3", children.getName());
                count++;
            }
        }
        Assert.assertEquals(1, count);
    }

    @Test
    @Order(2)
    public void basicDatabase() throws Exception {
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
        for(FileTreeNode nextNode: dbRoot.getChildren()) {
            for(FileTreeNode children: nextNode.getChildren()) {
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
    @Order(3)
    public void compareRwDb1() throws Exception {
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
        file.setName("Backup.dxf~3");
        file.clearRemoved();
        fileRepository.save(file);

        DbRoot dbRoot = new DbRoot(source, fileRepository, directoryRepository);

        initialiseDirectories();

        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        RwRoot rwRoot = new RwRoot(sourceDirectory, backupManager);

        // Compare
        RwDbTree rwDbTree = new RwDbTree(rwRoot,dbRoot);
        rwDbTree.compare();

        List<FileTreeNode> nodes = rwDbTree.getOrderedNodeList();

        Assert.assertEquals(4, nodes.size());
        for(FileTreeNode nextNode: nodes) {
            Assert.assertTrue(nextNode instanceof RwDbSectionNode);
        }
    }
}
