package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.dto.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestBasicCRUD {
    private MockMvc mockMvc;
    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.stream(converters)
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .orElse(null);

        assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    private String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        //noinspection unchecked
        this.mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    private MediaType getContentType() {
        return new MediaType(MediaType.APPLICATION_JSON.getType(),
                MediaType.APPLICATION_JSON.getSubtype(),
                Charset.forName("utf8"));
    }

    @Before
    public void setup() {
        // Setup the mock web context.
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void backupCRUD() {
        try {
            BackupDTO backup = new BackupDTO();
            backup.setId("TST");
            backup.setType("WhaT");
            backup.setTime(10);
            backup.setArtifact("Test");
            backup.setBackupName("Test");
            backup.setDirectory("Test");

            mockMvc.perform(get("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            mockMvc.perform(post("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            backup.setType("What");
            mockMvc.perform(put("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].type",is("What")));

            mockMvc.perform(get("/jbr/ext/backup/byId?id=TST")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void locationCRUD() {
        try {
            LocationDTO location = new LocationDTO();
            location.setId(6);
            location.setName("Test");
            location.setCheckDuplicates(false);
            location.setSize("1MB");

            mockMvc.perform(post("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[4].id",is(6)))
                    .andExpect(jsonPath("$[4].name",is("Test")));

            location.setName("TestUpd");
            mockMvc.perform(put("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[4].id",is(6)))
                    .andExpect(jsonPath("$[4].name",is("TestUpd")));

            mockMvc.perform(delete("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(4)));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void classificationCRUD() {
        try {
            /*
             * NOTE: assumes that classifications entered by liquibase are constant, if you add one then you
             * will need update the counts.
             */
            ClassificationDTO classification = new ClassificationDTO();
            classification.setAction("FRED");
            classification.setOrder(10);
            classification.setUseMD5(true);

            mockMvc.perform(post("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(34)));

            classification = new ClassificationDTO();
            classification.setId(34);
            classification.setOrder(1);
            classification.setAction("FRED2");
            classification.setUseMD5(false);

            mockMvc.perform(put("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(34)))
                    .andExpect(jsonPath("$[33].action",is("FRED2")));


            mockMvc.perform(delete("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(33)));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void hardwareCRUD() {
        try {
            HardwareDTO hardware = new HardwareDTO();
            hardware.setMacAddress("00:00:00:00:00:00");
            hardware.setReservedIP("N");
            hardware.setName("Testing");

            mockMvc.perform(get("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            mockMvc.perform(post("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name",is("Testing")));

            hardware.setName("Testing2");
            mockMvc.perform(put("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name",is("Testing2")));

            mockMvc.perform(delete("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void sourceCRUD() {
        try {
            LocationDTO location = new LocationDTO();
            location.setId(1);
            SourceDTO source = new SourceDTO();
            source.setId(3);
            source.setType("STD");
            source.setPath("C:\\Testing");
            source.setLocation(location);

            mockMvc.perform(get("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            mockMvc.perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[2].path",is("C:\\Testing")));

            source.setPath("C:\\Testing2");
            mockMvc.perform(put("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[2].path",is("C:\\Testing2")));

            mockMvc.perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void syncrhonizeCRUD() {
        try {
            LocationDTO location = new LocationDTO();
            location.setId(1);
            SourceDTO source = new SourceDTO();
            source.setId(2);
            source.setLocation(location);
            SynchronizeDTO syncrhonize = new SynchronizeDTO();
            syncrhonize.setId(2);
            syncrhonize.setSource(source);
            syncrhonize.setDestination(source);

            mockMvc.perform(get("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            mockMvc.perform(post("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[1].id",is(2)));

            mockMvc.perform(delete("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        } catch (Exception ex) {
            fail();
        }
    }
}
