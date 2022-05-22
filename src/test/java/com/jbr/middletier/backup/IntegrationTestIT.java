package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
@ContextConfiguration(initializers = {IntegrationTestIT.Initializer.class})
@ActiveProfiles(value="it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntegrationTestIT extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestIT.class);

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
    SourceRepository sourceRepository;

    @Autowired
    SynchronizeRepository synchronizeRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    DirectoryRepository directoryRepository;

    private void deleteDirectoryContents(Path path) throws IOException {
        if(!Files.exists(path))
            return;

        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    private static class StructureDescription {
        public final String filename;
        public final String directory;
        public final String destinationName;
        public final Date dateTime;

        public StructureDescription(String description) throws ParseException {
            String[] structureItems = description.split("\\s+");

            this.filename = structureItems[0];
            this.directory = structureItems[1];
            this.destinationName = structureItems[2];

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-hh-mm");
            this.dateTime = sdf.parse(structureItems[3]);
        }
    }

    private List<StructureDescription> getTestStructure(String testName) throws IOException, ParseException {
        List<StructureDescription> result = new ArrayList<>();

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("synchronise/" + testName + ".structure.txt");
        assert stream != null;
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));

        String resource;
        while((resource = br.readLine()) != null) {
            result.add(new StructureDescription(resource));
            LOG.info(resource);
        }

        return result;
    }

    private void copyFiles(List<StructureDescription> description, String destination) throws IOException {
        for(StructureDescription nextFile: description) {
            Files.createDirectories(new File(destination + "/" + nextFile.directory).toPath());

            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("synchronise/" + nextFile.filename);

            if(stream != null) {
                Path destinationFile = new File(destination + "/" + nextFile.directory + "/" + nextFile.destinationName).toPath();
                Files.copy(stream,
                        destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);

                Files.setLastModifiedTime(destinationFile, FileTime.fromMillis(nextFile.dateTime.getTime()));
            }
        }
    }

    private Location getLocation(Integer id) {
        Optional<Location> location = locationRepository.findById(id);
        if(!location.isPresent())
            fail();

        return location.get();
    }

    private Source createSource(String path, Integer locationId) {
        Source newSource = new Source();
        newSource.setLocation(getLocation(locationId));
        newSource.setPath(path);

        sourceRepository.save(newSource);

        return newSource;
    }

    @Test
    @Order(1)
    // TODO - add basic testing for source / synchronize
    public void source() throws Exception {
        LOG.info("Source Testing");

        // Use the default location (main drive).
        LocationDTO location1 = new LocationDTO();
        location1.setId(1);

        SourceDTO source = new SourceDTO();
        source.setPath("/target/testfiles/gather1");
        source.setLocation(location1);
        source.setFilter("filter");
        source.setStatus("OK");

        LOG.info("Create a source.");
        getMockMvc().perform(post("/jbr/ext/backup/source")
                        .content(this.json(source))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        LOG.info("Expect that the id is 1000000 - as that is the first.");
        getMockMvc().perform(get("/jbr/ext/backup/source")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1000000)))
                .andExpect(jsonPath("$[0].path", is("/target/testfiles/gather1")))
                .andExpect(jsonPath("$[0].filter", is("filter")))
                .andExpect(jsonPath("$[0].status", is("OK")))
                .andExpect(jsonPath("$[0].location.id", is(1)));

        // Perform an update.
        LocationDTO location2 = new LocationDTO();
        location2.setId(2);

        source.setId(1000000);
        source.setPath("/target/testfiles/gather2");
        source.setLocation(location2);
        source.setStatus("ERROR");
        source.setFilter("filter2");

        LOG.info("Update the source.");
        getMockMvc().perform(put("/jbr/ext/backup/source")
                        .content(this.json(source))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Check the update.
        getMockMvc().perform(get("/jbr/ext/backup/source")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1000000)))
                .andExpect(jsonPath("$[0].path", is("/target/testfiles/gather2")))
                .andExpect(jsonPath("$[0].filter", is("filter2")))
                .andExpect(jsonPath("$[0].status", is("ERROR")))
                .andExpect(jsonPath("$[0].location.id", is(2)));

        // Create a second source.
        LOG.info("Create another source.");
        source = new SourceDTO();
        source.setPath("/target/testfiles/gather3");
        source.setLocation(location1);
        source.setFilter("");
        source.setStatus("OK");

        getMockMvc().perform(post("/jbr/ext/backup/source")
                        .content(this.json(source))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/ext/backup/source")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Delete the original.
        LOG.info("Delete the original source.");
        source = new SourceDTO();
        source.setId(1000000);

        getMockMvc().perform(delete("/jbr/ext/backup/source")
                        .content(this.json(source))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/ext/backup/source")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1000001)));

        LOG.info("Delete the remaining source.");
        source = new SourceDTO();
        source.setId(1000001);

        getMockMvc().perform(delete("/jbr/ext/backup/source")
                        .content(this.json(source))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    public void directory() {
        LOG.info("Test the basic file object");
        Source testSource = createSource("./target/it_test/testing",1);
        DirectoryInfo directoryInfo = new DirectoryInfo();
        directoryInfo.setParent(testSource);
        directoryInfo.setName("test directory");
        directoryInfo.clearRemoved();

        directoryRepository.save(directoryInfo);
    }

    @Test
    @Order(3)
    public void synchronise() throws Exception {
        LOG.info("Synchronize Testing");

        // During this test create files in the following directories
        String sourceDirectory = "./target/it_test/source";
        deleteDirectoryContents(new File(sourceDirectory).toPath());
        Files.createDirectories(new File(sourceDirectory).toPath());

        String destinationDirectory = "./target/it_test/destination";
        deleteDirectoryContents(new File(destinationDirectory).toPath());
        Files.createDirectories(new File(destinationDirectory).toPath());

        // Copy the resource files into the source directory
        List<StructureDescription> sourceDescription = getTestStructure("test1");
        copyFiles(sourceDescription, sourceDirectory);

        // Create the source and synchronise entries
        Synchronize synchronize = new Synchronize();
        synchronize.setId(1);
        synchronize.setSource(createSource("./target/it_test/source",1));
        synchronize.setDestination(createSource("./target/it_test/destination",1));

        synchronizeRepository.save(synchronize);

        // Perform a gather.
        LOG.info("Gather the data.");
        getMockMvc().perform(post("/jbr/int/backup/gather")
                        .content(this.json("Testing"))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
    }
}
