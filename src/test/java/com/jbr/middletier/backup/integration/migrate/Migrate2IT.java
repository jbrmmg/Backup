package com.jbr.middletier.backup.integration.migrate;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.dto.MigrateDateDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import com.jbr.middletier.backup.manager.MigrateManager;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
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

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {Migrate2IT.Initializer.class})
@ActiveProfiles(value="it-migration2")
public class Migrate2IT {
    private static final Logger LOG = LoggerFactory.getLogger(MigrateIT.class);

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

    @Autowired
    FileSystemObjectManager fileSystemObjectManager;

    @Autowired
    AssociatedFileDataManager associatedFileDataManager;

    @Autowired
    MigrateManager migrateManager;

    @Test
    public void testMigrate2() {
        LOG.info("Migration test 2");

        // Perform the migration.
        List<MigrateDateDTO> result = migrateManager.postMigrationChecks();
        Assert.assertEquals(233,result.get(0).getCount(MigrateDateDTO.MigrateDataCountType.DIRECTORIES_UPDATED));
        Assert.assertEquals(0,result.get(0).getCount(MigrateDateDTO.MigrateDataCountType.NEW_DIRECTORIES));
        Assert.assertEquals(236,result.get(0).getCount(MigrateDateDTO.MigrateDataCountType.DOT_FILES_REMOVED));
        Assert.assertEquals(3,result.get(0).getCount(MigrateDateDTO.MigrateDataCountType.BLANKS_REMOVED));

        // Check the directory structure.
        List<DirectoryInfo> directories = new ArrayList<>();
        List<FileInfo> files = new ArrayList<>();
        fileSystemObjectManager.loadImmediateByParent(1, directories, files);

        Assert.assertEquals(20,directories.size());
        Assert.assertEquals(0,files.size());

        // Check again.
        directories = new ArrayList<>();
        files = new ArrayList<>();
        fileSystemObjectManager.loadByParent(1, directories, files);

        Assert.assertEquals(80,directories.size());
        Assert.assertEquals(386,files.size());
    }
}
