package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.WebTester;
import com.jbr.middletier.backup.data.ClassificationActionType;
import com.jbr.middletier.backup.data.Location;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.data.SourceStatusType;
import com.jbr.middletier.backup.dataaccess.LocationRepository;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
import com.jbr.middletier.backup.dto.*;
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

import java.util.Objects;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {NonFsoApiIT.Initializer.class})
@ActiveProfiles(value="it")
public class NonFsoApiIT extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(FsoApiIT.class);

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
    SourceRepository sourceRepository;

    @Autowired
    LocationRepository locationRepository;

    @Test
    public void synchronizeApi() throws Exception {
        LOG.info("Synchronize API Testing");

        // Setup basic source & location.
        Location newLocation = new Location();
        newLocation.setId(1000);
        newLocation.setName("Test");
        newLocation.setName("1GB");
        locationRepository.save(newLocation);

        Source newSource1 = new Source();
        newSource1.setLocation(newLocation);
        newSource1.setStatus(SourceStatusType.SST_OK);
        newSource1.setFilter("*.xml");
        newSource1.setPath("/test/directory");
        sourceRepository.save(newSource1);

        Source newSource2 = new Source();
        newSource2.setLocation(newLocation);
        newSource2.setStatus(SourceStatusType.SST_OK);
        newSource2.setFilter("*.xml");
        newSource2.setPath("/test/directory2");
        sourceRepository.save(newSource2);

        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1000);

        SourceDTO destination = new SourceDTO();
        destination.setId(newSource1.getIdAndType().getId());
        destination.setLocation(locationDTO);
        destination.setStatus("OK");

        SourceDTO source = new SourceDTO();
        source.setId(newSource2.getIdAndType().getId());
        source.setLocation(locationDTO);
        source.setStatus("OK");

        SynchronizeDTO newSync = new SynchronizeDTO();
        newSync.setId(1);
        newSync.setDestination(destination);
        newSync.setSource(source);

        LOG.info("Create a synchronize.");
        getMockMvc().perform(post("/jbr/ext/backup/synchronize")
                        .content(this.json(newSync))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Check can't create it again.
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/ext/backup/synchronize")
                        .content(this.json(newSync))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Synchronize with id (1) already exists.", error);

        LOG.info("Get the synchronize that was created");
        getMockMvc().perform(get("/jbr/ext/backup/synchronize")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].source.id", is(newSource2.getIdAndType().getId())))
                .andExpect(jsonPath("$[0].destination.id", is(newSource1.getIdAndType().getId())));

        LOG.info("Update the source (switch source and destination.");
        newSync.setDestination(source);
        newSync.setSource(destination);

        getMockMvc().perform(put("/jbr/ext/backup/synchronize")
                        .content(this.json(newSync))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        LOG.info("Get the synchronize that was created (again)");
        getMockMvc().perform(get("/jbr/ext/backup/synchronize")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].source.id", is(newSource1.getIdAndType().getId())))
                .andExpect(jsonPath("$[0].destination.id", is(newSource2.getIdAndType().getId())));

        LOG.info("Delete the remaining synchronize.");
        getMockMvc().perform(delete("/jbr/ext/backup/synchronize")
                        .content(this.json(newSync))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        error = Objects.requireNonNull(getMockMvc().perform(delete("/jbr/ext/backup/synchronize")
                        .content(this.json(newSync))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Synchronize with id (1) not found.", error);

        sourceRepository.delete(newSource1);
        sourceRepository.delete(newSource2);
        locationRepository.delete(newLocation);
    }

    @Test
    public void locationApi() throws Exception {
        LocationDTO location = new LocationDTO();
        location.setId(10);
        location.setName("Test");
        location.setSize("1GB");

        LOG.info("Create a location.");
        getMockMvc().perform(post("/jbr/ext/backup/location")
                        .content(this.json(location))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        LOG.info("Get the location that was created");
        getMockMvc().perform(get("/jbr/ext/backup/location")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[4].id", is(location.getId())))
                .andExpect(jsonPath("$[4].name", is(location.getName())))
                .andExpect(jsonPath("$[4].size", is(location.getSize())));

        LOG.info("Modify the location.");
        location.setSize("2GB");
        getMockMvc().perform(put("/jbr/ext/backup/location")
                        .content(this.json(location))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/ext/backup/location")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[4].size", is(location.getSize())));

        LOG.info("Delete the location.");
        getMockMvc().perform(delete("/jbr/ext/backup/location")
                        .content(this.json(location))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    public void hardwareApi() throws Exception {
        HardwareDTO hardwareDTO = new HardwareDTO();
        hardwareDTO.setMacAddress("01:02:04:06:A2:49");
        hardwareDTO.setName("Test");
        hardwareDTO.setIp("12.231.9.2");
        hardwareDTO.setReservedIP("Y");

        LOG.info("Create a hardware.");
        getMockMvc().perform(post("/jbr/ext/hardware")
                        .content(this.json(hardwareDTO))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        LOG.info("Get the hardware that was created");
        getMockMvc().perform(get("/jbr/ext/hardware")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].macAddress", is(hardwareDTO.getMacAddress())))
                .andExpect(jsonPath("$[0].name", is(hardwareDTO.getName())))
                .andExpect(jsonPath("$[0].ip", is(hardwareDTO.getIp())))
                .andExpect(jsonPath("$[0].reservedIP", is(hardwareDTO.getReservedIP())));

        LOG.info("Get the hardware that was created");
        getMockMvc().perform(get("/jbr/ext/hardware/byId?macAddress=" + hardwareDTO.getMacAddress())
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("macAddress", is(hardwareDTO.getMacAddress())))
                .andExpect(jsonPath("name", is(hardwareDTO.getName())))
                .andExpect(jsonPath("ip", is(hardwareDTO.getIp())))
                .andExpect(jsonPath("reservedIP", is(hardwareDTO.getReservedIP())));

        LOG.info("Modify the hardware.");
        hardwareDTO.setIp("12.231.9.22");
        getMockMvc().perform(put("/jbr/ext/hardware")
                        .content(this.json(hardwareDTO))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/ext/hardware")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ip", is(hardwareDTO.getIp())));

        LOG.info("Delete the hardware.");
        getMockMvc().perform(delete("/jbr/ext/hardware")
                        .content(this.json(hardwareDTO))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
    }

    @Test
    public void backupApi() throws Exception {
        BackupDTO backupDTO = new BackupDTO();
        backupDTO.setId("TXST");
        backupDTO.setTime(100);
        backupDTO.setBackupName("Test");
        backupDTO.setArtifact("Alpha");
        backupDTO.setDirectory("/test");
        backupDTO.setFileName("fred.txt");
        backupDTO.setType("XXXX");

        LOG.info("Create a backup.");
        getMockMvc().perform(post("/jbr/ext/backup")
                        .content(this.json(backupDTO))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        LOG.info("Get the hardware that was created");
        getMockMvc().perform(get("/jbr/ext/backup")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(backupDTO.getId())))
                .andExpect(jsonPath("$[0].time", is((int)backupDTO.getTime())))
                .andExpect(jsonPath("$[0].backupName", is(backupDTO.getBackupName())))
                .andExpect(jsonPath("$[0].artifact", is(backupDTO.getArtifact())))
                .andExpect(jsonPath("$[0].directory", is(backupDTO.getDirectory())))
                .andExpect(jsonPath("$[0].fileName", is(backupDTO.getFileName())))
                .andExpect(jsonPath("$[0].type", is(backupDTO.getType())));

        LOG.info("Get the hardware that was created");
        getMockMvc().perform(get("/jbr/ext/backup/byId?id=" + backupDTO.getId())
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(backupDTO.getId())))
                .andExpect(jsonPath("time", is((int)backupDTO.getTime())))
                .andExpect(jsonPath("backupName", is(backupDTO.getBackupName())))
                .andExpect(jsonPath("artifact", is(backupDTO.getArtifact())))
                .andExpect(jsonPath("directory", is(backupDTO.getDirectory())))
                .andExpect(jsonPath("fileName", is(backupDTO.getFileName())))
                .andExpect(jsonPath("type", is(backupDTO.getType())));

        LOG.info("Modify the hardware.");
        backupDTO.setFileName("fred.prep.txt");
        getMockMvc().perform(put("/jbr/ext/backup")
                        .content(this.json(backupDTO))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/ext/backup")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName", is(backupDTO.getFileName())));

        LOG.info("Delete the hardware.");
        getMockMvc().perform(delete("/jbr/ext/backup")
                        .content(this.json(backupDTO))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
    }

    @Test
    public void classificationApi() throws Exception {
        // TODO - do not assume the size is 33 - work it out.
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setIsVideo(false);
        classificationDTO.setOrder(33);
        classificationDTO.setUseMD5(true);
        classificationDTO.setAction(ClassificationActionType.CA_BACKUP);
        classificationDTO.setRegex("*/sdaf");
        classificationDTO.setIcon("Flahr");
        classificationDTO.setIsImage(true);

        LOG.info("Create a classification.");
        getMockMvc().perform(post("/jbr/ext/backup/classification")
                        .content(this.json(classificationDTO))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        LOG.info("Get the classification that was created");
        getMockMvc().perform(get("/jbr/ext/backup/classification")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(33)))
                .andExpect(jsonPath("$[32].id", is(33)))
                .andExpect(jsonPath("$[32].action", is(classificationDTO.getAction().toString())))
                .andExpect(jsonPath("$[32].useMD5", is(classificationDTO.getUseMD5())))
                .andExpect(jsonPath("$[32].regex", is(classificationDTO.getRegex())))
                .andExpect(jsonPath("$[32].isVideo", is(classificationDTO.getIsVideo())))
                .andExpect(jsonPath("$[32].icon", is(classificationDTO.getIcon())))
                .andExpect(jsonPath("$[32].isImage", is(classificationDTO.getIsImage())));

        LOG.info("Modify the classification.");
        classificationDTO.setId(33);
        classificationDTO.setIcon("FlahrXX");
        getMockMvc().perform(put("/jbr/ext/backup/classification")
                        .content(this.json(classificationDTO))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/ext/backup/classification")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[32].icon", is(classificationDTO.getIcon())));

        LOG.info("Delete the hardware.");
        getMockMvc().perform(delete("/jbr/ext/backup/classification")
                        .content(this.json(classificationDTO))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
    }
}
