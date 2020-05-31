package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class DriveManager {
    final static private Logger LOG = LoggerFactory.getLogger(DriveManager.class);

    final private DirectoryRepository directoryRepository;
    final private FileRepository fileRepository;
    final private SourceRepository sourceRepository;
    final private ClassificationRepository classificationRepository;
    final private SynchronizeRepository synchronizeRepository;
    final private BackupManager backupManager;
    final public ApplicationProperties applicationProperties;
    final private ActionConfirmRepository actionConfirmRepository;

    @Autowired
    public DriveManager(DirectoryRepository directoryRepository,
                        FileRepository fileRepository,
                        SourceRepository sourceRepository,
                        ClassificationRepository classificationRepository,
                        SynchronizeRepository synchronizeRepository,
                        BackupManager backupManager,
                        ApplicationProperties applicationProperties,
                        ActionConfirmRepository actionConfirmRepository ) {
        this.directoryRepository = directoryRepository;
        this.fileRepository = fileRepository;
        this.sourceRepository = sourceRepository;
        this.classificationRepository = classificationRepository;
        this.synchronizeRepository = synchronizeRepository;
        this.backupManager = backupManager;
        this.applicationProperties = applicationProperties;
        this.actionConfirmRepository = actionConfirmRepository;
    }

    private Classification classifyFile(FileInfo file, Iterable<Classification> classifications)  {
        for(Classification nextClassification : classifications) {
            if(nextClassification.fileMatches(file)) {
                return nextClassification;
            }
        }

        return null;
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

    private String getMD5(Path path, Classification classification) {
        if(classification == null || !classification.getUseMD5()) {
            return "";
        }

        try {
            // Calculate the MD5 for the file.
            MessageDigest md = MessageDigest.getInstance("MD5");

            DigestInputStream dis = new DigestInputStream(Files.newInputStream(path),md);
            //noinspection StatementWithEmptyBody
            while(dis.read() != -1);
            md = dis.getMessageDigest();

            return bytesToHex(md.digest());
        } catch (Exception ex) {
            LOG.error("Failed to get MD5, ",ex);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Cannot get MD5 - " + path.toString());
        }

        return "";
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

    private void processPath(Path path, Source nextSource, Iterable<Classification> classifications) {
        String directoryName = "";

        if(path.toFile().isDirectory()) {
            if(path.toAbsolutePath().toString().equals(nextSource.getPath())) {
                return;
            }

            directoryName = path.toAbsolutePath().toString().replace(nextSource.getPath(),"");
        } else {
            directoryName = path.toAbsolutePath().getParent().toString().replace(nextSource.getPath(),"");
        }

        if(!passFilter(directoryName,nextSource)) {
            return;
        }

        // Get the directory.
        Optional<DirectoryInfo> directory = directoryRepository.findBySourceAndPath(nextSource,directoryName);

        if(!directory.isPresent()) {
            DirectoryInfo newDirectoryInfo = new DirectoryInfo();
            newDirectoryInfo.setPath(directoryName);
            newDirectoryInfo.setSource(nextSource);

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
            // Get the file
            FileInfo newFile = new FileInfo();
            newFile.setName(path.getFileName().toString());
            newFile.setDirectoryInfo(directory.get());
            newFile.setClassification(classifyFile(newFile,classifications));
            newFile.setDate(fileDate);
            newFile.setSize(path.toFile().length());
            newFile.setMD5(getMD5(path,newFile.getClassification()));
            newFile.clearRemoved();

            fileRepository.save(newFile);
        } else {
            if(file.get().getClassification() == null) {
                Classification newClassification = classifyFile(file.get(),classifications);

                if(newClassification != null) {
                    file.get().setClassification(newClassification);
                }
            }

            if((file.get().getSize() != path.toFile().length()) ||
                    (file.get().getDate().compareTo(fileDate) != 0)) {
                file.get().setSize(path.toFile().length());
                file.get().setDate(fileDate);
                file.get().setMD5(getMD5(path,file.get().getClassification()));
            }

            file.get().clearRemoved();
            fileRepository.save(file.get());
        }

        LOG.info(path.toString());
    }

    @Scheduled(cron = "#{@applicationProperties.gatherSchedule}")
    public void gatherCron() {
        if(applicationProperties.getSynchronizeEnabled()) {
            try {
                gather();
            } catch (Exception ex) {
                LOG.error("Failed to gather",ex);
            }
        }
    }

    private void setSourceStatus(Source source, String status) {
        try {
            source.setStatus(status);
            sourceRepository.save(source);
        } catch(Exception ignored) {

        }
    }

    public void gather() throws IOException {
        Iterable<Source> sources = sourceRepository.findAll();

        Iterable<Classification> classifications = classificationRepository.findAll();

        fileRepository.markAllRemoved();
        directoryRepository.markAllRemoved();

        for(Source nextSource: sources) {
            if(nextSource.getStatus() != null && nextSource.getStatus().equals("GATHERING")) {
                continue;
            }

            setSourceStatus(nextSource,"GATHERING");

            backupManager.postWebLog(BackupManager.webLogLevel.INFO, "Gather - " + nextSource.getPath());

            // If the source does not exist, create it.
            createDirectory(nextSource.getPath());

            // Read directory structure into the database.
            try (Stream<Path> paths = Files.walk(Paths.get(nextSource.getPath()))) {
                paths
                   .forEach(path -> processPath(path,nextSource,classifications));
            } catch (IOException e) {
                setSourceStatus(nextSource,"ERROR");
                backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to gather + " + e.toString());
                throw e;
            }

            setSourceStatus(nextSource,"OK");
        }

        fileRepository.deleteRemoved();
        directoryRepository.deleteRemoved();
    }

    @Scheduled(cron = "#{@applicationProperties.synchronizeSchedule}")
    public void synchronizeCron()  {
        if(applicationProperties.getSynchronizeEnabled()) {
            try {
                synchronize();
            } catch (Exception ex) {
                LOG.error("Failed to syncrhonize",ex);
            }
        }
    }

    private boolean copyFile(FileInfo source, FileInfo destination) {
        if(destination == null) {
            return true;
        }

        // If the files have an MD5 and its different then copy.
        String sourceMD5 = "";
        if(source.getMD5() != null) {
            sourceMD5 = source.getMD5();
        }

        String destinationMD5 = "";
        if(destination.getMD5() != null) {
            destinationMD5 = destination.getMD5();
        }

        if(sourceMD5.length() > 0 && sourceMD5.equals(destinationMD5)) {
            // MD 5 matches - do not copy
            return false;
        }

        return source.getSize() != destination.getSize();
    }

    private void equalizeDate(FileInfo source, FileInfo destination) {
        if(destination == null) {
            return;
        }

        // Must have an MD5
        if(source.getMD5() == null || source.getMD5().length() == 0) {
            return;
        }

        // Name must be equal?
        if(!source.getName().equalsIgnoreCase(destination.getName())) {
            return;
        }

        // Must be the same size.
        if(source.getSize() != destination.getSize()) {
            return;
        }

        // Must have the same MD5
        if(!source.getMD5().equals(destination.getMD5())) {
            return;
        }

        // Dates must be different
        if(source.getDate().compareTo(destination.getDate()) == 0) {
            return;
        }

        LOG.info("Updating date " + source.getDate().getTime() + " - " + destination.getDate().getTime());
        // Make the date of the destination, equal to the source.
        File destinationFile = new File(destination.getDirectoryInfo().getSource().getPath() + "/" +
                destination.getDirectoryInfo().getPath() + "/" +
                destination.getName() );
        //noinspection ResultOfMethodCallIgnored
        destinationFile.setLastModified(source.getDate().getTime());
    }

    private void createDirectory(String path) {
        File directory = new File(path);
        if(!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
    }

    private void backup(SynchronizeStatus status) {
        try {
            LOG.info("Process backup - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());

            // Update the last modified time if necessary.
            equalizeDate(status.sourceFile, status.destinationFile);

            // Does the file need to be copied?
            if (copyFile(status.sourceFile, status.destinationFile)) {
                LOG.info("Copy File - " + status.sourceFile.toString());

                String sourceFilename = status.source.getPath() + "/" + status.sourceFile.getDirectoryInfo().getPath() + "/" + status.sourceFile.getName();
                String destinationFilename = status.destination.getPath() + "/" + status.sourceFile.getDirectoryInfo().getPath() + "/" + status.sourceFile.getName();

                createDirectory(status.destination.getPath() + "/" + status.sourceFile.getDirectoryInfo().getPath());

                LOG.info("Copy file from " + sourceFilename + " to " + destinationFilename);

                Files.copy(Paths.get(sourceFilename),
                        Paths.get(destinationFilename),
                        REPLACE_EXISTING);

                // Set the last modified date on the copied file to be the same as the source.
                File destinationFile = new File(destinationFilename);
                //noinspection ResultOfMethodCallIgnored
                destinationFile.setLastModified(status.sourceFile.getDate().getTime());
            }
        } catch(Exception ex) {
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to backup " + status.toString());
        }
    }

    private void warn(SynchronizeStatus status) {
        LOG.warn("File warning- " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
        backupManager.postWebLog(BackupManager.webLogLevel.WARN,"File warning - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
    }

    private void removeIfBackedup(SynchronizeStatus status) {
        // Does this exist on the remote server?
        if(status.destinationFile != null) {
            status.sourceFile = status.destinationFile;
            delete(status,false, true);
        }
    }

    private void deleteFileIfConfirmed(String name) {
        File file = new File(name);

        if(file.exists()) {
            // Has this action been confirmed?
            List<ActionConfirm> confirmedActions = actionConfirmRepository.findByPathAndAction(name,"DELETE");

            if(confirmedActions.size() > 0) {
                boolean confirmed = false;
                for(ActionConfirm nextConfirm: confirmedActions) {
                    if(nextConfirm.confirmed()) {
                        confirmed = true;
                    }
                }

                if(confirmed) {
                    // Delete the file.
                    LOG.info("Delete the file - " + file );
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();

                    for(ActionConfirm nextConfirm: confirmedActions) {
                        actionConfirmRepository.delete(nextConfirm);
                    }
                }
            } else {
                // Create an action to be confirmed.
                ActionConfirm actionConfirm = new ActionConfirm();
                actionConfirm.setPath(name);
                actionConfirm.setAction("DELETE");
                actionConfirm.setConfirmed(false);

                actionConfirmRepository.save(actionConfirm);
            }
        }
    }

    private void deleteDestinationFolder(SynchronizeStatus status) {
        LOG.info("Delete the destination folder - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
        String folderName = status.sourceFile.getDirectoryInfo().getSource().getPath() + "/" + status.sourceFile.getDirectoryInfo().getPath();

        deleteFileIfConfirmed(folderName);
    }

    private void delete(SynchronizeStatus status, boolean standard, boolean classified) {
        LOG.info("File should be deleted - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());

        if(!standard) {
            LOG.info("File should be deleted (should not have been copied) - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
            backupManager.postWebLog(BackupManager.webLogLevel.INFO,"File should be deleted (should not be there) - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
        }
        if(!classified) {
            LOG.info("File should be deleted (should not have been copied - unclassified) - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
            backupManager.postWebLog(BackupManager.webLogLevel.INFO,"File should be deleted (should not be there - unclassified) - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
        }
        backupManager.postWebLog(BackupManager.webLogLevel.INFO,"File should be deleted - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());

        String filename = status.sourceFile.getDirectoryInfo().getSource().getPath() + "/" + status.sourceFile.getDirectoryInfo().getPath() + "/" + status.sourceFile.getName();
        deleteFileIfConfirmed(filename);
    }

    public void synchronize() {
        Iterable<Synchronize> synchronizes = synchronizeRepository.findAll();

        for(Synchronize nextSynchronize : synchronizes) {
            backupManager.postWebLog(BackupManager.webLogLevel.INFO,"Synchronize - " + nextSynchronize.getSource().getPath() + " -> " + nextSynchronize.getDestination().getPath());

            if(nextSynchronize.getSource().getStatus() == null || !nextSynchronize.getSource().getStatus().equals("OK")) {
                backupManager.postWebLog(BackupManager.webLogLevel.WARN,"Skipping as source not OK");
                continue;
            }

            if(nextSynchronize.getDestination().getStatus() == null || !nextSynchronize.getDestination().getStatus().equals("OK")) {
                backupManager.postWebLog(BackupManager.webLogLevel.WARN,"Skipping as destination not OK");
                continue;
            }

            for(SynchronizeStatus nextStatus: fileRepository.findSynchronizeStatus(nextSynchronize.getId())) {
                // Perform the appropriate actions
                switch(nextStatus.classification.getAction()) {
                    case "BACKUP":
                        backup(nextStatus);
                        break;

                    case "IGNORE":
                        removeIfBackedup(nextStatus);
                        break;

                    case "DELETE":
                        delete(nextStatus,true, true);
                        break;

                    case "WARN":
                        removeIfBackedup(nextStatus);
                        warn(nextStatus);
                        break;

                    case "FOLDER":
                        break;

                    default:
                        LOG.warn("Unexpected action - " + nextStatus.classification.getAction() + " " + nextStatus.sourceDirectory.getPath() + " " + nextStatus.sourceFile.getName());
                        backupManager.postWebLog(BackupManager.webLogLevel.WARN,"Unexpected action - " + nextStatus.classification.getAction());
                }
            }

            for(SynchronizeStatus nextStatus: fileRepository.findSynchronizeExtraFiles(nextSynchronize.getId())) {
                // These are files that should not exists.
                if(nextStatus.classification == null) {
                    delete(nextStatus,false, false);
                    continue;
                }

                // Perform the appropriate actions
                switch(nextStatus.classification.getAction()) {
                    case "BACKUP":
                    case "IGNORE":
                    case "DELETE":
                    case "WARN":
                        delete(nextStatus,false, true);
                        break;

                    case "FOLDER":
                        deleteDestinationFolder(nextStatus);
                        break;

                    default:
                        LOG.warn("Unexpected action - " + nextStatus.classification.getAction() + " " + nextStatus.sourceDirectory.getPath() + " " + nextStatus.sourceFile.getName());
                        backupManager.postWebLog(BackupManager.webLogLevel.WARN,"Unexpected action - " + nextStatus.classification.getAction());
                }
            }

            LOG.info(nextSynchronize.getSource().getPath() + " -> " + nextSynchronize.getDestination().getPath());
        }
    }
}
