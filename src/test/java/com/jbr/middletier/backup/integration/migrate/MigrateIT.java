package com.jbr.middletier.backup.integration.migrate;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.ImportSource;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.data.SourceStatusType;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
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

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {MigrateIT.Initializer.class})
@ActiveProfiles(value="it-migration")
public class MigrateIT {
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

    @Test
    public void testMigrationProcess() {
        // That we have data.
        int count = 0;
        int importCount = 0;
        for(Source source :associatedFileDataManager.internalFindAllSource()) {
            count++;

            switch(source.getIdAndType().getId()) {
                case 1:
                    Assert.assertEquals("/source", source.getPath());
                    Assert.assertEquals("blah.*", source.getFilter());
                    Assert.assertEquals(1, (source.getLocation().getId()));
                    Assert.assertEquals(SourceStatusType.SST_OK,source.getStatus());
                    break;
                case 2:
                    Assert.assertEquals("/destination", source.getPath());
                    Assert.assertEquals("", source.getFilter());
                    Assert.assertEquals(1, source.getLocation().getId());
                    Assert.assertEquals(SourceStatusType.SST_ERROR,source.getStatus());
                    break;
                case 3:
                    Assert.assertEquals("/imports", source.getPath());
                    Assert.assertEquals("", source.getFilter());
                    Assert.assertEquals(1, source.getLocation().getId());
                    Assert.assertEquals(SourceStatusType.SST_OK,source.getStatus());
                    break;
                default:
                    Assert.fail();
            }

            if(source.getIdAndType().getType().equals(FileSystemObjectType.FSO_IMPORT_SOURCE)) {
                importCount++;
            }
        }
        Assert.assertEquals(3,count);
        Assert.assertEquals(1,importCount);

        count = 0;
        for(ImportSource nextImportSource : associatedFileDataManager.internalFindAllImportSource()) {
            Assert.assertEquals(3, (long)nextImportSource.getIdAndType().getId());
            Assert.assertEquals(1, (long)nextImportSource.getDestination().getIdAndType().getId());
            count++;
        }
        Assert.assertEquals(1,count);

        // Check the FK's
        try {
            SourceDTO delete = new SourceDTO(1, "ignored");
            associatedFileDataManager.deleteSource(delete);
            Assert.fail();
        } catch(Exception e) {
            Assert.assertEquals("could not execute statement; SQL [n/a]; constraint [null]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement", e.getMessage());
        }
    }
}
