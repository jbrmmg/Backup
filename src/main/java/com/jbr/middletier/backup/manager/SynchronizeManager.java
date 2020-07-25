package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.Synchronize;
import com.jbr.middletier.backup.data.SynchronizeStatus;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.SynchronizeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class SynchronizeManager {
    private static final Logger LOG = LoggerFactory.getLogger(SynchronizeManager.class);

    private final SynchronizeRepository synchronizeRepository;
    private final BackupManager backupManager;
    private final FileRepository fileRepository;
    private final ActionManager actionManager;

    @Autowired
    public SynchronizeManager(SynchronizeRepository synchronizeRepository,
                              BackupManager backupManager,
                              FileRepository fileRepository,
                              ActionManager actionManager) {
        this.synchronizeRepository = synchronizeRepository;
        this.backupManager = backupManager;
        this.fileRepository = fileRepository;
        this.actionManager = actionManager;
    }

    private void removeIfBackedup(SynchronizeStatus status) {
        // Does this exist on the remote server?
        if(status.getDestinationFile() != null) {
            status.setSourceFile(status.getDestinationFile());
            delete(status,false, true);
        }
    }

    private void warn(SynchronizeStatus status) {
        LOG.warn("File warning- {}/{}", status.getSourceDirectory().getPath(), status.getSourceFile().getName());
        backupManager.postWebLog(BackupManager.webLogLevel.WARN, String.format("File warning - %s/%s", status.getSourceDirectory().getPath(), status.getSourceFile().getName()));
    }

    private void delete(SynchronizeStatus status, boolean standard, boolean classified) {
        LOG.info("File should be deleted - {}/{}", status.getSourceDirectory().getPath(), status.getSourceFile().getName());

        if(!standard) {
            LOG.info("File should be deleted (should not have been copied) - {}/{}", status.getSourceDirectory().getPath(), status.getSourceFile().getName());
            backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format("File should be deleted (should not be there) - %s/%s", status.getSourceDirectory().getPath(), status.getSourceFile().getName()));
        }
        if(!classified) {
            LOG.info("File should be deleted (should not have been copied - unclassified) - {}/{}", status.getSourceDirectory().getPath(), status.getSourceFile().getName());
            backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format("File should be deleted (should not be there - unclassified) - %s/%s", status.getSourceDirectory().getPath(), status.getSourceFile().getName()));
        }
        backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format("File should be deleted - %s/%s", status.getSourceDirectory().getPath(), status.getSourceFile().getName()));

        actionManager.deleteFileIfConfirmed(status.getSourceFile());
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

        return !source.getSize().equals(destination.getSize());
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
        if(!source.getSize().equals(destination.getSize())) {
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

        LOG.info("Updating date {} - {}", source.getDate().getTime(), destination.getDate().getTime());
        // Make the date of the destination, equal to the source.
        File destinationFile = new File( destination.getFullFilename() );
        if(!destinationFile.setLastModified(source.getDate().getTime())) {
            LOG.warn("Failed to set the last modified date - {}", destination.getFullFilename());
        }
    }

    private void backup(SynchronizeStatus status) {
        try {
            LOG.info("Process backup - {}/{}", status.getSourceDirectory().getPath(), status.getSourceFile().getName());

            // Update the last modified time if necessary.
            equalizeDate(status.getSourceFile(), status.getDestinationFile());

            // Does the file need to be copied?
            if (copyFile(status.getSourceFile(), status.getDestinationFile())) {
                LOG.info("Copy File - {}", status.getSourceFile().toString());

                String sourceFilename = status.getSourceFile().getFullFilename();
                String destinationFilename = String.format("%s/%s/%s",
                        status.getDestination().getPath(),
                        status.getSourceFile().getDirectoryInfo().getPath(),
                        status.getSourceFile().getName() );

                File directory = new File(String.format("%s/%s", status.getDestination().getPath(), status.getSourceFile().getDirectoryInfo().getPath()));
                if(!directory.exists() && !directory.mkdirs() ) {
                    LOG.warn("Make directories failed.");
                }

                LOG.info("Copy file from {} to {}", sourceFilename, destinationFilename);

                Files.copy(Paths.get(sourceFilename),
                        Paths.get(destinationFilename),
                        REPLACE_EXISTING);

                // Set the last modified date on the copied file to be the same as the source.
                File destinationFile = new File(destinationFilename);
                if(!destinationFile.setLastModified(status.getSourceFile().getDate().getTime())) {
                    LOG.warn("Failed to set the last modified date {}", destinationFilename);
                }
            }
        } catch(Exception ex) {
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to backup " + status.toString());
        }
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
                if(nextStatus.getClassification() == null) {
                    LOG.warn("Unknown classification on file {}", nextStatus.getSourceFile().getId());
                    continue;
                }

                // Perform the appropriate actions
                switch(nextStatus.getClassification().getAction()) {
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
                        LOG.warn("Unexpected action - {} {} {}", nextStatus.getClassification().getAction(), nextStatus.getSourceDirectory().getPath(), nextStatus.getSourceFile().getName());
                        backupManager.postWebLog(BackupManager.webLogLevel.WARN, String.format("Unexpected action - %s", nextStatus.getClassification().getAction()));
                }
            }

            for(SynchronizeStatus nextStatus: fileRepository.findSynchronizeExtraFiles(nextSynchronize.getId())) {
                // These are files that should not exists.
                if(nextStatus.getClassification() == null) {
                    delete(nextStatus,false, false);
                    continue;
                }

                // Perform the appropriate actions
                switch(nextStatus.getClassification().getAction()) {
                    case "BACKUP":
                    case "IGNORE":
                    case "DELETE":
                    case "WARN":
                        delete(nextStatus,false, true);
                        break;

                    case "FOLDER":
                        actionManager.deleteFileIfConfirmed(nextStatus.getSourceFile());
                        break;

                    default:
                        LOG.warn("Unexpected action - {} {} {}", nextStatus.getClassification().getAction(), nextStatus.getSourceDirectory().getPath(), nextStatus.getSourceFile().getName());
                        backupManager.postWebLog(BackupManager.webLogLevel.WARN,String.format("Unexpected action - %s", nextStatus.getClassification().getAction()));
                }
            }

            LOG.info("{} -> {}", nextSynchronize.getSource().getPath(), nextSynchronize.getDestination().getPath());
        }
    }
}
