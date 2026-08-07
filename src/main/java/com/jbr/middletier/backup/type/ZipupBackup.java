package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.RunStatus;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystem;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ZipupBackup implements PerformBackup {
    private static final Logger LOG = LoggerFactory.getLogger(ZipupBackup.class);

    private final ApplicationProperties applicationProperties;
    private String lastSummary = "";

    @Autowired
    public ZipupBackup(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getType() {
        return TypeManager.ZIPUP_TYPE;
    }

    @Override
    public String getSummary() {
        return lastSummary;
    }

    private void getAllFiles(File dir, List<File> fileList) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.add(file);
                if (file.isDirectory()) {
                    LOG.info("DirectoryInfo: {}", file.getCanonicalPath());
                    getAllFiles(file, fileList);
                } else {
                    LOG.info("     file: {}", file.getCanonicalPath());
                }
            }
        }
    }

    private void writeZipFile(String outputFilename, File directoryToZip, List<File> fileList) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFilename);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (File file : fileList) {
                if (!file.isDirectory()) {
                    addToZip(directoryToZip, file, zos);
                }
            }
        }
    }

    private void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1);
            LOG.info("Writing {} to zip file", zipFilePath);
            ZipEntry zipEntry = new ZipEntry(zipFilePath);
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }

    @Override
    public RunStatus performBackup(BackupManager backupManager, FileSystem fileSystem, Backup backup) {
        try {
            LOG.info("Zipup backup");
            String zipFilename = String.format("%s/backups.zip", applicationProperties.getZipDirectory());

            File zipFile = new File(zipFilename);
            if (zipFile.exists()) {
                FileUtils.forceDelete(zipFile);
            }

            File directoryToZip = new File(backupManager.todaysDirectory());
            List<File> fileList = new ArrayList<>();
            LOG.info("Getting references to all files in: {}", directoryToZip.getCanonicalPath());
            getAllFiles(directoryToZip, fileList);

            int fileCount = (int) fileList.stream().filter(f -> !f.isDirectory()).count();

            LOG.info("Creating zip file");
            writeZipFile(zipFilename, directoryToZip, fileList);

            LOG.info("Done");
            lastSummary = String.format("Zip created, %d files", fileCount);
            return RunStatus.SUCCESS;
        } catch (Exception ex) {
            LOG.error("Failed to perform zip backup", ex);
            lastSummary = ex.getMessage();
            return RunStatus.FAILED;
        }
    }
}
