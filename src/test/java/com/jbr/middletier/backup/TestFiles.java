package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.dto.SynchronizeDTO;
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
import org.apache.commons.io.FileUtils;

import java.io.File;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFiles extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(TestFiles.class);

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DirectoryRepository directoryRepository;

    @Autowired
    private ActionConfirmRepository actionConfirmRepository;

    @Autowired
    private IgnoreFileRepository ignoreFileRepository;

    @Autowired
    private ImportFileRepository importFileRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Test
    public void TestGather() {
        try {
            LOG.info("Test Gather");
            // Setup a directory structure.
            //   ./target/testfiles/gather
            //                       fileA.txt
            //                       fileB.txt

            File testPath = new File("./target/testfiles/gather");
            if (testPath.exists()) {
                FileUtils.cleanDirectory(testPath);
                assertTrue(testPath.delete());
            }

            assertTrue(testPath.mkdirs());

            File testFileA = new File("./target/testfiles/gather/fileA.txt");
            assertTrue(testFileA.createNewFile());

            File testFileB = new File("./target/testfiles/gather/fileB.txt");
            assertTrue(testFileB.createNewFile());

            // Setup a new source
            LocationDTO location = new LocationDTO();
            location.setId(1);
            SourceDTO source = new SourceDTO();
            source.setId(1);
            source.setType("STD");
            source.setPath("./target/testfiles/gather");
            source.setLocation(location);

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
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
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$..name", hasItems(".","fileA.txt", "fileB.txt")));

            // Remove the files
            assertTrue(testFileA.delete());
            assertTrue(testFileB.delete());

            // Perform the gather
            getMockMvc().perform(post("/jbr/int/backup/gather")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/files")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is(".")));

            // Clear out the data.
            fileRepository.deleteAll();
            directoryRepository.deleteAll();

            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/files")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void TestFileWeb() {
        try {
            LOG.info("Test File Web");
            for(Source next: sourceRepository.findAll()) {
                LOG.info("Source {}",next.getId());
            }

            // get the current working directory.
            String cwd = System.getProperty("user.dir");

            // Setup a directory structure.
            //   ./target/testfiles/gather
            //                       fileA.mp4
            //                       fileB.jpg

            File testPath = new File("./target/testfiles/gather");
            if (testPath.exists()) {
                FileUtils.cleanDirectory(testPath);
                assertTrue(testPath.delete());
            }

            assertTrue(testPath.mkdirs());

            File testFileA = new File("./target/testfiles/gather/fileA.jpg");
            assertTrue(testFileA.createNewFile());

            File testFileB = new File("./target/testfiles/gather/fileB.mov");
            assertTrue(testFileB.createNewFile());

            // Setup a new source
            LocationDTO location = new LocationDTO();
            location.setId(1);
            SourceDTO source = new SourceDTO();
            source.setId(1);
            source.setType("STD");
            source.setPath(cwd + "/target/testfiles/gather");
            source.setLocation(location);

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            // Modify the classifications.
            ClassificationDTO jpgClass = new ClassificationDTO();
            jpgClass.setId(4);
            jpgClass.setOrder(4);
            jpgClass.setRegex(".*\\.jpg$");
            jpgClass.setAction("BACKUP");
            jpgClass.setImage(true);
            jpgClass.setVideo(true);
            jpgClass.setUseMD5(false);

            getMockMvc().perform(put("/jbr/ext/backup/classification")
                    .content(this.json(jpgClass))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            // Perform the gather
            String temp = "testing";
            getMockMvc().perform(post("/jbr/int/backup/gather")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            // Find the id of the file called FileA.jpg
            int id1 = -1;
            int id2 = -1;
            for (FileInfo next : fileRepository.findAll()) {
                if (next.getName().equals("fileA.jpg")) {
                    id1 = next.getId();
                }
                if (next.getName().equals("fileB.mov")) {
                    id2 = next.getId();
                }
            }

            getMockMvc().perform(get("/jbr/int/backup/file?id=" + id1)
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("file.id", is(id1)))
                    .andExpect(jsonPath("file.name", is("fileA.jpg")));

            getMockMvc().perform(get("/jbr/int/backup/fileImage?id=" + id1)
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/fileVideo?id=" + id1)
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/fileVideo?id=" + id2)
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().is(400));

            getMockMvc().perform(delete("/jbr/int/backup/file?id=" + id1)
                    .content(this.json(temp))
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

            getMockMvc().perform(get("/jbr/int/backup/files")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            getMockMvc().perform(get("/jbr/int/backup/fileVideo?id=3")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            getMockMvc().perform(get("/jbr/int/backup/fileImage?id=3")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            getMockMvc().perform(get("/jbr/int/backup/file?id=3")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            getMockMvc().perform(delete("/jbr/int/backup/file?id=3")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void TestDuplicate() {
        try {
            LOG.info("Test Duplicate");

            // get the current working directory.
            String cwd = System.getProperty("user.dir");

            // Setup a directory structure.
            //   ./target/testfiles/gather
            //                       fileA.txt
            //                       fileB.txt

            File testPath = new File("./target/testfiles/duplicate");
            if (testPath.exists()) {
                FileUtils.cleanDirectory(testPath);
                assertTrue(testPath.delete());
            }

            assertTrue(testPath.mkdirs());

            File subPath = new File("./target/testfiles/duplicate/Sub");
            assertTrue(subPath.mkdir());

            File subPath2 = new File("./target/testfiles/duplicate/Sub2");
            assertTrue(subPath2.mkdir());

            File testFileA = new File("./target/testfiles/duplicate/Sub/fileA.txt");
            assertTrue(testFileA.createNewFile());

            File testFileB = new File("./target/testfiles/duplicate/Sub/fileB.txt");
            assertTrue(testFileB.createNewFile());

            File testFileC = new File("./target/testfiles/duplicate/Sub/fileC.txt");
            assertTrue(testFileC.createNewFile());

            File testFileB2 = new File("./target/testfiles/duplicate/Sub2/fileB.txt");
            assertTrue(testFileB2.createNewFile());

            // Setup a new source
            LocationDTO location = new LocationDTO(1,"Main Shared Drive", "1.8TB");
            location.setCheckDuplicates(true);

            SourceDTO source = new SourceDTO();
            source.setId(1);
            source.setType("STD");
            source.setPath(cwd + "/target/testfiles/duplicate");
            source.setLocation(location);

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(put("/jbr/ext/backup/location")
                    .content(this.json(location))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            // Perform the gather
            String temp = "testing";
            getMockMvc().perform(post("/jbr/int/backup/gather")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/int/backup/duplicate")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/actions")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            // Clear out the data.
            actionConfirmRepository.deleteAll();
            ignoreFileRepository.deleteAll();
            importFileRepository.deleteAll();
            fileRepository.deleteAll();
            directoryRepository.deleteAll();

            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            temp = "testing";
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
    public void TestHierarchy() {
        try {
            LOG.info("Test Hierarchy");

            // get the current working directory.
            String cwd = System.getProperty("user.dir");

            // Setup a directory structure.
            //   ./target/testfiles/gather
            //                       fileA.txt
            //                       fileB.txt

            File testPath = new File("./target/testfiles/duplicate");
            if (testPath.exists()) {
                FileUtils.cleanDirectory(testPath);
                assertTrue(testPath.delete());
            }

            assertTrue(testPath.mkdirs());
            LOG.info("Test Hierarchy 1");

            File subPath = new File("./target/testfiles/duplicate/Sub");
            assertTrue(subPath.mkdir());

            File subPath2 = new File("./target/testfiles/duplicate/Sub2");
            assertTrue(subPath2.mkdir());

            File testFileA = new File("./target/testfiles/duplicate/Sub/fileA.txt");
            assertTrue(testFileA.createNewFile());

            File testFileB = new File("./target/testfiles/duplicate/Sub/fileB.txt");
            assertTrue(testFileB.createNewFile());

            File testFileC = new File("./target/testfiles/duplicate/Sub/fileC.txt");
            assertTrue(testFileC.createNewFile());

            File testFileB2 = new File("./target/testfiles/duplicate/Sub2/fileB.txt");
            assertTrue(testFileB2.createNewFile());

            File testFileD = new File("./target/testfiles/duplicate/fileD.txt");
            assertTrue(testFileD.createNewFile());

            LocationDTO location = new LocationDTO();
            location.setId(1);

            SourceDTO source = new SourceDTO();
            source.setId(1);
            source.setType("STD");
            source.setPath(cwd + "/target/testfiles/duplicate");
            source.setLocation(location);

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            SynchronizeDTO synchronize = new SynchronizeDTO(1);
            synchronize.setSource(source);
            synchronize.setDestination(source);

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

            HierarchyResponse hierarchy = new HierarchyResponse();
            hierarchy.setId(-1);

            getMockMvc().perform(post("/jbr/int/backup/hierarchy")
                    .content(this.json(hierarchy))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            hierarchy.setId(1);
            for(FileInfo next: fileRepository.findAll()) {
                LOG.info("{} {} {}", next.getId(), next.getDirectoryInfo().getId(), next.getFullFilename());

                if(next.getFullFilename().contains("Sub/fileA.txt")) {
                    LOG.info("Use this as underlying {}", next.getDirectoryInfo().getId());
                    hierarchy.setUnderlyingId(next.getDirectoryInfo().getId());
                }
            }

            getMockMvc().perform(post("/jbr/int/backup/hierarchy")
                    .content(this.json(hierarchy))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));

            // Clear out the data.
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
    public void TestSource() {
        try {
            Source testSource = new Source();

            testSource.setTypeEnum(Source.SourceTypeType.IMPORT);
            assertEquals(Source.SourceTypeType.IMPORT,testSource.getTypeEnum());
        } catch(Exception ex) {
            fail();
        }

        Source testSource = new Source();

        testSource.setType("WRG");

        try {
            testSource.getTypeEnum();
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        } catch(Exception ex) {
            fail();
        }
    }

    @Test
    public void TestSyncrhonizeStatus() {
        try {
            SynchronizeStatus status = new SynchronizeStatus(null,null,null, null, null, null, null);

            assertEquals("",status.toString());
        } catch(Exception ex) {
            fail();
        }
    }
}
