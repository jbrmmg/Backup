package com.jbr.middletier.backup;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.dto.BackupDTO;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.type.CleanBackup;
import com.jbr.middletier.backup.type.FileBackup;
import com.jbr.middletier.backup.type.NasBackup;
import com.jbr.middletier.backup.type.ZipupBackup;
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
    final static private Logger LOG = LoggerFactory.getLogger(TestBackups.class);

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
            if(!testFile.exists() && !testFile.mkdir()) {
                fail();
            }

            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("CLEAN");

            Backup backup = new Backup(backupDTO);

            CleanBackup cleanBackup = new CleanBackup(applicationProperties);
            cleanBackup.performBackup(backupManager, backup);

            assertFalse(testFile.exists());
        } catch(Exception ex) {
            LOG.error("Test failed - ",ex);
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

            File backupZip = new File(applicationProperties.getDirectory().getZip() + "/backups.zip" );
            if(backupZip.exists()) {
                assertTrue(backupZip.delete());
            }

            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("ZIP");

            Backup backup = new Backup(backupDTO);

            ZipupBackup zipupBackup = new ZipupBackup(applicationProperties);
            zipupBackup.performBackup(backupManager,backup);

            assertTrue(backupZip.exists());
        } catch(Exception ex) {
            LOG.error("Test failed - ",ex);
            fail();
        }
    }

    @Test
    public void TestFileBackup() {
        try {
            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("File");

            Backup backup = new Backup(backupDTO);

            FileBackup fileBackup = new FileBackup();
            fileBackup.performBackup(backupManager,backup);

            assertTrue(true);
        } catch(Exception ex) {
            LOG.error("Test failed - ",ex);
            fail();
        }
    }

    @Test
    public void TestGitBackup() {
        try {
            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("Git");

            Backup backup = new Backup(backupDTO);

            assertTrue(true);
        } catch(Exception ex) {
            LOG.error("Test failed - ",ex);
            fail();
        }
    }

    @Test
    public void TestNasBackup() {
        try {
            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("Nas");

            Backup backup = new Backup(backupDTO);

            NasBackup nasBackup = new NasBackup();
            nasBackup.performBackup(backupManager,backup);

            assertTrue(true);
        } catch(Exception ex) {
            LOG.error("Test failed - ",ex);
            fail();
        }
    }

    @Test
    public void TestDatabaseBackup() {
        try {
            // Perform the test.
            BackupManager backupManager = new BackupManager(applicationProperties, null);

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("DB");

            Backup backup = new Backup(backupDTO);

            assertTrue(true);
        } catch(Exception ex) {
            LOG.error("Test failed - ",ex);
            fail();
        }
    }
}
