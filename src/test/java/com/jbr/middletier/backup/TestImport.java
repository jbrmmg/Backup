package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import org.apache.commons.io.FileUtils;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
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
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestImport extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(TestImport.class);

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
    private ClassificationRepository classificationRepository;

    @Test
    @Ignore
    public void TestImportBasic() {
        try {
            LOG.info("Test Import");
            // get the current working directory.
            String cwd = System.getProperty("user.dir");

            // Setup a directory structure.
            //   ./target/testfiles/gather
            //                       fileA.txt
            //                       fileB.txt

            File testPath = new File(cwd + "/target/testfiles/gather");
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
//            source.setType("STD");
            source.setPath("./target/testfiles/gather");
            source.setLocation(location);

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());


            // Setup the import directory.
            File importPath = new File("./target/testfiles/import");
            if (importPath.exists()) {
                FileUtils.cleanDirectory(importPath);
                assertTrue(importPath.delete());
            }

            assertTrue(importPath.mkdirs());

            File testFileC = new File("./target/testfiles/import/fileC.txt");
            assertTrue(testFileC.createNewFile());

            Calendar calendar = Calendar.getInstance();
            calendar.set(2020, Calendar.JANUARY, 1);
            assertTrue(testFileC.setLastModified(calendar.getTimeInMillis()));

            ImportRequest importRequest = new ImportRequest();
            importRequest.setSource(1);
            importRequest.setPath(cwd + "/target/testfiles/import");

            getMockMvc().perform(post("/jbr/int/backup/import")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/importfiles")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            getMockMvc().perform(post("/jbr/int/backup/importprocess")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            // Get the action id.
            ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
            for (ActionConfirm next : actionConfirmRepository.findAll()) {
                if (next.getAction().equals("IMPORT")) {
                    confirmActionRequest.setId(next.getId());
                    confirmActionRequest.setConfirm(true);
                    confirmActionRequest.setParameter("Test");
                }
            }

            getMockMvc().perform(get("/jbr/int/backup/actions")
                    .content(this.json(confirmActionRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            getMockMvc().perform(post("/jbr/int/backup/actions")
                    .content(this.json(confirmActionRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/confirmed-actions")
                    .content(this.json(confirmActionRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            getMockMvc().perform(put("/jbr/int/backup/importfiles")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/int/backup/importprocess")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            File importedFile = new File("./target/testfiles/gather/2020/January/Test/fileC.txt");
            assertTrue(importedFile.exists());

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

            source.setId(2);
            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            String temp = "testing";
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
    @Ignore
    public void TestImportIngore() {
        try {
            LOG.info("Test Import ignore");

            // get the current working directory.
            String cwd = System.getProperty("user.dir");

            // Setup a directory structure.
            //   ./target/testfiles/gather
            //                       fileA.txt
            //                       fileB.txt

            File testPath = new File(cwd + "/target/testfiles/gather");
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
//            source.setType("STD");
            source.setPath("./target/testfiles/gather");
            source.setLocation(location);

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());


            // Setup the import directory.
            File importPath = new File("./target/testfiles/import");
            if (importPath.exists()) {
                FileUtils.cleanDirectory(importPath);
                assertTrue(importPath.delete());
            }

            assertTrue(importPath.mkdirs());

            File testFileC = new File("./target/testfiles/import/fileC.txt");
            assertTrue(testFileC.createNewFile());

            Calendar calendar = Calendar.getInstance();
            calendar.set(2020, Calendar.JANUARY, 1);
            assertTrue(testFileC.setLastModified(calendar.getTimeInMillis()));

            ImportRequest importRequest = new ImportRequest();
            importRequest.setSource(1);
            importRequest.setPath(cwd + "/target/testfiles/import");

            getMockMvc().perform(post("/jbr/int/backup/import")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/importfiles")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            getMockMvc().perform(post("/jbr/int/backup/importprocess")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            // Get the action id.
            ConfirmActionRequest confirmActionRequest = new ConfirmActionRequest();
            for (ActionConfirm next : actionConfirmRepository.findAll()) {
                if (next.getAction().equals("IMPORT")) {
                    confirmActionRequest.setId(next.getId());
                    confirmActionRequest.setConfirm(true);
                    confirmActionRequest.setParameter("IGNORE");
                }
            }

            getMockMvc().perform(post("/jbr/int/backup/actions")
                    .content(this.json(confirmActionRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            confirmActionRequest.setId(300);

            getMockMvc().perform(post("/jbr/int/backup/actions")
                    .content(this.json(confirmActionRequest))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            getMockMvc().perform(put("/jbr/int/backup/importfiles")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/int/backup/importprocess")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(get("/jbr/int/backup/ignore")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            File importedFile = new File("./target/testfiles/gather/2020/January/Test/fileC.txt");
            assertFalse(importedFile.exists());

            for(ImportFile next: importFileRepository.findAll()) {
                next.setStatus("READ");
                importFileRepository.save(next);
            }

            getMockMvc().perform(post("/jbr/int/backup/importprocess")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());
            assertFalse(testFileC.exists());

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

            source.setId(2);
            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            String temp = "testing";
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
    @Ignore
    public void TestImportDoesNotExist() {
        try {
            // get the current working directory.
            String cwd = System.getProperty("user.dir");

            // Setup the import directory.
            File importPath = new File("./target/testfiles/import");
            if (importPath.exists()) {
                FileUtils.cleanDirectory(importPath);
                assertTrue(importPath.delete());
            }

            assertTrue(importPath.mkdirs());

            // Setup the import directory.
            ImportRequest importRequest = new ImportRequest();
            importRequest.setSource(1);
            importRequest.setPath(cwd + "/target/testfiles/import/doesnot");

            getMockMvc().perform(post("/jbr/int/backup/import")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            importRequest.setSource(10);
            importRequest.setPath(cwd + "/target/testfiles/import");
            getMockMvc().perform(post("/jbr/int/backup/import")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().is(404));

            // Clear out the data.
            actionConfirmRepository.deleteAll();
            ignoreFileRepository.deleteAll();
            importFileRepository.deleteAll();
            fileRepository.deleteAll();
            directoryRepository.deleteAll();
        } catch(Exception ex) {
            fail();
        }
    }


    @Test
    @Ignore
    public void TestClearImport() {
        try {
            // get the current working directory.
            String cwd = System.getProperty("user.dir");

            File testPath = new File(cwd + "/target/testfiles/gather");
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
//            source.setType("STD");
            source.setPath("./target/testfiles/gather");
            source.setLocation(location);

            getMockMvc().perform(post("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());
            // Setup the import directory.
            File importPath = new File("./target/testfiles/import");
            if (importPath.exists()) {
                FileUtils.cleanDirectory(importPath);
                assertTrue(importPath.delete());
            }

            assertTrue(importPath.mkdirs());

            File testFileC = new File("./target/testfiles/import/fileC.txt");
            assertTrue(testFileC.createNewFile());

            Calendar calendar = Calendar.getInstance();
            calendar.set(2020, Calendar.JANUARY, 1);
            assertTrue(testFileC.setLastModified(calendar.getTimeInMillis()));

            // Setup the import directory.
            ImportRequest importRequest = new ImportRequest();
            importRequest.setSource(1);
            importRequest.setPath(cwd + "/target/testfiles/import");

            getMockMvc().perform(post("/jbr/int/backup/import")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/int/backup/importprocess")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/int/backup/import")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

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
        } catch(Exception ex) {
            fail();
        }
    }

    @Test
    @Ignore
    public void TestAlreadyExists() {
        try {
            ClassificationDTO classificationDTO = new ClassificationDTO();
            classificationDTO.setVideo(false);
            classificationDTO.setImage(false);
            classificationDTO.setUseMD5(true);
            classificationDTO.setAction("BACKUP");
            classificationDTO.setRegex(".*\\.tstmd5");
            classificationDTO.setOrder(1);

            Classification newClassification = new Classification(classificationDTO);
            classificationRepository.save(newClassification);
            LOG.info("Classification {}", newClassification);

            // get the current working directory.
            String cwd = System.getProperty("user.dir");

            File testPath = new File(cwd + "/target/testfiles/gather");
            if (testPath.exists()) {
                FileUtils.cleanDirectory(testPath);
                assertTrue(testPath.delete());
            }

            assertTrue(testPath.mkdirs());

            File testFileA = new File("./target/testfiles/gather/fileA.tstmd5");
            assertTrue(testFileA.createNewFile());

            File testFileB = new File("./target/testfiles/gather/fileB.tstmd5");
            assertTrue(testFileB.createNewFile());

            // Setup a new source
            LocationDTO location = new LocationDTO();
            location.setId(1);
            SourceDTO source = new SourceDTO();
            source.setId(1);
//            source.setType("STD");
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

            // Setup the import directory.
            File importPath = new File("./target/testfiles/import");
            if (importPath.exists()) {
                FileUtils.cleanDirectory(importPath);
                assertTrue(importPath.delete());
            }

            assertTrue(importPath.mkdirs());

            File testFileB2 = new File("./target/testfiles/import/fileB.tstmd5");
            assertTrue(testFileB2.createNewFile());

            Calendar calendar = Calendar.getInstance();
            calendar.set(2020, Calendar.JANUARY, 1);
            assertTrue(testFileB.setLastModified(calendar.getTimeInMillis()));
            assertTrue(testFileB2.setLastModified(calendar.getTimeInMillis()));

            // Setup the import directory.
            ImportRequest importRequest = new ImportRequest();
            importRequest.setSource(1);
            importRequest.setPath(cwd + "/target/testfiles/import");

            getMockMvc().perform(post("/jbr/int/backup/import")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/int/backup/importprocess")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            assertFalse(testFileB2.exists());

            // Clear out the data.
            actionConfirmRepository.deleteAll();
            ignoreFileRepository.deleteAll();
            importFileRepository.deleteAll();
            fileRepository.deleteAll();
            directoryRepository.deleteAll();
            classificationRepository.delete(newClassification);

            getMockMvc().perform(delete("/jbr/ext/backup/source")
                    .content(this.json(source))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());
        } catch(Exception ex) {
            fail();
        }
    }

    @Test
    @Ignore
    public void TestImportFileRemoved() {
        try {
            // get the current working directory.
            String cwd = System.getProperty("user.dir");

            File testPath = new File(cwd + "/target/testfiles/gather");
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
//            source.setType("STD");
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

            // Setup the import directory.
            File importPath = new File("./target/testfiles/import");
            if (importPath.exists()) {
                FileUtils.cleanDirectory(importPath);
                assertTrue(importPath.delete());
            }

            assertTrue(importPath.mkdirs());

            File testFileC = new File("./target/testfiles/import/fileC.txt");
            assertTrue(testFileC.createNewFile());

            File testFileD = new File("./target/testfiles/import/fileD.txt");
            assertTrue(testFileD.createNewFile());

            // Setup the import directory.
            ImportRequest importRequest = new ImportRequest();
            importRequest.setSource(1);
            importRequest.setPath(cwd + "/target/testfiles/import");

            getMockMvc().perform(post("/jbr/int/backup/import")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            getMockMvc().perform(post("/jbr/int/backup/importprocess")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            assertTrue(testFileD.delete());

            getMockMvc().perform(delete("/jbr/int/backup/import")
                    .content(this.json(importRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

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
        } catch(Exception ex) {
            fail();
        }
    }
}
