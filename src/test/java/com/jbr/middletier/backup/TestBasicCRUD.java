package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.Classification;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.dataaccess.ClassificationRepository;
import com.jbr.middletier.backup.dto.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBasicCRUD extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(TestBasicCRUD.class);

    @Autowired
    ClassificationRepository classificationRepository;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    BackupRepository backupRepository;

    @Test
    public void backupCRUD() {
        try {
            BackupDTO backup = new BackupDTO("TST","WhaT");
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

            getMockMvc().perform(post("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().is(409));

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

            getMockMvc().perform(get("/jbr/ext/backup/byId?id=XXX")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

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
    public void TestCleanBackupDirect() {
        try {
            // Setup the test
            File backupDirectory = new File(applicationProperties.getDirectory().getName());
            if (!backupDirectory.exists() && !backupDirectory.mkdirs()) {
                LOG.warn("Cannot create the backup directory.");
            }

            File testFile = new File(applicationProperties.getDirectory().getName() + "/2020-01-01");
            if (!testFile.exists() && !testFile.mkdir()) {
                fail();
            }

            BackupDTO backupDTO = new BackupDTO("CLN","clean");
            backupDTO.setTime(100);
            Backup backup = new Backup(backupDTO);

            backupRepository.save(backup);

            getMockMvc().perform(post("/jbr/ext/backup/run?id=" + backup.getId())
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            assertFalse(testFile.exists());

            backupRepository.deleteAll();
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    public void locationCRUD() {
        try {
            LocationDTO location = new LocationDTO(6,"Test", "1MB");
            location.setCheckDuplicates(false);

            getMockMvc().perform(post("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[4].id",is(6)))
                    .andExpect(jsonPath("$[4].name",is("Test")));

            getMockMvc().perform(post("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().is(409));

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

            getMockMvc().perform(put("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            getMockMvc().perform(delete("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            getMockMvc().perform(get("/jbr/ext/backup/location")
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
            int classificationCount = (int)classificationRepository.count();

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
                    .andExpect(jsonPath("$", hasSize(classificationCount + 1)));

            int id = 0;
            for(Classification next: classificationRepository.findAll()) {
                if(next.getAction().equals("FRED")) {
                    id = next.getId();
                }
            }

            classification = new ClassificationDTO();
            classification.setId(id);
            classification.setOrder(1);
            classification.setAction("FRED2");
            classification.setUseMD5(false);

            LOG.info("Classification {}", classification);

            getMockMvc().perform(put("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(classificationCount + 1)))
                    .andExpect(jsonPath("$..action",hasItems("FRED2")));

            getMockMvc().perform(get("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(classificationCount + 1)));

            getMockMvc().perform(delete("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(classificationCount)));

            getMockMvc().perform(put("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            getMockMvc().perform(delete("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            getMockMvc().perform(post("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().is(409));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void hardwareCRUD() {
        try {
            HardwareDTO hardware = new HardwareDTO("00:00:00:00:00:00","N");
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

            getMockMvc().perform(post("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().is(409));

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


            getMockMvc().perform(get("/jbr/ext/hardware/byId?macAddress=00:00:00:00:00:10")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

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
            LocationDTO location = new LocationDTO(1,"Test", "1MB");
            SourceDTO source = new SourceDTO(1,"C:\\Testing");
            source.setType("STD");
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

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().is(409));

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

            getMockMvc().perform(put("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void syncrhonizeCRUD() {
        try {
            LocationDTO location = new LocationDTO(1, "Test", "1MB");

            SourceDTO source = new SourceDTO(1, "C:\\TestSource1");
            source.setLocation(location);
            source.setType("STD");

            SourceDTO destination = new SourceDTO(2,"C:\\TestSource2");
            destination.setLocation(location);
            destination.setType("STD");

            SynchronizeDTO syncrhonize = new SynchronizeDTO(1);
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

            getMockMvc().perform(post("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().is(409));

            getMockMvc().perform(put("/jbr/ext/backup/synchronize")
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

            getMockMvc().perform(put("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            getMockMvc().perform(delete("/jbr/ext/backup/synchronize")
                    .content(this.json(syncrhonize))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

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
