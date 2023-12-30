package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.Location;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.dto.PrintSizeDTO;
import com.jbr.middletier.backup.dto.SelectedPrintDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.exception.InvalidLocationIdException;
import com.jbr.middletier.backup.exception.SourceAlreadyExistsException;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import com.jbr.middletier.backup.manager.PrintManager;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Autowired
    PrintManager printManager;

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
        // Remove the sources, files & directories.
        associatedFileDataManager.deleteAllSynchronize();
        fileSystemObjectManager.deleteAllFileObjects();
        associatedFileDataManager.deleteAllPreImportSource();
        associatedFileDataManager.deleteAllImportSource();
        associatedFileDataManager.deleteAllSource();
    }

    private String setupPrint(String name) throws Exception {
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
            if(nextFile.getName().equals(name)) {
                testFile = Optional.of(nextFile);
            }
        }

        Assert.assertTrue(testFile.isPresent());
        return testFile.get().getIdAndType().getId().toString();
    }

    @Test
    public void testPrintController() throws Exception {
        String id = setupPrint("IMG_8231.jpg");
        getMockMvc().perform(post("/jbr/int/backup/print")
                    .content(id)
                    .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/backup/unprint")
                        .content(id)
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/backup/print")
                        .content(id)
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/int/backup/prints")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileId",is(Integer.parseInt(id))))
                .andExpect(jsonPath("$", hasSize(1)));

        getMockMvc().perform(post("/jbr/int/backup/generate")
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(delete("/jbr/int/backup/prints")
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/int/backup/prints")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        getMockMvc().perform(get("/jbr/int/backup/print-size")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)));

        SelectedPrintDTO testUpdate = new SelectedPrintDTO();
        testUpdate.setFileId(Integer.parseInt(id));
        testUpdate.setSizeId(16);
        testUpdate.setBorder(false);
        testUpdate.setBlackWhite(false);
        getMockMvc().perform(put("/jbr/int/backup/print")
                        .content(this.json(testUpdate))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

    }

    @Test
    public void testPrintManager() throws Exception {
        String id = setupPrint("IMG_3891.jpeg");

        List<PrintSizeDTO> size = printManager.getPrintSizes();
        Assert.assertEquals(25,size.size());

        Assert.assertEquals("6x4 in",printManager.getPrintSize(12).getName());

        Assert.assertEquals(Integer.parseInt(id),(long)printManager.select(Integer.parseInt(id)));

        List<FileInfo> files = new ArrayList<>();
        List<DirectoryInfo> directories = new ArrayList<>();
        Optional<FileInfo> testFile = Optional.empty();
        fileSystemObjectManager.loadByParent(source.getIdAndType().getId(), directories, files);
        int nonExistentId = 1;
        boolean clash = true;
        while(clash) {
            clash = false;
            for (FileInfo nextFile : files) {
                if(testFile.isEmpty()) {
                    if(nextFile.getName().equalsIgnoreCase("IMG_3891.jpeg")) {
                        testFile = Optional.of(nextFile);
                    }
                }
                if(nextFile.getIdAndType().getId().equals(nonExistentId)) {
                    clash = true;
                    break;
                }
            }

            if(clash) {
                nonExistentId++;
            }
        }
        Assert.assertFalse(testFile.isEmpty());
        Assert.assertNull(printManager.select(nonExistentId));
        Assert.assertNull(printManager.unselect(nonExistentId));
        printManager.deletePrints();

        testFile.get().setFlags("P");
        fileSystemObjectManager.save(testFile.get());

        List<SelectedPrintDTO> prints = printManager.getPrints();
        Assert.assertEquals(1,prints.size());

        printManager.deletePrints();
    }
}
