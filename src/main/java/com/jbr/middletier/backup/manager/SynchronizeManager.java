package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.SourceStatusType;
import com.jbr.middletier.backup.data.Synchronize;
import com.jbr.middletier.backup.data.SynchronizeStatus;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dto.SyncDataDTO;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.compare.DbTree;
import com.jbr.middletier.backup.filetree.compare.node.DbCompareNode;
import com.jbr.middletier.backup.filetree.compare.node.RwDbCompareNode;
import com.jbr.middletier.backup.filetree.compare.node.SectionNode;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class SynchronizeManager {
    private static final Logger LOG = LoggerFactory.getLogger(SynchronizeManager.class);

    private final AssociatedFileDataManager associatedFileDataManager;
    private final BackupManager backupManager;
    private final FileRepository fileRepository;
    private final DirectoryRepository directoryRepository;
    private final ActionManager actionManager;

    @Autowired
    public SynchronizeManager(AssociatedFileDataManager associatedFileDataManager,
                              BackupManager backupManager,
                              FileRepository fileRepository,
                              DirectoryRepository directoryRepository,
                              ActionManager actionManager) {
        this.associatedFileDataManager = associatedFileDataManager;
        this.backupManager = backupManager;
        this.fileRepository = fileRepository;
        this.directoryRepository = directoryRepository;
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
        LOG.warn("File warning- {}/{}", status.getSourceDirectory().getName(), status.getSourceFile().getName());
        backupManager.postWebLog(BackupManager.webLogLevel.WARN, String.format("File warning - %s/%s", status.getSourceDirectory().getName(), status.getSourceFile().getName()));
    }

    private void delete(SynchronizeStatus status, boolean standard, boolean classified) {
        LOG.info("File should be deleted - {}/{}", status.getSourceDirectory().getName(), status.getSourceFile().getName());

        if(!standard) {
            LOG.info("File should be deleted (should not have been copied) - {}/{}", status.getSourceDirectory().getName(), status.getSourceFile().getName());
            backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format("File should be deleted (should not be there) - %s/%s", status.getSourceDirectory().getName(), status.getSourceFile().getName()));
        }
        if(!classified) {
            LOG.info("File should be deleted (should not have been copied - unclassified) - {}/{}", status.getSourceDirectory().getName(), status.getSourceFile().getName());
            backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format("File should be deleted (should not be there - unclassified) - %s/%s", status.getSourceDirectory().getName(), status.getSourceFile().getName()));
        }
        backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format("File should be deleted - %s/%s", status.getSourceDirectory().getName(), status.getSourceFile().getName()));

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
            LOG.info("Process backup - {}/{}", status.getSourceDirectory().getName(), status.getSourceFile().getName());

            // Update the last modified time if necessary.
            equalizeDate(status.getSourceFile(), status.getDestinationFile());

            // Does the file need to be copied?
            if (copyFile(status.getSourceFile(), status.getDestinationFile())) {
                LOG.info("Copy File - {}", status.getSourceFile());

                String sourceFilename = status.getSourceFile().getFullFilename();
                //TODO fix this
                String destinationFilename = String.format("%s/%s/%s",
                        status.getDestination().getPath(),
                        status.getSourceFile().getParentId().getId(),
                        status.getSourceFile().getName() );

                File directory = new File(String.format("%s/%s", status.getDestination().getPath(), status.getSourceFile().getParentId().getId()));
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

    private void processSynchronizeStatusAtSource(SynchronizeStatus nextStatus) {
        if(nextStatus.getClassification() == null) {
            LOG.warn("Unknown classification on file {}", nextStatus.getSourceFile().getIdAndType());
            return;
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
                LOG.warn("Unexpected action - {} {} {}", nextStatus.getClassification().getAction(), nextStatus.getSourceDirectory().getName(), nextStatus.getSourceFile().getName());
                backupManager.postWebLog(BackupManager.webLogLevel.WARN, String.format("Unexpected action - %s", nextStatus.getClassification().getAction()));
        }
    }

    private void processSynchronizeStatusAtDestination(SynchronizeStatus nextStatus) {
        // These are files that should not exists.
        if(nextStatus.getClassification() == null) {
            delete(nextStatus,false, false);
            return;
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
                LOG.warn("Unexpected action - {} {} {}", nextStatus.getClassification().getAction(), nextStatus.getSourceDirectory().getName(), nextStatus.getSourceFile().getName());
                backupManager.postWebLog(BackupManager.webLogLevel.WARN,String.format("Unexpected action - %s", nextStatus.getClassification().getAction()));
        }
    }

    private void deleteFile(DbCompareNode node) {
        // Delete the file specified in the node.
        backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format("File should be deleted - %s/%s", status.getSourceDirectory().getName(), status.getSourceFile().getName()));

        actionManager.deleteFileIfConfirmed();
    }

    private void deleteDirectory(DbCompareNode node) {

    }

    private void copyFile(DbCompareNode node) {

    }

    private void copyDirectory(DbCompareNode node) {

    }

    private SyncDataDTO processSynchronize(Synchronize nextSynchronize) {
        SyncDataDTO result = new SyncDataDTO();
        result.setSyncId(nextSynchronize.getId());

        try {
            backupManager.postWebLog(BackupManager.webLogLevel.INFO, "Synchronize - " + nextSynchronize.getSource().getPath() + " -> " + nextSynchronize.getDestination().getPath());

            if (nextSynchronize.getSource().getStatus() == null || !SourceStatusType.SST_OK.equals(nextSynchronize.getSource().getStatus())) {
                backupManager.postWebLog(BackupManager.webLogLevel.WARN, "Skipping as source not OK");
                result.setFailed();
                return result;
            }

            if (nextSynchronize.getDestination().getStatus() == null || !SourceStatusType.SST_OK.equals(nextSynchronize.getDestination().getStatus())) {
                backupManager.postWebLog(BackupManager.webLogLevel.WARN, "Skipping as destination not OK");
                result.setFailed();
                return result;
            }

            // Compare the source and destination.
            DbRoot source = new DbRoot(nextSynchronize.getSource(), fileRepository, directoryRepository);
            DbRoot destination = new DbRoot(nextSynchronize.getDestination(), fileRepository, directoryRepository);

            DbTree dbTree = new DbTree(source, destination);
            dbTree.compare();

            // Process the comparison.
            SectionNode.SectionNodeType section = SectionNode.SectionNodeType.UNKNOWN;
            for (FileTreeNode nextNode : dbTree.getOrderedNodeList()) {
                if (nextNode instanceof DbCompareNode) {
                    DbCompareNode compareNode = (DbCompareNode) nextNode;
                    switch (section) {
                        case FILE_FOR_REMOVE:
                            deleteFile(compareNode);
                            result.incrementFilesDeleted();
                            break;
                        case DIRECTORY_FOR_REMOVE:
                            deleteDirectory(compareNode);
                            result.incrementDirectoriesDeleted();
                            break;
                        case DIRECTORY_FOR_INSERT:
                            copyDirectory(compareNode);
                            result.incrementFilesCopied();
                            break;
                        case FILE_FOR_INSERT:
                            copyFile(compareNode);
                            result.incrementDirectoriesCopied();
                            break;
                    }
                } else if (nextNode instanceof SectionNode) {
                    SectionNode sectionNode = (SectionNode) nextNode;
                    section = sectionNode.getSection();
                }
            }

            LOG.info("{} -> {}", nextSynchronize.getSource().getPath(), nextSynchronize.getDestination().getPath());
        } catch (Exception e) {
            LOG.warn("Failure in {} -> {} {}", nextSynchronize.getSource().getPath(), nextSynchronize.getDestination().getPath(), e);
            result.setFailed();
        }

        return result;
    }

    public List<SyncDataDTO> synchronize() {
        List<SyncDataDTO> result = new ArrayList<>();

        for(Synchronize nextSynchronize : associatedFileDataManager.internalFindAllSynchronize()) {
            result.add(processSynchronize(nextSynchronize));
        }

        return result;
    }
}
