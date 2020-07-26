package com.jbr.middletier.backup;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.dataaccess.BackupSpecifications;
import com.jbr.middletier.backup.dto.BackupDTO;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.schedule.BackupCtrl;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBackups {
    private static final Logger LOG = LoggerFactory.getLogger(TestBackups.class);

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    BackupRepository backupRepository;

    @Autowired
    BackupCtrl backupCtrl;

    private int GetBackupTime() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE) - 5;
    }

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

            BackupDTO backupDTO = new BackupDTO("CLN","clean");
            backupDTO.setTime(GetBackupTime());
            Backup backup = new Backup(backupDTO);

            backupRepository.save(backup);
            backupCtrl.scheduleBackup();

            assertFalse(testFile.exists());

            backupRepository.deleteAll();
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

            BackupDTO backupDTO = new BackupDTO("ZIP", "zipup");
            backupDTO.setTime(GetBackupTime());
            Backup backup = new Backup(backupDTO);

            backupRepository.save(backup);
            backupCtrl.scheduleBackup();

            assertTrue(backupZip.exists());

            backupRepository.deleteAll();
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

            BackupDTO backupDTO = new BackupDTO("File", "file");
            backupDTO.setDirectory("./target/testfiles/Backup");
            backupDTO.setBackupName("Test");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test.txt");
            backupDTO.setTime(GetBackupTime());

            File testFile = new File("./target/testfiles/Backup/test.txt");
            if (testFile.exists()) {
                assertTrue(testFile.delete());
            }
            if(!testFile.getParentFile().exists()) {
                assertTrue(testFile.getParentFile().mkdirs());
            }
            if(!testFile.exists()) {
                assertTrue(testFile.createNewFile());
            }

            Backup backup = new Backup(backupDTO);

            backupRepository.save(backup);
            backupCtrl.scheduleBackup();

            assertTrue(backedup.exists());

            backupRepository.deleteAll();
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    public void TestGitBackup() {
        try {
            File backupDir = new File("./target/testfiles/Backup");
            if (!backupDir.exists()) {
                assertTrue(backupDir.mkdirs());
            }
            File source = new File("./target/testfiles/BackupGit");
            if (!source.exists()) {
                assertTrue(source.mkdirs());
            }
            File source2 = new File("./target/testfiles/BackupGit/src");
            if (!source2.exists()) {
                assertTrue(source2.mkdirs());
            }
            File source2_1 = new File("./target/testfiles/BackupGit/.fred");
            if (!source2_1.exists()) {
                assertTrue(source2_1.createNewFile());
            }
            File source2_2 = new File("./target/testfiles/BackupGit/cpy.txt");
            if (!source2_2.exists()) {
                assertTrue(source2_2.createNewFile());
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
            File expected3 = new File(backupManager.todaysDirectory() + "/TestGit/cpy.txt");
            if (expected3.exists()) {
                assertTrue(expected3.delete());
            }
            File expected4 = new File(backupManager.todaysDirectory() + "/TestGit/.fred");
            if (expected4.exists()) {
                assertTrue(expected4.delete());
            }

            BackupDTO backupDTO = new BackupDTO("Git","git");
            backupDTO.setDirectory("./target/testfiles/BackupGit");
            backupDTO.setBackupName("TestGit");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test.txt");
            backupDTO.setTime(GetBackupTime());

            Backup backup = new Backup(backupDTO);

            backupRepository.save(backup);
            backupCtrl.scheduleBackup();

            assertTrue(expected1.exists());
            assertFalse(expected2.exists());
            assertTrue(expected3.exists());
            assertFalse(expected4.exists());

            backupRepository.deleteAll();
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    public void TestDatabaseBackup() {
        try {
            File backupDir = new File("./target/testfiles/Backup");
            if (!backupDir.exists()) {
                assertTrue(backupDir.mkdirs());
            }

            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            File expected1 = new File(backupManager.todaysDirectory() + "/TestDB/test.sql");
            if (expected1.exists()) {
                assertTrue(expected1.delete());
            }

            BackupDTO backupDTO = new BackupDTO("DB", "database");
            backupDTO.setDirectory("db:2:usr:pwd");
            backupDTO.setBackupName("TestDB");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test");
            backupDTO.setTime(GetBackupTime());

            Backup backup = new Backup(backupDTO);

            backupRepository.save(backup);
            backupCtrl.scheduleBackup();

            assertTrue(expected1.exists());

            backupRepository.deleteAll();
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    public void TestDatabaseBackupTimeout() {
        try {
            File backupDir = new File("./target/testfiles/Backup");
            if (!backupDir.exists()) {
                assertTrue(backupDir.mkdirs());
            }

            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            File expected1 = new File(backupManager.todaysDirectory() + "/TestDB/test.sql");
            if (expected1.exists()) {
                assertTrue(expected1.delete());
            }

            BackupDTO backupDTO = new BackupDTO("DB", "database");
            backupDTO.setDirectory("TestDB");
            backupDTO.setBackupName("TestDB");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test");
            backupDTO.setTime(GetBackupTime());

            Backup backup = new Backup(backupDTO);

            backupRepository.save(backup);
            backupCtrl.scheduleBackup();

            assertTrue(expected1.exists());
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    public void TestBackupBetween() {
        BackupDTO backupDTO = new BackupDTO();
        backupDTO.setId("TST1");
        backupDTO.setTime(100);

        Backup backup = new Backup(backupDTO);
        backupRepository.save(backup);

        backupDTO.setId("Tst2");
        backupDTO.setTime(200);
        backup = new Backup(backupDTO);
        backupRepository.save(backup);

        backupDTO.setId("Tst3");
        backupDTO.setTime(300);
        backup = new Backup(backupDTO);
        backupRepository.save(backup);

        backupDTO.setId("Tst4");
        backupDTO.setTime(400);
        backup = new Backup(backupDTO);
        backupRepository.save(backup);

        List<Backup> backupList = backupRepository.findAll(Specification.where(BackupSpecifications.backupsBetweenTimes(199,301)));
        assertEquals(2,backupList.size());

        backupRepository.deleteAll();
    }

    @Test
    public void TestInvalidType() {
        try {
            // Perform the test.
            BackupDTO backupDTO = new BackupDTO("BOB", "bob");
            backupDTO.setTime(GetBackupTime());

            Backup backup = new Backup(backupDTO);

            backupRepository.save(backup);
            backupCtrl.scheduleBackup();
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            assertTrue(true);
        }

        backupRepository.deleteAll();
    }
}
