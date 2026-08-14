package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.MD5;
import com.jbr.middletier.backup.dto.ProcessResultDTO;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class FileSystem {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystem.class);

    private final ApplicationProperties applicationProperties;

    @Autowired
    public FileSystem(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public boolean directoryIsEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.findFirst().isEmpty();
            }
        }

        return false;
    }

    public void deleteFile(File file, ProcessResultDTO processResult, Integer id) {
        LOG.info("Delete the file - {}", file );
        try {
            // Does it exist?
            if(!file.exists()) {
                LOG.info("{} does not exist for delete.", file);
                return;
            }

            // If the file is a folder, then delete the directory.
            if(!file.isDirectory()) {
                Files.deleteIfExists(file.toPath());
            }
        } catch (IOException e) {
            LOG.warn("Failed to delete file {}", file);
            processResult.setProblems();
        }
    }

    public void deleteDirectory(File file, ProcessResultDTO processResult, Integer id) {
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

            deleteDirectory(file, result,null);

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

    public void setFileFromLocalDateTime(File destination, LocalDateTime overrideTime, long defaultTime) {
        if(overrideTime != null) {
            ZonedDateTime zonedDateTime = overrideTime.atZone(ZoneId.systemDefault());
            defaultTime = zonedDateTime.toInstant().toEpochMilli();
        }
        setFileDateTime(destination, defaultTime);
    }

    public void copyDirectory(File source, File destination, ProcessResultDTO processResult) {
        try {
            FileUtils.copyDirectory(source,destination,true);
        } catch(IOException e) {
            processResult.setProblems();
            LOG.error("Unable to copy directory {}", source);
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

    public Optional<MD5> getFileMD5(Path path, Integer id) {
        try {
            LOG.debug("Start get MD5 for {}", path);
            // Calculate the MD5 for the file.
            MessageDigest md = MessageDigest.getInstance("MD5");

            try (InputStream is = Files.newInputStream(path);
                 DigestInputStream dis = new DigestInputStream(is, md)) {

                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    LOG.trace("Read {} bytes", buffer.length);
                }
                md = dis.getMessageDigest();
            }
            MD5 md5 = new MD5(bytesToHex(md.digest()));
            LOG.debug("End get MD5 for {} {}", path, md5);

            return Optional.of(md5);
        } catch (Exception ex) {
            LOG.error("Failed to get MD5, ",ex);
        }

        return Optional.empty();
    }

    public interface FileWalker {
        void processNextPath(Path path);
    }

    public void walkThePath(Path path, FileWalker walker) throws IOException {
        try(Stream<Path> pathStream = Files.walk(path)) {
            pathStream.forEach(walker::processNextPath);
        } catch(IOException e) {
            LOG.error("Failed to walk {}", path);
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
            // Use the Exif tool to read metadata from the specified file.
            Process process = new ProcessBuilder("exiftool", file.getPath()).start();

            InputStream processInputStream = process.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(processInputStream));

            List<String> tmp = reader.lines().toList();
            Map<String,String> map = new HashMap<>();

            for(String line : tmp) {
                String key = line.substring(0,line.indexOf(":")).trim().toLowerCase();
                String value = line.substring(line.indexOf(":")+1).trim().toLowerCase();

                if(map.containsKey(key)) {
                    LOG.info("Line {} is a duplicate key {}", line, key);
                } else {
                    map.put(key,value);
                }
            }

            FileSystemImageData imageData = new FileSystemImageData(map);
            if(imageData.isValid()) {
                return Optional.of(imageData);
            }
        } catch (IOException e) {
            LOG.info("Failed to read any meta data from file",e);
        }

        // Return nothing
        return Optional.empty();
    }

    public Set<String> listFilesInDirectory(File directory) {
        return Stream.of(Objects.requireNonNull(directory.listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    private void runCommand(String command, File input, File output) throws IOException, InterruptedException {
        command = command.replace("%%INPUT%%", input.toString().replace(" ", "\\ "));
        command = command.replace("%%OUTPUT%%", output.toString().replace(" ", "\\ "));

        LOG.info("Command: {}", command);

        String[] cmd = new String[]{"sh", "-c", command};
        final Process backupProcess = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

        String processOutput;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(backupProcess.getInputStream()))) {
            processOutput = reader.lines().collect(Collectors.joining("\n"));
        }

        boolean completed = backupProcess.waitFor(20L, TimeUnit.MINUTES);
        int exitCode = backupProcess.exitValue();
        backupProcess.destroyForcibly();

        LOG.info("Command completed: {}, exit code: {}", completed, exitCode);
        if(!processOutput.isEmpty()) {
            LOG.info("Command output: {}", processOutput);
        }
        if(exitCode != 0) {
            LOG.warn("Command failed with exit code {}: {}", exitCode, command);
        }
        LOG.info("Output file exists: {}", output.exists());
    }

    public File getImageFileFromVideoFile(File file) throws IOException, InterruptedException, NoSuchAlgorithmException {
        // Turn the path into base64.
        String tempName = file.getPath().trim();

        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] digestBytes = md.digest(tempName.getBytes());

        StringBuilder sb = new StringBuilder();
        for(byte b : digestBytes) {
            String hex = Integer.toHexString(0xFF & b);
            if(hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }

        tempName = applicationProperties.getVidToImageLocation() + "/" + sb + ".jpg";

        File tempFile = new File(tempName);
        if(tempFile.exists()) {
            return tempFile;
        }

        String copyCommand = applicationProperties.getVidToImageCommand();
        runCommand(copyCommand, file, tempFile);

        return tempFile;
    }

    public File getTransformedImageFile(File file, String transformerName) throws IOException, InterruptedException, NoSuchAlgorithmException {
        ApplicationProperties.Transformer transformer = applicationProperties.getTransformers().get(transformerName);
        if(transformer == null) {
            throw new IllegalArgumentException("Unknown image transformer: " + transformerName);
        }

        String tempName = file.getPath().trim();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digestBytes = md.digest(tempName.getBytes());
        StringBuilder sb = new StringBuilder();
        for(byte b : digestBytes) {
            String hex = Integer.toHexString(0xFF & b);
            if(hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }

        tempName = transformer.getLocation() + "/" + sb + ".jpg";
        File tempFile = new File(tempName);
        if(tempFile.exists()) {
            return tempFile;
        }

        runCommand(transformer.getCommand(), file, tempFile);
        return tempFile;
    }

    public void copyConvertMov(File source, File destination, String originalName, String originalMd5, Long originalSize, LocalDateTime originalDate) throws IOException, InterruptedException {
        String copyCommand = applicationProperties.getFfmpegCommand();

        String description = String.format("converted from %s at %s",
                originalName,
                originalDate != null ? originalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "unknown");

        String metadataFlags = String.format(
                "-metadata jbr_original_file='%s' " +
                "-metadata jbr_original_file_md5='%s' " +
                "-metadata jbr_original_file_size='%s' " +
                "-metadata description='%s' ",
                originalName,
                originalMd5 != null ? originalMd5 : "",
                originalSize != null ? originalSize : 0L,
                description);

        copyCommand = copyCommand.replace("%%OUTPUT%%", metadataFlags + "%%OUTPUT%%");
        runCommand(copyCommand, source, destination);

        if (originalDate != null) {
            writeExifDate(destination, originalDate);
        }
    }

    public boolean writeExifDate(File file, LocalDateTime dateTime) {
        try {
            String formatted = dateTime.format(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"));
            Process process = new ProcessBuilder(
                    "exiftool",
                    "-DateTimeOriginal=" + formatted,
                    "-CreateDate=" + formatted,
                    "-overwrite_original",
                    file.getPath()
            ).redirectErrorStream(true).start();
            boolean completed = process.waitFor(1L, TimeUnit.MINUTES);
            int exitCode = process.exitValue();
            process.destroyForcibly();
            return completed && exitCode == 0;
        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to write EXIF date to {}", file, e);
            return false;
        }
    }

    public boolean writeExifLocation(File file, double latitude, double longitude) {
        try {
            String latRef = latitude >= 0 ? "N" : "S";
            String lonRef = longitude >= 0 ? "E" : "W";
            Process process = new ProcessBuilder(
                    "exiftool",
                    "-GPSLatitude=" + Math.abs(latitude),
                    "-GPSLatitudeRef=" + latRef,
                    "-GPSLongitude=" + Math.abs(longitude),
                    "-GPSLongitudeRef=" + lonRef,
                    "-overwrite_original",
                    file.getPath()
            ).redirectErrorStream(true).start();
            boolean completed = process.waitFor(1L, TimeUnit.MINUTES);
            int exitCode = process.exitValue();
            process.destroyForcibly();
            return completed && exitCode == 0;
        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to write EXIF location to {}", file, e);
            return false;
        }
    }
}
