package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Optional;

abstract class FileProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(FileProcessor.class);

    final DirectoryRepository directoryRepository;
    final FileRepository fileRepository;
    final BackupManager backupManager;
    private final ActionManager actionManager;

    FileProcessor(DirectoryRepository directoryRepository,
                  FileRepository fileRepository,
                  BackupManager backupManager,
                  ActionManager actionManager) {
        this.directoryRepository = directoryRepository;
        this.fileRepository = fileRepository;
        this.backupManager = backupManager;
        this.actionManager = actionManager;
    }

    private boolean passFilter(String directory, Source nextSource) {
        // Is there a filter?
        if(nextSource.getFilter() != null && nextSource.getFilter().length() > 0 ) {
            // The filter is applied to the first directory after the source.
            String pathToFilter = directory.replace(nextSource.getPath(),"");

            String[] folders = pathToFilter.split("/");

            int index = 0;
            String filterFolder = "";
            while( (filterFolder.length() == 0) && (index < folders.length) ) {
                filterFolder = folders[index];
                index++;
            }

            if(filterFolder.length() > 0) {
                // Does it meet the filter?
                if(!filterFolder.matches(nextSource.getFilter())) {
                    LOG.trace("here");
                    return false;
                }
            }
        }

        return true;
    }

    private Classification classifyFile(FileInfo file, Iterable<Classification> classifications)  {
        for(Classification nextClassification : classifications) {
            if(nextClassification.fileMatches(file)) {
                return nextClassification;
            }
        }

        return null;
    }

    private boolean deleteFileIfRequired(FileInfo file, List<ActionConfirm> deletes) {
        for(ActionConfirm nextAction: deletes) {
            if(nextAction.getPath().getId().equals(file.getId())) {
                LOG.info("Deleteing the file " + file.getFullFilename());
                actionManager.deleteFileIfConfirmed(file);
                return true;
            }
        }

        return false;
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

    String getMD5(Path path, Classification classification) {
        if(classification == null || !classification.getUseMD5()) {
            return "";
        }

        try {
            // Calculate the MD5 for the file.
            MessageDigest md = MessageDigest.getInstance("MD5");

            try(DigestInputStream dis = new DigestInputStream(Files.newInputStream(path),md) ) {
                //noinspection StatementWithEmptyBody
                while (dis.read() != -1) ;
                md = dis.getMessageDigest();
            }

            return bytesToHex(md.digest());
        } catch (Exception ex) {
            LOG.error("Failed to get MD5, ",ex);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Cannot get MD5 - " + path.toString());
        }

        return "";
    }

    private String determineDirectoryName(Path path, Source nextSource) {
        if(path.toFile().isDirectory()) {
            if(path.toAbsolutePath().toString().equals(nextSource.getPath())) {
                // This is the source
                return "";
            }

            // Return the path
            return path.toAbsolutePath().toString().replace(nextSource.getPath(), "");
        }

        // Directory name from file.
        return path.toAbsolutePath().getParent().toString().replace(nextSource.getPath(),"");
    }

    abstract void newFileInserted(FileInfo newFile);

    private void createFile(Path path, DirectoryInfo directory, Iterable<Classification> classifications, Date fileDate, boolean skipMD5) {
        // Get the file
        FileInfo newFile = new FileInfo();
        newFile.setName(path.getFileName().toString());
        newFile.setDirectoryInfo(directory);
        newFile.setClassification(classifyFile(newFile,classifications));
        newFile.setDate(fileDate);
        newFile.setSize(path.toFile().length());
        if(!skipMD5) {
            newFile.setMD5(getMD5(path, newFile.getClassification()));
        }
        newFile.clearRemoved();

        fileRepository.save(newFile);

        newFileInserted(newFile);
    }

    private void updateFile(Path path, FileInfo file, Date fileDate, List<ActionConfirm> deletes, Iterable<Classification> classifications, boolean skipMD5) {
        // Has this file been marked for delete?
        if(deleteFileIfRequired(file,deletes)) {
            fileRepository.delete(file);
            return;
        }

        if(file.getClassification() == null) {
            Classification newClassification = classifyFile(file,classifications);

            if(newClassification != null) {
                file.setClassification(newClassification);
            }
        }

        long dbTime = file.getDate().getTime() / 1000;
        long fileTime = fileDate.getTime() / 1000;

        if((file.getSize().compareTo(path.toFile().length()) != 0) ||
                (Math.abs(dbTime - fileTime) > 1)) {
            file.setSize(path.toFile().length());
            file.setDate(fileDate);
            if(!skipMD5) {
                file.setMD5(getMD5(path, file.getClassification()));
            }
        }

        file.clearRemoved();
        fileRepository.save(file);
    }

    void processPath(Path path, List<ActionConfirm> deletes, Source nextSource, Iterable<Classification> classifications, boolean skipMD5) {
        String directoryName = determineDirectoryName(path,nextSource);

        if(!passFilter(directoryName,nextSource)) {
            return;
        }

        // Get the directory.
        Optional<DirectoryInfo> directory = directoryRepository.findBySourceAndPath(nextSource,directoryName);

        if(!directory.isPresent()) {
            DirectoryInfo newDirectoryInfo = new DirectoryInfo();
            newDirectoryInfo.setPath(directoryName);
            newDirectoryInfo.setSource(nextSource);
            newDirectoryInfo.clearRemoved();

            directory = Optional.of(directoryRepository.save(newDirectoryInfo));
        } else {
            directory.get().clearRemoved();
            directoryRepository.save(directory.get());
        }

        // Check the folder file.
        Optional<FileInfo> directoryFile = fileRepository.findByDirectoryInfoAndName(directory.get(),".");
        if(!directoryFile.isPresent()) {
            // Create a file to represent the folder.
            directoryFile = Optional.of(new FileInfo());
            directoryFile.get().setName(".");
            directoryFile.get().setDirectoryInfo(directory.get());
            directoryFile.get().setClassification(classifyFile(directoryFile.get(),classifications));
        }

        directoryFile.get().clearRemoved();
        fileRepository.save(directoryFile.get());

        // If this is a directory, we are done.
        if(path.toFile().isDirectory()) {
            return;
        }

        Date fileDate = new Date(path.toFile().lastModified());

        // Does the file exist?
        Optional<FileInfo> file = fileRepository.findByDirectoryInfoAndName(directory.get(),path.getFileName().toString());

        if(!file.isPresent()) {
            createFile(path,directory.get(),classifications,fileDate,skipMD5);
        } else {
            updateFile(path,file.get(),fileDate,deletes,classifications,skipMD5);
        }

        LOG.info(path.toString());
    }

    void createDirectory(String path) {
        File directory = new File(path);
        if(!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
    }
}
