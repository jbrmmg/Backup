package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.Location;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.exception.InvalidLocationIdException;
import com.jbr.middletier.backup.exception.SourceAlreadyExistsException;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {PrintIT.Initializer.class})
@ActiveProfiles(value="it")
public class PrintIT extends FileTester {
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

    private Source source;

    @Before
    public void setupTest() throws IOException, InvalidLocationIdException, SourceAlreadyExistsException {
        deleteDirectoryContents(new File(sourceDirectory).toPath());
        Files.createDirectories(new File(sourceDirectory).toPath());

        // Create the standard sources
        Optional<Location> existingLocation = associatedFileDataManager.findLocationById(1);
        if(existingLocation.isEmpty())
            fail();

        LocationDTO location = associatedFileDataManager.convertToDTO(existingLocation.get());
        location.setCheckDuplicates(true);
        associatedFileDataManager.updateLocation(associatedFileDataManager.convertToEntity(location));

        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setLocation(associatedFileDataManager.convertToDTO(existingLocation.get()));
        sourceDTO.setStatus("OK");
        sourceDTO.setPath(sourceDirectory);

        this.source = associatedFileDataManager.createSource(associatedFileDataManager.convertToEntity(sourceDTO));
    }

    @After
    public void cleanUpTest() {

    }

    @Test
    public void basicTest() throws Exception {
        // Copy the resource files into the source directory
        initialiseDirectories();
        List<StructureDescription> sourceDescription = getTestStructure("test2");
        copyFiles(sourceDescription, sourceDirectory);

        // Import the files.
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failed", is(false)))
                .andExpect(jsonPath("$[0].filesInserted", is(14)))
                .andExpect(jsonPath("$[0].directoriesInserted", is(11)))
                .andExpect(jsonPath("$[0].filesRemoved", is(0)))
                .andExpect(jsonPath("$[0].directoriesRemoved", is(0)))
                .andExpect(jsonPath("$[0].deletes", is(0)));

        // Select file for print.
        List<FileInfo> files = new ArrayList<>();
        List<DirectoryInfo> directories = new ArrayList<>();

        fileSystemObjectManager.loadByParent(source.getIdAndType().getId(), directories, files);
        Optional<FileInfo> testFile = Optional.empty();
        for(FileInfo nextFile : files) {
            if(nextFile.getName().equals("IMG_8231.jpg")) {
                testFile = Optional.of(nextFile);
            }
        }

        Assert.assertTrue(testFile.isPresent());

        String id = testFile.get().getIdAndType().getId().toString();
        getMockMvc().perform(post("/jbr/int/backup/print")
                    .content(id)
                    .contentType(getContentType()))
                .andExpect(status().isOk());
    }
}
