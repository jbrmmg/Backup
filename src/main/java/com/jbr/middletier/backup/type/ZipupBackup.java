package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.DbLoggingManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.BackupManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by jason on 16/02/17.
 */
@Component
public class ZipupBackup implements PerformBackup  {
    private static final Logger LOG = LoggerFactory.getLogger(ZipupBackup.class);

    private final ApplicationProperties applicationProperties;

    public ZipupBackup(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    private void getAllFiles(File dir, List<File> fileList) throws IOException {
        File[] files = dir.listFiles();
        if(files != null) {
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
        FileOutputStream fos = new FileOutputStream(outputFilename);
        ZipOutputStream zos = new ZipOutputStream(fos);

        for (File file : fileList) {
            if (!file.isDirectory()) { // we only zip files, not directories
                addToZip(directoryToZip, file, zos);
            }
        }

        zos.close();
        fos.close();
    }

    private void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws
            IOException {

        try(FileInputStream fis = new FileInputStream(file)) {

            // we want the zipEntry's path to be a relative path that is relative
            // to the directory being zipped, so chop off the rest of the path
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
    public void performBackup(BackupManager backupManager, DbLoggingManager dbLoggingManager, FileSystem fileSystem, Backup backup) {
        try {
            dbLoggingManager.info("Zipup backup");
            String zipFilename = String.format("%s/backups.zip", applicationProperties.getZipDirectory());

            // If zip file exists, delete it.
            File zipFile = new File(zipFilename);
            if (zipFile.exists()) {
                FileUtils.forceDelete(zipFile);
            }

            // Zip up today's directory.
            File directoryToZip = new File(backupManager.todaysDirectory());
            List<File> fileList = new ArrayList<>();

            LOG.info("Getting references to all files in: {}",directoryToZip.getCanonicalPath());

            getAllFiles(directoryToZip, fileList);

            LOG.info("Creating zip file");

            writeZipFile(zipFilename, directoryToZip, fileList);

            LOG.info("Done");
        } catch (Exception ex) {
            LOG.error("Failed to perform zip backup",ex);
            dbLoggingManager.error("zipup backup " + ex);
        }
    }
}
