package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.ActionConfirm;
import com.jbr.middletier.backup.data.Classification;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.dto.SynchronizeDTO;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
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
import java.util.Calendar;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSynchronize extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronize.class);

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DirectoryRepository directoryRepository;

    @Autowired
    private ActionConfirmRepository actionConfirmRepository;

    @Autowired
    private ClassificationRepository classificationRepository;

    @Before
    public void setupFolderClassification() {
        ClassificationDTO classification = new ClassificationDTO();
        classification.setOrder(1);
        classification.setRegex("^\\.$");
        classification.setAction("FOLDER");
        classification.setImage(false);
        classification.setVideo(false);
        classification.setUseMD5(false);

        classificationRepository.save(new Classification(classification));

        classification = new ClassificationDTO();
        classification.setOrder(1);
        classification.setRegex(".*\\.pdfx$");
        classification.setAction("BACKUP");
        classification.setImage(false);
        classification.setVideo(false);
        classification.setUseMD5(true);

        classificationRepository.save(new Classification(classification));
    }

    @Test
    public void TestSynchronization() {
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

            File testFileUnk = new File("./target/testfiles/gather1/Sub/fileC.txtx");
            assertTrue(testFileUnk.createNewFile());

            File testFileCbup = new File("./target/testfiles/gather1/Sub/fileC.txt~");
            assertTrue(testFileCbup.createNewFile());

            File testFileMd5 = new File("./target/testfiles/gather1/Sub/useMD5.pdfx");
            assertTrue(testFileMd5.createNewFile());

            Calendar calendar = Calendar.getInstance();
            calendar.set(2020, Calendar.JANUARY, 1);
            assertTrue(testFileMd5.setLastModified(calendar.getTimeInMillis()));

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

            File testFileUnkd = new File("./target/testfiles/gather2/Sub/fileC.txtx");
            assertFalse(testFileUnkd.exists());

            File testFileC2bup = new File("./target/testfiles/gather2/Sub/fileC.txt~");
            assertTrue(testFileC2bup.createNewFile());

            File testFileMd52 = new File("./target/testfiles/gather2/Sub/useMD5.pdfx");
            assertTrue(testFileMd52.createNewFile());
            assertNotEquals(testFileMd52.lastModified(),testFileMd5.lastModified());

            File testFileD2 = new File("./target/testfiles/gather2/Sub/fileD.txt");
            assertTrue(testFileD2.createNewFile());

            File testFileE2 = new File("./target/testfiles/gather2/Sub/fileE.txtx");
            assertTrue(testFileE2.createNewFile());

            File lockFile2 = new File("./target/testfiles/gather2/Sub/.~lock.File.ods#");
            assertFalse(lockFile2.exists());

            File testDirectory = new File("./target/testfiles/gather2/Sub2");
            assertTrue(testDirectory.mkdir());

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
                    .andExpect(jsonPath("$", hasSize(19)));

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
                    .andExpect(jsonPath("$", hasSize(20)));
            assertTrue(testFileC2.exists());
            assertFalse(lockFile2.exists());
            assertFalse(testFileUnkd.exists());

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
                    .andExpect(jsonPath("$", hasSize(15)));
            assertTrue(testFileC2.exists());
            assertFalse(lockFile2.exists());
            assertFalse(deleteFile.exists());
            assertFalse(testFileD2.exists());
            assertFalse(testFileE2.exists());
            assertFalse(testDirectory.exists());
            assertFalse(testFileC2bup.exists());
            assertEquals(testFileMd52.lastModified(),testFileMd5.lastModified());

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

    @Test
    public void TestSourceStatusNull() {
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

            // Setup a new source
            LocationDTO location = new LocationDTO();
            location.setId(1);
            SourceDTO source = new SourceDTO();
            source.setId(1);
            source.setType("STD");
            source.setPath(cwd + "/target/testfiles/gather1");
            source.setLocation(location);
            source.setStatus(null);

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            SourceDTO source2 = new SourceDTO();
            source2.setId(2);
            source2.setType("STD");
            source2.setPath(cwd + "/target/testfiles/gather2");
            source2.setLocation(location);
            source2.setStatus(null);

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

            String temp = "";
            getMockMvc().perform(post("/jbr/int/backup/sync")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            source.setStatus("OK");
            getMockMvc().perform(put("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/int/backup/sync")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

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
        } catch(Exception ex) {
            fail();
        }
    }

    @Test
    public void TestSynchronizeUnknownAction() {
        try {
            LOG.info("Test Synch");

            // get the current working directory.
            String cwd = System.getProperty("user.dir");

            // Setup the bad action.
            ClassificationDTO classification = new ClassificationDTO();
            classification.setOrder(1);
            classification.setRegex(".*\\.txtxx~$");
            classification.setAction("BLAHBLAH");
            classification.setImage(false);
            classification.setVideo(false);
            classification.setUseMD5(false);

            getMockMvc().perform(post("/jbr/ext/backup/classification")
                    .content(this.json(classification))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

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

            File testFileA = new File("./target/testfiles/gather1/Sub/fileA.txtxx~");
            assertTrue(testFileA.createNewFile());

            File testPath2 = new File("./target/testfiles/gather2");
            if (testPath2.exists()) {
                FileUtils.cleanDirectory(testPath2);
                assertTrue(testPath2.delete());
            }

            assertTrue(testPath2.mkdirs());

            File subPath2 = new File("./target/testfiles/gather2/Sub");
            assertTrue(subPath2.mkdir());

            File testFileA2 = new File("./target/testfiles/gather2/Sub/fileB.txtxx~");
            assertTrue(testFileA2.createNewFile());

            // Setup a new source
            LocationDTO location = new LocationDTO();
            location.setId(1);
            SourceDTO source = new SourceDTO();
            source.setId(1);
            source.setType("STD");
            source.setPath(cwd + "/target/testfiles/gather1");
            source.setLocation(location);
            source.setStatus("OK");

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            SourceDTO source2 = new SourceDTO();
            source2.setId(2);
            source2.setType("STD");
            source2.setPath(cwd + "/target/testfiles/gather2");
            source2.setLocation(location);
            source2.setStatus("OK");

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

            String temp = "";
            getMockMvc().perform(post("/jbr/int/backup/gather")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/int/backup/sync")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(delete("/jbr/ext/backup/synchronize")
                    .content(this.json(synchronize))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            // Clear out the data.
            actionConfirmRepository.deleteAll();
            fileRepository.deleteAll();
            directoryRepository.deleteAll();

            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source2))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());
        } catch(Exception ex) {
            fail();
        }
    }
}
