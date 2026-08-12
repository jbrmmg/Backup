package com.jbr.middletier.backup;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.RunStatus;
import com.jbr.middletier.backup.dataaccess.BackupJobRunRepository;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.dto.BackupDTO;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.schedule.BackupCtrl;
import com.jbr.middletier.backup.type.CleanBackup;
import com.jbr.middletier.backup.type.DatabaseBackup;
import com.jbr.middletier.backup.type.ZipupBackup;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestBackups {
    private static final Logger LOG = LoggerFactory.getLogger(TestBackups.class);

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    BackupRepository backupRepository;

    @Autowired
    BackupJobRunRepository backupJobRunRepository;

    @Autowired
    BackupCtrl backupCtrl;

    @Autowired
    ModelMapper modelMapper;

    @BeforeEach
    void clearJobRuns() {
        backupJobRunRepository.deleteAll();
    }

    @Test
    void TestCleanBackup() {
        try {
            File testFile = new File(applicationProperties.getDirectory().getName() + "/2020-01-01");
            Files.createDirectories(testFile.toPath());
            assertTrue(testFile.exists());

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("CLN");
            backupDTO.setType("clean");
            backupDTO.setTime(1);
            Backup backup = modelMapper.map(backupDTO, Backup.class);

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
    void TestCleanBackupFailure() {
        ApplicationProperties mockApplicationProperties = mock(ApplicationProperties.class);
        ApplicationProperties.Directory directory = mock(ApplicationProperties.Directory.class);
        when(mockApplicationProperties.getDirectory()).thenReturn(directory);
        when(directory.getName()).thenReturn("thisdirectorydoesnotexist");

        BackupManager backupManager = mock(BackupManager.class);
        FileSystem fileSystem = mock(FileSystem.class);
        Backup backup = mock(Backup.class);

        CleanBackup cleanBackup = new CleanBackup(mockApplicationProperties);

        assertThrows(IllegalStateException.class,
                () -> cleanBackup.performBackup(backupManager, fileSystem, backup));
        assertEquals("Backup directory does not exist.", cleanBackup.getSummary());
    }

    @Test
    void TestCleanBackupFailure2() throws IOException {
        File testFile = new File(applicationProperties.getDirectory().getName() + "/20201401");
        Files.createDirectories(testFile.toPath());
        assertTrue(testFile.exists());

        File testFile2 = new File(applicationProperties.getDirectory().getName() + "/20201401/Text.txt");
        if (!testFile2.exists()) {
            Files.createFile(testFile2.toPath());
        }
        assertTrue(testFile2.exists());

        BackupManager backupManager = mock(BackupManager.class);
        FileSystem fileSystem = mock(FileSystem.class);
        Backup backup = mock(Backup.class);

        CleanBackup cleanBackup = new CleanBackup(applicationProperties);
        RunStatus status = cleanBackup.performBackup(backupManager, fileSystem, backup);

        assertEquals(RunStatus.SUCCESS, status);
        assertTrue(testFile.exists());
    }

    @Test
    void TestZipBackup() {
        try {
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

            BackupManager backupManager = new BackupManager(applicationProperties);

            File testDirectory = new File(backupManager.todaysDirectory());
            if (!testDirectory.exists()) {
                assertTrue(testDirectory.mkdirs());
            }

            File testDirectory2 = new File(backupManager.todaysDirectory() + "//Sub1");
            if (!testDirectory2.exists()) {
                assertTrue(testDirectory2.mkdirs());
            }

            File testFile = new File(backupManager.todaysDirectory() + "//Sub1//TestA.txt");
            Files.deleteIfExists(testFile.toPath());
            if (!testFile.exists()) {
                assertTrue(testFile.createNewFile());
                PrintWriter writer = new PrintWriter(testFile.toPath().toString());
                writer.println("Test File");
                writer.close();
            }

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("ZIP");
            backupDTO.setType("zipup");
            backupDTO.setTime(1);
            Backup backup = modelMapper.map(backupDTO, Backup.class);

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
    void TestZipBackupExists() {
        try {
            File backupDirectory = new File(applicationProperties.getDirectory().getName());
            if (!backupDirectory.mkdirs()) {
                LOG.warn("Cannot create the backup directory.");
            }
            backupDirectory = new File(applicationProperties.getDirectory().getZip());
            if (!backupDirectory.mkdirs()) {
                LOG.warn("Cannot create the backup directory.");
            }

            File backupZip = new File(applicationProperties.getDirectory().getZip() + "/backups.zip");
            if (!backupZip.exists()) {
                assertTrue(backupZip.createNewFile());
            }

            BackupManager backupManager = new BackupManager(applicationProperties);

            File testDirectory = new File(backupManager.todaysDirectory());
            if (!testDirectory.exists()) {
                assertTrue(testDirectory.mkdirs());
            }

            File testDirectory2 = new File(backupManager.todaysDirectory() + "//Sub1");
            if (!testDirectory2.exists()) {
                assertTrue(testDirectory2.mkdirs());
            }

            File testFile = new File(backupManager.todaysDirectory() + "//Sub1//TestA.txt");
            if (!testFile.exists()) {
                assertTrue(testFile.createNewFile());
            }

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("ZIP");
            backupDTO.setType("zipup");
            backupDTO.setTime(1);
            Backup backup = modelMapper.map(backupDTO, Backup.class);

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
    void TestZipDirectoryEmpty() {
        try {
            File backupDirectory = new File(applicationProperties.getDirectory().getName());
            if (!backupDirectory.mkdirs()) {
                LOG.warn("Cannot create the backup directory.");
            }
            backupDirectory = new File(applicationProperties.getDirectory().getZip());
            if (!backupDirectory.mkdirs()) {
                LOG.warn("Cannot create the backup directory.");
            }

            File backupZip = new File(applicationProperties.getDirectory().getZip() + "/backups.zip");
            if (!backupZip.exists()) {
                assertTrue(backupZip.createNewFile());
            }

            BackupManager backupManager = new BackupManager(applicationProperties);

            File testDirectory = new File(backupManager.todaysDirectory());
            if (testDirectory.exists()) {
                FileUtils.deleteDirectory(testDirectory);
            }

            FileSystem fileSystem = mock(FileSystem.class);
            Backup backup = mock(Backup.class);

            ZipupBackup zipupBackup = new ZipupBackup(applicationProperties);
            RunStatus status = zipupBackup.performBackup(backupManager, fileSystem, backup);

            assertEquals(RunStatus.SUCCESS, status);
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    public void cleanUpLockedDirectory() {
        try {
            File newDir = new File(applicationProperties.getDirectory().getZip() + "/backups.zip");
            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(newDir.toPath(), permissions);
            FileUtils.deleteDirectory(newDir);
        } catch (Exception ex) {
            LOG.error("Failed to clean up.");
        }
    }

    @Test
    void TestZipBackupFail() {
        // Root bypasses POSIX file permissions, so the permission-restricted directory
        // can be deleted and the backup succeeds rather than failing as expected.
        assumeFalse("root".equals(System.getProperty("user.name")), "Skipping: file permission restrictions are not enforced when running as root");
        try {
            File backupDirectory = new File(applicationProperties.getDirectory().getName());
            if (backupDirectory.exists()) {
                FileUtils.deleteDirectory(backupDirectory);
            }
            backupDirectory = new File(applicationProperties.getDirectory().getZip());
            if (!backupDirectory.mkdirs()) {
                LOG.warn("Cannot create the backup directory.");
            }

            File backupFile = new File(applicationProperties.getDirectory().getZip() + "/backups.zip");
            if (backupFile.exists()) {
                assertTrue(backupFile.delete());
            }

            File backupFile2 = new File(applicationProperties.getDirectory().getZip() + "/backups_zip");
            if (backupFile2.exists()) {
                FileUtils.deleteDirectory(backupFile2);
            }
            assertTrue(backupFile2.mkdir());
            File backupFile3 = new File(applicationProperties.getDirectory().getZip() + "/backups_zip/test.txt");
            assertTrue(backupFile3.createNewFile());

            File dir = new File(applicationProperties.getDirectory().getZip() + "/backups_zip");
            File newDir = new File(applicationProperties.getDirectory().getZip() + "/backups.zip");
            assertTrue(dir.renameTo(newDir));

            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            Files.setPosixFilePermissions(newDir.toPath(), permissions);

            BackupManager backupManager = new BackupManager(applicationProperties);

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("ZIP");
            backupDTO.setType("zipup");
            backupDTO.setTime(1);
            Backup backup = modelMapper.map(backupDTO, Backup.class);

            FileSystem fileSystem = mock(FileSystem.class);
            ZipupBackup zipupBackup = new ZipupBackup(applicationProperties);
            RunStatus status = zipupBackup.performBackup(backupManager, fileSystem, backup);

            assertEquals(RunStatus.FAILED, status);
            backupRepository.deleteAll();
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        } finally {
            cleanUpLockedDirectory();
        }
    }

    @Test
    void TestFileBackup() {
        try {
            BackupManager backupManager = new BackupManager(applicationProperties);

            File backedup = new File(backupManager.todaysDirectory() + "/Test/test.txt");
            if (backedup.exists()) {
                assertTrue(backedup.delete());
            }

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("File");
            backupDTO.setType("file");
            backupDTO.setDirectory("./target/testfiles/Backup");
            backupDTO.setBackupName("Test");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test.txt");
            backupDTO.setTime(1);

            File testFile = new File("./target/testfiles/Backup/test.txt");
            if (testFile.exists()) {
                assertTrue(testFile.delete());
            }
            if (!testFile.getParentFile().exists()) {
                assertTrue(testFile.getParentFile().mkdirs());
            }
            if (!testFile.exists()) {
                assertTrue(testFile.createNewFile());
            }

            Backup backup = modelMapper.map(backupDTO, Backup.class);

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
    void TestFileBackupNoSource() {
        try {
            BackupManager backupManager = new BackupManager(applicationProperties);

            File backedup = new File(backupManager.todaysDirectory() + "/Test/test.txt");
            if (backedup.exists()) {
                assertTrue(backedup.delete());
            }
            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("File");
            backupDTO.setType("file");
            backupDTO.setDirectory("./target/testfiles/Backup");
            backupDTO.setBackupName("Test");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("testx.txt");
            backupDTO.setTime(1);

            File testFile = new File("./target/testfiles/Backup/test.txt");
            if (testFile.exists()) {
                assertTrue(testFile.delete());
            }
            if (!testFile.getParentFile().exists()) {
                assertTrue(testFile.getParentFile().mkdirs());
            }

            Backup backup = modelMapper.map(backupDTO, Backup.class);

            backupRepository.save(backup);
            backupCtrl.scheduleBackup();

            assertFalse(backedup.exists());

            backupRepository.deleteAll();
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    void TestFileBackupNoSourceDir() {
        try {
            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("File");
            backupDTO.setType("file");
            backupDTO.setDirectory("./target/testfiles/Backupx");
            backupDTO.setBackupName("Test");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("testx.txt");
            backupDTO.setTime(1);

            Backup backup = modelMapper.map(backupDTO, Backup.class);

            backupRepository.save(backup);
            backupCtrl.scheduleBackup();

            backupRepository.deleteAll();
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    void TestGitBackup() {
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
            File source21 = new File("./target/testfiles/BackupGit/.fred");
            if (!source21.exists()) {
                assertTrue(source21.createNewFile());
            }
            File source22 = new File("./target/testfiles/BackupGit/cpy.txt");
            if (!source22.exists()) {
                assertTrue(source22.createNewFile());
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

            BackupManager backupManager = new BackupManager(applicationProperties);

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

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("Git");
            backupDTO.setType("git");
            backupDTO.setDirectory("./target/testfiles/BackupGit");
            backupDTO.setBackupName("TestGit");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test.txt");
            backupDTO.setTime(1);

            Backup backup = modelMapper.map(backupDTO, Backup.class);

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
    void TestDatabaseBackup() {
        try {
            File backupDir = new File("./target/testfiles/Backup");
            if (!backupDir.exists()) {
                assertTrue(backupDir.mkdirs());
            }

            BackupManager backupManager = new BackupManager(applicationProperties);

            File expected1 = new File(backupManager.todaysDirectory() + "/TestDB/test.sql");
            if (expected1.exists()) {
                assertTrue(expected1.delete());
            }

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("DB");
            backupDTO.setType("database");
            backupDTO.setDirectory("db:2:usr:pwd");
            backupDTO.setBackupName("TestDB");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test");
            backupDTO.setTime(1);

            Backup backup = modelMapper.map(backupDTO, Backup.class);

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
    void TestDatabaseBackupTimeout() {
        try {
            File backupDir = new File("./target/testfiles/Backup");
            if (!backupDir.exists()) {
                assertTrue(backupDir.mkdirs());
            }

            BackupManager backupManager = new BackupManager(applicationProperties);

            File expected1 = new File(backupManager.todaysDirectory() + "/TestDB/test.sql");
            if (expected1.exists()) {
                assertTrue(expected1.delete());
            }

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("DB");
            backupDTO.setType("database");
            backupDTO.setDirectory("TestDB");
            backupDTO.setBackupName("TestDB");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test");
            backupDTO.setTime(1);

            Backup backup = modelMapper.map(backupDTO, Backup.class);

            backupRepository.save(backup);
            backupCtrl.scheduleBackup();

            assertTrue(expected1.exists());
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    void TestInvalidType() {
        BackupDTO backupDTO = new BackupDTO();
        backupDTO.setId("BOB");
        backupDTO.setType("bob");
        backupDTO.setTime(1);

        Backup backup = modelMapper.map(backupDTO, Backup.class);
        backupRepository.save(backup);
        backupCtrl.scheduleBackup();

        backupRepository.deleteAll();
    }

    @Test
    void TestDatabaseInvalidDb() {
        try {
            File backupDir = new File("./target/testfiles/Backup");
            if (!backupDir.exists()) {
                assertTrue(backupDir.mkdirs());
            }

            BackupManager backupManager = new BackupManager(applicationProperties);

            File expected1 = new File(backupManager.todaysDirectory() + "/TestDB/test.sql");
            if (expected1.exists()) {
                assertTrue(expected1.delete());
            }

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("DB");
            backupDTO.setType("database");
            backupDTO.setDirectory("db:2");
            backupDTO.setBackupName("TestDB");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("synchronise");
            backupDTO.setTime(1);

            Backup backup = modelMapper.map(backupDTO, Backup.class);

            backupRepository.save(backup);
            backupCtrl.scheduleBackup();

            assertFalse(expected1.exists());

            backupRepository.deleteAll();
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    void TestDatabaseAlreadyDone() {
        try {
            File backupDir = new File("./target/testfiles/Backup");
            Files.createDirectories(backupDir.toPath());
            assertTrue(backupDir.exists());

            BackupManager backupManager = new BackupManager(applicationProperties);

            File expected1 = new File(backupManager.todaysDirectory() + "/TestDB/test.sql");
            if (expected1.exists()) {
                assertTrue(expected1.delete());
            }

            assertTrue(expected1.getParentFile().mkdir());
            assertTrue(expected1.createNewFile());

            RandomAccessFile raf = new RandomAccessFile(backupManager.todaysDirectory() + "/TestDB/test.sql", "rw");
            raf.setLength(102);
            raf.close();

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("DB");
            backupDTO.setType("database");
            backupDTO.setDirectory("db:2:x:y");
            backupDTO.setBackupName("TestDB");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test.sql");
            backupDTO.setTime(1);

            Backup backup = modelMapper.map(backupDTO, Backup.class);

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
    void TestDatabaseBadConfig() {
        try {
            FileSystem fileSystem = mock(FileSystem.class);

            File backupDir = new File("./target/testfiles/Backup");
            if (!backupDir.exists()) {
                assertTrue(backupDir.mkdirs());
            }

            BackupManager backupManager = new BackupManager(applicationProperties);

            DatabaseBackup databaseBackup = new DatabaseBackup(applicationProperties);
            String backupDbUrl = applicationProperties.getDbUrl();
            applicationProperties.setDbUrl("x:y");

            File expected1 = new File(backupManager.todaysDirectory() + "/TestDB/test.sql");
            if (expected1.exists()) {
                assertTrue(expected1.delete());
            }

            BackupDTO backupDTO = new BackupDTO();
            backupDTO.setId("DB");
            backupDTO.setType("database");
            backupDTO.setDirectory("2:x:y");
            backupDTO.setBackupName("TestDB");
            backupDTO.setFileName("Fred");
            backupDTO.setArtifact("test.sql");
            backupDTO.setTime(1);

            Backup backup = modelMapper.map(backupDTO, Backup.class);

            RunStatus status = databaseBackup.performBackup(backupManager, fileSystem, backup);
            applicationProperties.setDbUrl(backupDbUrl);

            assertEquals(RunStatus.FAILED, status);
            assertFalse(expected1.exists());
        } catch (Exception ex) {
            LOG.error("Test failed - ", ex);
            fail();
        }
    }

    @Test
    void testBackupCtrlDisabled() {
        ApplicationProperties mockApplicationProperties = new ApplicationProperties();
        mockApplicationProperties.setEnabled(false);

        BackupCtrl localBackupCtrl = new BackupCtrl(null, null, null, null, mockApplicationProperties, null);
        localBackupCtrl.scheduleBackup();
    }

    @Test
    void testDbBackup() {
        FileSystem fileSystem = mock(FileSystem.class);

        ApplicationProperties properties = mock(ApplicationProperties.class);
        when(properties.getDbBackupCommand()).thenReturn("xx");
        when(properties.getDbUrl()).thenReturn("xx:xx:xx:xx");

        BackupManager manager = mock(BackupManager.class);
        when(manager.todaysDirectory()).thenReturn("./target/it_test");

        Backup backup = mock(Backup.class);
        when(backup.getBackupName()).thenReturn("test");
        when(backup.getArtifact()).thenReturn("blah.txt");
        when(backup.getDirectory()).thenReturn("xx");

        DatabaseBackup dbBackup = new DatabaseBackup(properties);
        assertNotNull(dbBackup);
        RunStatus status = dbBackup.performBackup(manager, fileSystem, backup);
        assertEquals(RunStatus.SUCCESS, status);
    }

    @Test
    void testDbBackup2() {
        FileSystem fileSystem = mock(FileSystem.class);

        ApplicationProperties properties = mock(ApplicationProperties.class);
        when(properties.getDbBackupCommand()).thenReturn("xx");
        when(properties.getDbUrl()).thenReturn("xx:xx:xx:xx");
        when(properties.getDbUsername()).thenReturn("user");
        when(properties.getDbPassword()).thenReturn("pwd");

        BackupManager manager = mock(BackupManager.class);
        when(manager.todaysDirectory()).thenReturn("./target/it_test");

        Backup backup = mock(Backup.class);
        when(backup.getBackupName()).thenReturn("test");
        when(backup.getArtifact()).thenReturn("blah.txt");
        when(backup.getDirectory()).thenReturn("xx");

        DatabaseBackup dbBackup = new DatabaseBackup(properties);
        assertNotNull(dbBackup);
        RunStatus status = dbBackup.performBackup(manager, fileSystem, backup);
        assertEquals(RunStatus.SUCCESS, status);
    }
}
