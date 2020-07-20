package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.ActionConfirm;
import com.jbr.middletier.backup.data.ConfirmActionRequest;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.ImportRequest;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Calendar;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestFiles extends WebTester {
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

    @Test
    public void testGather() {
        try {
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
                    .andExpect(jsonPath("$[0].name",is(".")))
                    .andExpect(jsonPath("$[1].name",is("fileA.txt")))
                    .andExpect(jsonPath("$[2].name",is("fileB.txt")));

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
                    .andExpect(jsonPath("$[0].name",is(".")));

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
    public void testFileWeb() {
        try {
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
            jpgClass.setId(5);
            jpgClass.setOrder(5);
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
            for(FileInfo next: fileRepository.findAll()) {
                if(next.getName().equals("fileA.jpg")) {
                    id1 = next.getId();
                }
                if(next.getName().equals("fileB.mov")) {
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

            try {
                getMockMvc().perform(get("/jbr/int/backup/fileVideo?id=" + id2)
                        .content(this.json(temp))
                        .contentType(getContentType()))
                        .andExpect(status().isOk());
            } catch(Exception ex) {
                assertTrue(true);
            }

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
        } catch (Exception ex) {
            fail();
        }

        try {
            String temp = "testing";
            getMockMvc().perform(get("/jbr/int/backup/fileVideo?id=3")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());
        } catch(Exception ex) {
            assertTrue(true);
        }

        try {
            String temp = "testing";
            getMockMvc().perform(get("/jbr/int/backup/fileImage?id=3")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());
        } catch(Exception ex) {
            assertTrue(true);
        }

        try {
            String temp = "testing";
            getMockMvc().perform(get("/jbr/int/backup/file?id=3")
                    .content(this.json(temp))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());
        } catch(Exception ex) {
            assertTrue(true);
        }
    }

    @Test
    public void testImport() {
        try {
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
            source.setType("STD");
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
            calendar.set(2020,Calendar.JANUARY,1);
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
            for(ActionConfirm next: actionConfirmRepository.findAll()) {
                if(next.getAction().equals("IMPORT")) {
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

            String temp = "testing";
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
    public void testImportIngore() {
        try {
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
            source.setType("STD");
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
            calendar.set(2020,Calendar.JANUARY,1);
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
            for(ActionConfirm next: actionConfirmRepository.findAll()) {
                if(next.getAction().equals("IMPORT")) {
                    confirmActionRequest.setId(next.getId());
                    confirmActionRequest.setConfirm(true);
                    confirmActionRequest.setParameter("IGNORE");
                }
            }

            getMockMvc().perform(post("/jbr/int/backup/actions")
                    .content(this.json(confirmActionRequest))
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

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

            String temp = "testing";
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