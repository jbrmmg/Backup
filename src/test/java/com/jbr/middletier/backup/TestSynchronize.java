package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.ActionConfirm;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.dto.SynchronizeDTO;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestSynchronize extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronize.class);

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DirectoryRepository directoryRepository;

    @Autowired
    private ActionConfirmRepository actionConfirmRepository;

    @Test
    public void testSynchronization() {
        try {
            LOG.info("Test Synch");

            // get the current working directory.
            String cwd = System.getProperty("user.dir");

            // Setup a directory structure.
            //   ./target/testfiles/gather
            //                       fileA.txt
            //                       fileB.txt

            File testPath = new File("./target/testfiles/gather1");
            if (testPath.exists()) {
                FileUtils.cleanDirectory(testPath);
                assertTrue(testPath.delete());
            }

            assertTrue(testPath.mkdirs());

            File subPath = new File("./target/testfiles/gather1/Sub");
            assertTrue(subPath.mkdir());

            File testFileA = new File("./target/testfiles/gather1/Sub/fileA.txt");
            assertTrue(testFileA.createNewFile());

            File testFileB = new File("./target/testfiles/gather1/Sub/fileB.txt");
            assertTrue(testFileB.createNewFile());

            File testFileC = new File("./target/testfiles/gather1/Sub/fileC.txt");
            assertTrue(testFileC.createNewFile());

            File lockFile = new File("./target/testfiles/gather1/Sub/.~lock.File.ods#");
            assertTrue(lockFile.createNewFile());

            File deleteFile = new File("./target/testfiles/gather1/Sub/.ds_store");
            assertTrue(deleteFile.createNewFile());

            File testPath2 = new File("./target/testfiles/gather2");
            if (testPath2.exists()) {
                FileUtils.cleanDirectory(testPath2);
                assertTrue(testPath2.delete());
            }

            assertTrue(testPath2.mkdirs());

            File subPath2 = new File("./target/testfiles/gather2/Sub");
            assertTrue(subPath2.mkdir());

            File testFileA2 = new File("./target/testfiles/gather2/Sub/fileA.txt");
            assertTrue(testFileA2.createNewFile());

            File testFileB2 = new File("./target/testfiles/gather2/Sub/fileB.txt");
            assertTrue(testFileB2.createNewFile());

            File testFileC2 = new File("./target/testfiles/gather2/Sub/fileC.txt");
            assertFalse(testFileC2.exists());

            File testFileD2 = new File("./target/testfiles/gather2/Sub/fileD.txt");
            assertTrue(testFileD2.createNewFile());

            File lockFile2 = new File("./target/testfiles/gather2/Sub/.~lock.File.ods#");
            assertFalse(lockFile2.exists());

            // Setup a new source
            LocationDTO location = new LocationDTO();
            location.setId(1);
            SourceDTO source = new SourceDTO();
            source.setId(1);
            source.setType("STD");
            source.setPath(cwd + "/target/testfiles/gather1");
            source.setLocation(location);

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            SourceDTO source2 = new SourceDTO();
            source2.setId(2);
            source2.setType("STD");
            source2.setPath(cwd + "/target/testfiles/gather2");
            source2.setLocation(location);

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source2))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            SynchronizeDTO synchronize = new SynchronizeDTO(1);
            synchronize.setSource(source);
            synchronize.setDestination(source2);

            getMockMvc().perform(post("/jbr/ext/backup/synchronize")
                    .content(this.json(synchronize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            // Perform the gather
            String temp = "testing";
            getMockMvc().perform(post("/jbr/int/backup/gather")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/files")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(12)));

            getMockMvc().perform(post("/jbr/int/backup/sync")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/int/backup/gather")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/files")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(13)));
            assertTrue(testFileC2.exists());
            assertFalse(lockFile2.exists());

            // Confirm the actions
            for(ActionConfirm next: actionConfirmRepository.findAll()) {
                next.setConfirmed(true);
                actionConfirmRepository.save(next);
            }

            getMockMvc().perform(post("/jbr/int/backup/sync")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/int/backup/gather")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/files")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(11)));
            assertTrue(testFileC2.exists());
            assertFalse(lockFile2.exists());
            assertFalse(deleteFile.exists());
            assertFalse(testFileD2.exists());

            // Clear out the data.
            actionConfirmRepository.deleteAll();
            fileRepository.deleteAll();
            directoryRepository.deleteAll();

            getMockMvc().perform(delete("/jbr/ext/backup/synchronize")
                    .content(this.json(synchronize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source2))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/files")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        } catch(Exception ex) {
            fail();
        }
    }
}
