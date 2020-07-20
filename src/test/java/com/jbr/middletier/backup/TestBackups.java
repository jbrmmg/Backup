package com.jbr.middletier.backup;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.dto.BackupDTO;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.type.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestBackups {
    private static final Logger LOG = LoggerFactory.getLogger(TestBackups.class);

    @Autowired
    ApplicationProperties applicationProperties;

    @Test
    public void TestCleanBackup() {
        try {
            // Setup the test
            File backupDirectory = new File(applicationProperties.getDirectory().getName());
            if (!backupDirectory.exists() && !backupDirectory.mkdir()) {
                LOG.warn("Cannot create the backup directory.");
            }

            File testFile = new File(applicationProperties.getDirectory().getName() + "/2020-01-01");
            if (!testFile.exists() && !testFile.mkdir()) {
                fail();
            }

            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);
            backupManager.initialiseDay();

            BackupDTO backupDTO = new BackupDTO("CLEAN","CLEAN");
            Backup backup = new Backup(backupDTO);

            CleanBackup cleanBackup = new CleanBackup(applicationProperties);
            cleanBackup.performBackup(backupManager, backup);

            assertFalse(testFile.exists());
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    public void TestZipBackup() {
        try {
            // Setup the test
            File backupDirectory = new File(applicationProperties.getDirectory().getName());
            if (!backupDirectory.mkdirs()) {
                LOG.warn("Cannot create the backup directory.");
            }
            backupDirectory = new File(applicationProperties.getDirectory().getZip());
            if (!backupDirectory.mkdirs()) {
                LOG.warn("Cannot create the backup directory.");
            }

            File backupZip = new File(applicationProperties.getDirectory().getZip() + "/backups.zip");
            if (backupZip.exists()) {
                assertTrue(backupZip.delete());
            }

            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            File testDirectory = new File(backupManager.todaysDirectory());
            if (!testDirectory.exists()) {
                assertTrue(testDirectory.mkdirs());
            }

            File testDirectory2 = new File(backupManager.todaysDirectory() + "//Sub1");
            if (!testDirectory2.exists()) {
                assertTrue(testDirectory2.mkdirs());
            }

            File testFile = new File(backupManager.todaysDirectory() + "//Sub1//TestA.txt");
            if(!testFile.exists()) {
                assertTrue(testFile.createNewFile());
            }

            BackupDTO backupDTO = new BackupDTO("ZIP", "ZIP");
            Backup backup = new Backup(backupDTO);

            ZipupBackup zipupBackup = new ZipupBackup(applicationProperties);
            zipupBackup.performBackup(backupManager, backup);

            assertTrue(backupZip.exists());
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    public void TestFileBackup() {
        try {
            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            File backedup = new File(backupManager.todaysDirectory() + "/Test/test.txt");
            if (backedup.exists()) {
                assertTrue(backedup.delete());
            }

            BackupDTO backupDTO = new BackupDTO("File", "File");
            backupDTO.setDirectory("./target/testfiles/Backup");
            backupDTO.setBackupName("Test");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test.txt");

            File testFile = new File("./target/testfiles/Backup/test.txt");
            if (testFile.exists()) {
                assertTrue(testFile.delete());
            }
            assertTrue(testFile.createNewFile());

            Backup backup = new Backup(backupDTO);

            FileBackup fileBackup = new FileBackup();
            fileBackup.performBackup(backupManager, backup);

            assertTrue(backedup.exists());
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    public void TestGitBackup() {
        try {
            File source = new File("./target/testfiles/BackupGit");
            if (!source.exists()) {
                assertTrue(source.mkdirs());
            }
            File source2 = new File("./target/testfiles/BackupGit/src");
            if (!source2.exists()) {
                assertTrue(source2.mkdirs());
            }
            File source3 = new File("./target/testfiles/BackupGit/target");
            if (!source3.exists()) {
                assertTrue(source3.mkdirs());
            }
            File source4 = new File("./target/testfiles/BackupGit/src/test.txt");
            if (!source4.exists()) {
                assertTrue(source4.createNewFile());
            }
            File source5 = new File("./target/testfiles/BackupGit/target/test.txt");
            if (!source5.exists()) {
                assertTrue(source5.createNewFile());
            }

            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            File expected1 = new File(backupManager.todaysDirectory() + "/TestGit/src/test.txt");
            if (expected1.exists()) {
                assertTrue(expected1.delete());
            }
            File expected2 = new File(backupManager.todaysDirectory() + "/TestGit/target/test.txt");
            if (expected2.exists()) {
                assertTrue(expected2.delete());
            }

            BackupDTO backupDTO = new BackupDTO("Git","Git");
            backupDTO.setDirectory("./target/testfiles/BackupGit");
            backupDTO.setBackupName("TestGit");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test.txt");

            Backup backup = new Backup(backupDTO);

            GitBackup gitBackup = new GitBackup();
            gitBackup.performBackup(backupManager, backup);

            assertTrue(expected1.exists());
            assertFalse(expected2.exists());
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    public void TestNasBackup() {
        try {
            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            BackupDTO backupDTO = new BackupDTO("NAS", "NAS");
            Backup backup = new Backup(backupDTO);

            NasBackup nasBackup = new NasBackup();
            nasBackup.performBackup(backupManager, backup);

            assertTrue(true);
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    public void TestDatabaseBackup() {
        try {
            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            File expected1 = new File(backupManager.todaysDirectory() + "/TestDB/test.sql");
            if (expected1.exists()) {
                assertTrue(expected1.delete());
            }

            BackupDTO backupDTO = new BackupDTO("DB", "DB");
            backupDTO.setDirectory("db:2:usr:pwd");
            backupDTO.setBackupName("TestDB");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test");

            Backup backup = new Backup(backupDTO);

            DatabaseBackup dbBackup = new DatabaseBackup(applicationProperties);
            dbBackup.performBackup(backupManager, backup);

            assertTrue(expected1.exists());
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    public void TestDatabaseBackupTimeout() {
        try {
            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            File expected1 = new File(backupManager.todaysDirectory() + "/TestDB/test.sql");
            if (expected1.exists()) {
                assertTrue(expected1.delete());
            }

            BackupDTO backupDTO = new BackupDTO("DB", "DB");
            backupDTO.setDirectory("TestDB");
            backupDTO.setBackupName("TestDB");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test");

            Backup backup = new Backup(backupDTO);

            DatabaseBackup dbBackup = new DatabaseBackup(applicationProperties);
            dbBackup.performBackup(backupManager, backup);

            assertTrue(expected1.exists());
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }
}
