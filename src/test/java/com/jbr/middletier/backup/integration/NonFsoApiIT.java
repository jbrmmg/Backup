package com.jbr.middletier.backup.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.WebTester;
import com.jbr.middletier.backup.data.Location;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dataaccess.LocationRepository;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
import com.jbr.middletier.backup.dto.HardwareDTO;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.dto.SynchronizeDTO;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NonFsoApiIT extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(FsoApiIT.class);

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
    @Order(1)
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
        newSource1.setStatus("OK");
        newSource1.setFilter("*.xml");
        newSource1.setPath("/test/directory");
        sourceRepository.save(newSource1);

        Source newSource2 = new Source();
        newSource2.setLocation(newLocation);
        newSource2.setStatus("OK");
        newSource2.setFilter("*.xml");
        newSource2.setPath("/test/directory2");
        sourceRepository.save(newSource2);

        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(1000);

        SourceDTO destination = new SourceDTO();
        destination.setId(newSource1.getIdAndType().getId());
        destination.setLocation(locationDTO);

        SourceDTO source = new SourceDTO();
        source.setId(newSource2.getIdAndType().getId());
        source.setLocation(locationDTO);

        SynchronizeDTO newSync = new SynchronizeDTO();
        newSync.setId(1);
        newSync.setDestination(destination);
        newSync.setSource(source);

        LOG.info("Create a synchronize.");
        getMockMvc().perform(post("/jbr/ext/backup/synchronize")
                        .content(this.json(newSync))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        LOG.info("Get the synchronize that was created");
        getMockMvc().perform(get("/jbr/ext/backup/synchronize")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].source.idAndType.id", is(newSource2.getIdAndType().getId())))
                .andExpect(jsonPath("$[0].destination.idAndType.id", is(newSource1.getIdAndType().getId())));

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
                .andExpect(jsonPath("$[0].source.idAndType.id", is(newSource1.getIdAndType().getId())))
                .andExpect(jsonPath("$[0].destination.idAndType.id", is(newSource2.getIdAndType().getId())));

        LOG.info("Delete the remaining synchronize.");
        getMockMvc().perform(delete("/jbr/ext/backup/synchronize")
                        .content(this.json(newSync))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        sourceRepository.delete(newSource1);
        sourceRepository.delete(newSource2);
        locationRepository.delete(newLocation);
    }

    @Test
    @Order(2)
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
    @Order(3)
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
}
