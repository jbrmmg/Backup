package com.jbr.middletier.backup.manager;

import com.drew.imaging.ImageMetadataReader;
import com.jbr.middletier.backup.data.Classification;
import com.jbr.middletier.backup.data.MD5;
import com.jbr.middletier.backup.dto.ProcessResultDTO;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class FileSystem {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystem.class);

    private final DbLoggingManager dbLoggingManager;

    @Autowired
    public FileSystem(DbLoggingManager dbLoggingManager) {
        this.dbLoggingManager = dbLoggingManager;
    }

    public boolean directoryIsEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.findFirst().isEmpty();
            }
        }

        return false;
    }

    public void deleteFile(File file, ProcessResultDTO processResult) {
        LOG.info("Delete the file - {}", file );
        try {
            // Does it exist?
            if(!file.exists()) {
                LOG.info("{} does not exist", file);
                return;
            }

            // If the file is a folder, then delete the directory.
            if(!file.isDirectory()) {
                Files.deleteIfExists(file.toPath());
            }
        } catch (IOException e) {
            LOG.warn("Failed to delete file {}", file);
            dbLoggingManager.error(String.format("File delete failure: %s", file));
            processResult.setProblems();
        }
    }

    public void deleteDirectory(File file, ProcessResultDTO processResult) {
        LOG.info("Delete the directory - {}", file );
        try {
            // Does it exist?
            if(!file.exists()) {
                LOG.info("{} does not exist", file);
                return;
            }

            // If the file is a folder, then delete the directory.
            if(file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            }
        } catch (IOException e) {
            LOG.warn("Failed to delete file {}", file, e);
            dbLoggingManager.error(String.format("Directory delete failure: %s", file));
            processResult.setProblems();
        }
    }

    public static class TemporaryResultDTO extends ProcessResultDTO {
        public TemporaryResultDTO() {
            super(-1);
        }
    }

    public void deleteDirectoryIfEmpty(File file) throws IOException {
        if(directoryIsEmpty(file.toPath())) {
            TemporaryResultDTO result = new TemporaryResultDTO();

            deleteDirectory(file, result);

            if(result.hasProblems()) {
                throw new IOException("Failed to delete the directory " + file);
            }
        }
    }

    public void copyFile(File source, File destination, ProcessResultDTO processResult) {
        try {
            Files.copy(source.toPath(), destination.toPath(), REPLACE_EXISTING);
        } catch(IOException e) {
            processResult.setProblems();
            LOG.error("Unable to copy file {}", source);
        }
    }

    public void copyDirectory(File source, File destination, ProcessResultDTO processResult) {
        try {
            FileUtils.copyDirectory(source,destination,true);
        } catch(IOException e) {
            processResult.setProblems();
            LOG.error("Unable to copy file {}", source);
        }
    }

    public void moveFile(File source, File destination, ProcessResultDTO processResult) {
        try {
            LOG.info("Importing file {} to {}", source, destination);
            Files.move(source.toPath(),destination.toPath(),REPLACE_EXISTING);
        } catch (IOException e) {
            processResult.setProblems();
            LOG.error("Unable to move file {}", source);
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public MD5 getClassifiedFileMD5(Path path, Classification classification) {
        if(classification == null || !classification.getUseMD5()) {
            return new MD5();
        }

        try {
            // Calculate the MD5 for the file.
            MessageDigest md = MessageDigest.getInstance("MD5");

            try(DigestInputStream dis = new DigestInputStream(Files.newInputStream(path),md) ) {
                //noinspection StatementWithEmptyBody
                while (dis.read() != -1) ;
                md = dis.getMessageDigest();
            }

            return new MD5(bytesToHex(md.digest()));
        } catch (Exception ex) {
            LOG.error("Failed to get MD5, ",ex);
            dbLoggingManager.error("Cannot get MD5 - " + path.toString());
        }

        return new MD5();
    }

    public interface FileWalker {
        void processNextPath(Path path);
    }

    public void walkThePath(Path path, FileWalker walker) throws IOException {
        try(Stream<Path> pathStream = Files.walk(path)) {
            pathStream.forEach(walker::processNextPath);
        } catch(IOException e) {
            dbLoggingManager.error("Failed to walk + " + path);
            throw e;
        }
    }

    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    public void createDirectory(Path directory) throws IOException {
        if(Files.exists(directory)) {
            return;
        }

        Files.createDirectories(directory);
    }

    public byte[] readAllBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    public boolean fileExists(File file) {
        return Files.exists(file.toPath());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean directoryExists(Path path) {
        return Files.exists(path);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean setFileDateTime(File file, long time) {
        return file.setLastModified(time);
    }

    public boolean validateMountCheck(Optional<File> file) {
        if(file.isEmpty()) {
            LOG.warn("Mount check - skipped");
            return true;
        }

        if(Files.exists(file.get().toPath())) {
            return true;
        }

        LOG.warn("Mount check - missing {}", file);

        return false;
    }

    public Optional<FileSystemImageData> readImageMetaData(File file) {
        try {
            FileSystemImageData imageData = new FileSystemImageData(ImageMetadataReader.readMetadata(file));
            if(imageData.isValid()) {
                return Optional.of(imageData);
            }
        }
        catch (Exception e) {
            LOG.error("Unable to get image data from file {}", file.getName());
        }

        return Optional.empty();
    }

    public Set<String> listFilesInDirectory(String directory) {
        return Stream.of(Objects.requireNonNull(new File(directory).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }
}
