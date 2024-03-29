package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.Classification;
import com.jbr.middletier.backup.data.ClassificationActionType;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.dataaccess.ClassificationRepository;
import com.jbr.middletier.backup.dto.*;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.io.File;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("ConstantConditions")
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

    @Autowired
    ModelMapper modelMapper;

    @Test
    public void BackupCRUD() {
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

            String error = getMockMvc().perform(post("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().is(409))
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Backup with id (TST) already exists.", error);

            backup.setType("What");
            getMockMvc().perform(put("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            BackupDTO backup2 = new BackupDTO();
            backup2.setId("TSTX");
            backup2.setType("WhaT");
            error = getMockMvc().perform(put("/jbr/ext/backup")
                            .content(this.json(backup2))
                            .contentType(getContentType()))
                            .andExpect(status().isNotFound())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Backup with id (TSTX) not found.", error);

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

            error = getMockMvc().perform(get("/jbr/ext/backup/byId?id=XXX")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isNotFound())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Backup with id (XXX) not found.", error);

            getMockMvc().perform(delete("/jbr/ext/backup")
                    .content(this.json(backup))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            error = getMockMvc().perform(delete("/jbr/ext/backup")
                            .content(this.json(backup2))
                            .contentType(getContentType()))
                            .andExpect(status().isNotFound())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Backup with id (TSTX) not found.", error);

            error = getMockMvc().perform(post("/jbr/ext/backup/run")
                            .content(this.json(backup2))
                            .contentType(getContentType()))
                    .andExpect(status().isNotFound())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Backup with id () not found.", error);

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

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("CLN");
            backupDTO.setType("clean");
            backupDTO.setTime(100);
            Backup backup = modelMapper.map(backupDTO,Backup.class);

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
    public void LocationCRUD() {
        try {
            LocationDTO location = new LocationDTO();
            location.setId(6);
            location.setName("Test");
            location.setSize("1MB");
            location.setCheckDuplicates(false);

            getMockMvc().perform(post("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[4].id",is(6)))
                    .andExpect(jsonPath("$[4].name",is("Test")));

            String error = getMockMvc().perform(post("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().isConflict())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Location with id (6) already exists.", error);

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

            error = getMockMvc().perform(put("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().isNotFound())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Location with id (6) not found.", error);

            error = getMockMvc().perform(delete("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().isNotFound())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Location with id (6) not found.", error);

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
    public void ClassificationCRUD() {
        try {
            int classificationCount = (int)classificationRepository.count();

            /*
             * NOTE: assumes that classifications entered by liquibase are constant, if you add one then you
             * will need update the counts.
             */
            ClassificationDTO classification = new ClassificationDTO();
            classification.setAction(ClassificationActionType.CA_BACKUP);
            classification.setOrder(10131);
            classification.setUseMD5(true);

            getMockMvc().perform(post("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(classificationCount + 1)));

            int id = 0;
            for(Classification next: classificationRepository.findAll()) {
                if(next.getOrder().equals(10131)) {
                    id = next.getId();
                    Assert.assertTrue(next.getUseMD5());
                    Assert.assertEquals(id + "-null", next.toString());
                }
            }

            classification = new ClassificationDTO();
            classification.setId(id);
            classification.setOrder(1);
            classification.setAction(ClassificationActionType.CA_BACKUP);
            classification.setUseMD5(false);

            LOG.info("Classification {}", classification);

            getMockMvc().perform(put("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(classificationCount + 1)))
                    .andExpect(jsonPath("$..action",hasItems("CA_BACKUP")));

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

            String error = getMockMvc().perform(put("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isNotFound())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Classification with id (" + id + ") not found.", error);

            error = getMockMvc().perform(delete("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isNotFound())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Classification with id (" + id + ") not found.", error);

            error = getMockMvc().perform(post("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isConflict())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Classification must not be specified on creation", error);
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void HardwareCRUD() {
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

            String error = getMockMvc().perform(post("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isConflict())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Hardware with id (00:00:00:00:00:00) already exists.", error);

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

            HardwareDTO hardware2 = new HardwareDTO();
            hardware2.setMacAddress("00:00:00:00:00:99");
            hardware2.setReservedIP("N");
            getMockMvc().perform(put("/jbr/ext/hardware")
                            .content(this.json(hardware2))
                            .contentType(getContentType()))
                    .andExpect(status().isNotFound());

            getMockMvc().perform(get("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name",is("Testing2")));

            error = getMockMvc().perform(get("/jbr/ext/hardware/byId?macAddress=00:00:00:00:00:10")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isNotFound())
                    .andReturn().getResolvedException().getMessage();
            Assert.assertEquals("Hardware with id (00:00:00:00:00:10) not found.", error);

            getMockMvc().perform(get("/jbr/ext/hardware/byId?macAddress=00:00:00:00:00:00")
                            .content(this.json(hardware))
                            .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(delete("/jbr/ext/hardware")
                            .content(this.json(hardware))
                            .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(delete("/jbr/ext/hardware")
                            .content(this.json(hardware2))
                            .contentType(getContentType()))
                    .andExpect(status().isNotFound());

            getMockMvc().perform(get("/jbr/ext/hardware")
                    .content(this.json(hardware))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

        } catch (Exception ex) {
            fail();
        }
    }
}
