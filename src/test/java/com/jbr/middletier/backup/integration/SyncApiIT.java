package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.WebTester;
import com.jbr.middletier.backup.data.Location;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.data.Synchronize;
import com.jbr.middletier.backup.dataaccess.LocationRepository;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
import com.jbr.middletier.backup.dataaccess.SynchronizeRepository;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {SyncApiIT.Initializer.class})
@ActiveProfiles(value="it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SyncApiIT extends WebTester  {
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
    SourceRepository sourceRepository;

    @Autowired
    SynchronizeRepository synchronizeRepository;

    @Autowired
    LocationRepository locationRepository;

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

    @Test
    @Order(1)
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

        // TODO - check what was gathered was expected.
    }
}
