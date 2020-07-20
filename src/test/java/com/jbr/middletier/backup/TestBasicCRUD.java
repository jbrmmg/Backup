package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.dto.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestBasicCRUD extends WebTester {
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

            getMockMvc().perform(get("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            getMockMvc().perform(post("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            backup.setType("What");
            getMockMvc().perform(put("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].type",is("What")));

            getMockMvc().perform(get("/jbr/ext/backup/byId?id=TST")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(delete("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/ext/backup")
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

            getMockMvc().perform(post("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[4].id",is(6)))
                    .andExpect(jsonPath("$[4].name",is("Test")));

            location.setName("TestUpd");
            getMockMvc().perform(put("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[4].id",is(6)))
                    .andExpect(jsonPath("$[4].name",is("TestUpd")));

            getMockMvc().perform(delete("/jbr/ext/backup/location")
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

            getMockMvc().perform(post("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(33)));

            classification = new ClassificationDTO();
            classification.setId(33);
            classification.setOrder(1);
            classification.setAction("FRED2");
            classification.setUseMD5(false);

            getMockMvc().perform(put("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(33)))
                    .andExpect(jsonPath("$[32].action",is("FRED2")));


            getMockMvc().perform(delete("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(32)));
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

            getMockMvc().perform(get("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            getMockMvc().perform(post("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name",is("Testing")));

            hardware.setName("Testing2");
            getMockMvc().perform(put("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name",is("Testing2")));

            getMockMvc().perform(delete("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/ext/hardware")
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
            source.setId(1);
            source.setType("STD");
            source.setPath("C:\\Testing");
            source.setLocation(location);

            getMockMvc().perform(get("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].path",is("C:\\Testing")));

            source.setPath("C:\\Testing2");
            getMockMvc().perform(put("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].path",is("C:\\Testing2")));

            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
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
            source.setId(1);
            source.setLocation(location);
            source.setType("STD");
            source.setPath("C:\\TestSource1");

            SourceDTO destination = new SourceDTO();
            destination.setId(2);
            destination.setLocation(location);
            destination.setType("STD");
            destination.setPath("C:\\TestSource2");

            SynchronizeDTO syncrhonize = new SynchronizeDTO();
            syncrhonize.setId(1);
            syncrhonize.setSource(source);
            syncrhonize.setDestination(destination);

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(destination))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            getMockMvc().perform(post("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id",is(1)));

            getMockMvc().perform(delete("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(destination))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            getMockMvc().perform(get("/jbr/ext/backup/source")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        } catch (Exception ex) {
            fail();
        }
    }
}
