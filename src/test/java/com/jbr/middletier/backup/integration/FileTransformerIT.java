package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MiddleTier.class)
@ContextConfiguration(initializers = {FileTransformerIT.Initializer.class})
@ActiveProfiles(value = "it")
@Testcontainers
public class FileTransformerIT extends FileTester {
    private static final Logger LOG = LoggerFactory.getLogger(FileTransformerIT.class);

    @SuppressWarnings("rawtypes")
    @Container
    public static MySQLContainer mysqlContainer = new MySQLContainer("mysql:8.0.28")
            .withDatabaseName("integration-tests-db")
            .withUsername("sa")
            .withPassword("sa");

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
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

    @BeforeEach
    void setup() throws Exception {
        Files.createDirectories(new File(SOURCE_DIRECTORY).toPath());

        addClassification(associatedFileDataManager, ".*\\.stl$", ClassificationActionType.CA_BACKUP, 50, true, false, "stl");

        Optional<Location> location = associatedFileDataManager.findLocationById(1);
        assertTrue(location.isPresent());

        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setLocation(associatedFileDataManager.convertToDTO(location.get()));
        sourceDTO.setStatus("OK");
        sourceDTO.setPath(SOURCE_DIRECTORY);

        this.source = associatedFileDataManager.createSource(associatedFileDataManager.convertToEntity(sourceDTO));

        InputStream stlStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("synchronise/Test.stl");
        assert stlStream != null;
        Files.copy(stlStream, new File(SOURCE_DIRECTORY + "/Test.stl").toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @AfterEach
    void cleanup() throws IOException {
        deleteDirectoryContents(new File(SOURCE_DIRECTORY).toPath());
    }

    @Test
    void fileImageEndpointRunsTransformerAndReturnsImage() throws Exception {
        LOG.info("Testing image transformer via fileImage endpoint");

        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        List<FileInfo> files = new ArrayList<>();
        List<DirectoryInfo> directories = new ArrayList<>();
        fileSystemObjectManager.loadByParent(this.source.getIdAndType().getId(), directories, files);

        Optional<FileInfo> stlFile = files.stream()
                .filter(f -> f.getName().equals("Test.stl"))
                .findFirst();
        assertTrue(stlFile.isPresent(), "STL file should be gathered into the database");

        byte[] result = getMockMvc().perform(get("/jbr/int/backup/fileImage?id=" + stlFile.get().getIdAndType().getId())
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertTrue(result.length > 0, "Response should contain image bytes from the transformer");
    }

    @Test
    void fileImageEndpointReturnsNotFoundForInvalidId() throws Exception {
        getMockMvc().perform(get("/jbr/int/backup/fileImage?id=99999")
                        .contentType(getContentType()))
                .andExpect(status().isNotFound());
    }
}
