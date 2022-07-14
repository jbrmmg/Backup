package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.SyncDataDTO;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.compare.DbTree;
import com.jbr.middletier.backup.filetree.compare.node.DbCompareNode;
import com.jbr.middletier.backup.filetree.compare.node.SectionNode;
import com.jbr.middletier.backup.filetree.database.DbDirectory;
import com.jbr.middletier.backup.filetree.database.DbFile;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class SynchronizeManager {
    private static final Logger LOG = LoggerFactory.getLogger(SynchronizeManager.class);

    private final AssociatedFileDataManager associatedFileDataManager;
    private final BackupManager backupManager;
    private final ActionManager actionManager;
    private final FileSystemObjectManager fileSystemObjectManager;
    private final FileSystem fileSystem;
    private static final String ERROR_FORMAT = "File should be deleted - %s %s";

    @Autowired
    public SynchronizeManager(AssociatedFileDataManager associatedFileDataManager,
                              BackupManager backupManager,
                              FileSystemObjectManager fileSystemObjectManager,
                              ActionManager actionManager,
                              FileSystem fileSystem) {
        this.associatedFileDataManager = associatedFileDataManager;
        this.backupManager = backupManager;
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.actionManager = actionManager;
        this.fileSystem = fileSystem;
    }

    private void warn(DbCompareNode node, SyncDataDTO result) {
        result.increment(SyncDataDTO.SyncDataCountType.FILES_WARNED);
        LOG.warn("File warning- {}/{}", node.getSource().getFSO().getName(), node.getSource().getFSO().getIdAndType());
        backupManager.postWebLog(BackupManager.webLogLevel.WARN, String.format("File warning - %s/%s", node.getSource().getFSO().getName(), node.getSource().getFSO().getIdAndType()));
    }

    private void equalizeDate(DbCompareNode node, SyncDataDTO result) {
        result.increment(SyncDataDTO.SyncDataCountType.DATES_UPDATED);
        FileInfo sourceFileInfo = (FileInfo)node.getSource().getFSO();

        LOG.info("Updating date {} -> {} {}", sourceFileInfo.getDate().getTime(), node.getDestination().getFSO().getName(), node.getDestination().getFSO().getIdAndType());
        // Make the date of the destination, equal to the source.
        File destinationFile = fileSystemObjectManager.getFile(node.getDestination().getFSO());
        if(!fileSystem.setFileDateTime(destinationFile,sourceFileInfo.getDate().getTime())) {
            LOG.warn("Failed to set the last modified date - {}", destinationFile);
            result.setProblems();
        }
    }

    private void backup(DbCompareNode node, Source destination, SyncDataDTO result) {
        try {
            result.increment(SyncDataDTO.SyncDataCountType.FILES_COPIED);
            FileInfo sourceFileInfo = (FileInfo)node.getSource().getFSO();

            LOG.info("Process backup - {}", node);

            File sourceFile = fileSystemObjectManager.getFile(node.getSource().getFSO());
            File destinationFile = fileSystemObjectManager.getFileAtDestination(node.getSource().getFSO(), destination);

            LOG.info("Copy file from {} to {}", sourceFile, destinationFile);
            fileSystem.copyFile(sourceFile, destinationFile, result);

            // Set the last modified date on the copied file to be the same as the source.
            if(!fileSystem.setFileDateTime(destinationFile,sourceFileInfo.getDate().getTime())) {
                LOG.warn("Failed to set the last modified date {}", destinationFile);
                result.setProblems();
            }
        } catch(Exception ex) {
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to backup " + node.toString());
        }
    }

    private void removeSource(DbCompareNode node, SyncDataDTO result) {
        result.increment(SyncDataDTO.SyncDataCountType.SOURCES_REMOVED);
        DbFile dbFile = (DbFile)node.getSource();
        fileSystem.deleteFile(fileSystemObjectManager.getFile(dbFile.getFSO()), result);
    }

    private void deleteFile(DbCompareNode node, SyncDataDTO result) {
        result.increment(SyncDataDTO.SyncDataCountType.FILES_DELETED);
        DbFile dbFile = (DbFile)node.getDestination();

        // Delete the file specified in the node.
        backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format(ERROR_FORMAT, dbFile.getFSO().getName(), dbFile.getFSO().getIdAndType()));
        actionManager.deleteFileIfConfirmed((FileInfo)dbFile.getFSO(), result);

        // If there is a sub-action of remove source then that should be deleted too.
        if(node.getSubActionType().equals(DbCompareNode.SubActionType.REMOVE_SOURCE)) {
            removeSource(node, result);
        }
    }

    private void deleteDirectory(DbCompareNode node, SyncDataDTO result) throws IOException {
        result.increment(SyncDataDTO.SyncDataCountType.DIRECTORIES_DELETED);
        DbDirectory dbDirectory = (DbDirectory)node.getDestination();
        File directory = fileSystemObjectManager.getFile(dbDirectory.getFSO());

        // Delete the file specified in the node.
        backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format(ERROR_FORMAT, dbDirectory.getFSO().getName(), dbDirectory.getFSO().getIdAndType()));
        fileSystem.deleteDirectoryIfEmpty(directory);
    }

    private void copyFile(DbCompareNode node, Source destination, SyncDataDTO result) {
        // Action depends on the sub action.
        switch(node.getSubActionType()) {
            case NONE:
                backup(node, destination, result);
                break;
            case DATE_UPDATE:
                equalizeDate(node, result);
                break;
            case WARN:
                warn(node, result);
                break;
            case REMOVE_SOURCE:
                removeSource(node, result);
                break;
            default:
                // There is nothing else to process.
        }
    }

    private void createDestinationDirectory(DbCompareNode node, Source destination, SyncDataDTO result) throws IOException {
        // Create the directory at the destination.
        result.increment(SyncDataDTO.SyncDataCountType.DIRECTORIES_COPIED);
        DbDirectory dbDirectory = (DbDirectory)node.getSource();

        // Create this directory.
        File directory = fileSystemObjectManager.getFileAtDestination(dbDirectory.getFSO(), destination);
        fileSystem.createDirectory(directory.toPath());
    }

    private SyncDataDTO processSynchronize(Synchronize nextSynchronize) {
        SyncDataDTO result = new SyncDataDTO(nextSynchronize.getId());

        try {
            backupManager.postWebLog(BackupManager.webLogLevel.INFO, "Synchronize - " + nextSynchronize.getSource().getPath() + " -> " + nextSynchronize.getDestination().getPath());

            if (nextSynchronize.getSource().getStatus() == null || !SourceStatusType.SST_OK.equals(nextSynchronize.getSource().getStatus())) {
                backupManager.postWebLog(BackupManager.webLogLevel.WARN, "Skipping as source not OK");
                result.setProblems();
                return result;
            }

            if (nextSynchronize.getDestination().getStatus() == null || !SourceStatusType.SST_OK.equals(nextSynchronize.getDestination().getStatus())) {
                backupManager.postWebLog(BackupManager.webLogLevel.WARN, "Skipping as destination not OK");
                result.setProblems();
                return result;
            }

            // Compare the source and destination.
            DbRoot source = fileSystemObjectManager.createDbRoot(nextSynchronize.getSource());
            DbRoot destination = fileSystemObjectManager.createDbRoot(nextSynchronize.getDestination());

            DbTree dbTree = new DbTree(source, destination);
            dbTree.compare();

            // Process the comparison.
            SectionNode.SectionNodeType section = null;
            List<FileTreeNode> orderedNodeList = dbTree.getOrderedNodeList();
            LOG.info("Actions {}", orderedNodeList.size());
            for (FileTreeNode nextNode : orderedNodeList) {
                if (nextNode instanceof DbCompareNode) {
                    DbCompareNode compareNode = (DbCompareNode) nextNode;
                    switch (Objects.requireNonNull(section,"Section not initialised")) {
                        case FILE_FOR_REMOVE:
                            deleteFile(compareNode, result);
                            break;
                        case DIRECTORY_FOR_REMOVE:
                            deleteDirectory(compareNode, result);
                            break;
                        case DIRECTORY_FOR_INSERT:
                            createDestinationDirectory(compareNode, nextSynchronize.getDestination(), result);
                            break;
                        case FILE_FOR_INSERT:
                            copyFile(compareNode, nextSynchronize.getDestination(), result);
                            break;
                    }
                } else {
                    SectionNode sectionNode = (SectionNode) nextNode;
                    section = sectionNode.getSection();
                }
            }

            LOG.info("{} -> {}", nextSynchronize.getSource().getPath(), nextSynchronize.getDestination().getPath());
        } catch (Exception e) {
            LOG.warn("Failure in {} -> {} {}", nextSynchronize.getSource().getPath(), nextSynchronize.getDestination().getPath(), e);
            result.setProblems();
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
