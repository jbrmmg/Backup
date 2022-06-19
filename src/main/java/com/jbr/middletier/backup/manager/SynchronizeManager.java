package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.SyncDataDTO;
import com.jbr.middletier.backup.exception.MissingFileSystemObject;
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class SynchronizeManager {
    private static final Logger LOG = LoggerFactory.getLogger(SynchronizeManager.class);

    private final AssociatedFileDataManager associatedFileDataManager;
    private final BackupManager backupManager;
    private final ActionManager actionManager;
    private final FileSystemObjectManager fileSystemObjectManager;

    @Autowired
    public SynchronizeManager(AssociatedFileDataManager associatedFileDataManager,
                              BackupManager backupManager,
                              FileSystemObjectManager fileSystemObjectManager,
                              ActionManager actionManager) {
        this.associatedFileDataManager = associatedFileDataManager;
        this.backupManager = backupManager;
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.actionManager = actionManager;
    }

    private void warn(DbCompareNode node) {
        LOG.warn("File warning- {}/{}", node.getSource().getFSO().getName(), node.getSource().getFSO().getIdAndType());
        backupManager.postWebLog(BackupManager.webLogLevel.WARN, String.format("File warning - %s/%s", node.getSource().getFSO().getName(), node.getSource().getFSO().getIdAndType()));
    }

    private void equalizeDate(DbCompareNode node) throws MissingFileSystemObject {
        FileInfo sourceFileInfo = (FileInfo)node.getSource().getFSO();

        LOG.info("Updating date {} -> {} {}", sourceFileInfo.getDate().getTime(), node.getDestination().getFSO().getName(), node.getDestination().getFSO().getIdAndType());
        // Make the date of the destination, equal to the source.
        File destinationFile = fileSystemObjectManager.getFile(node.getDestination().getFSO());
        if(!destinationFile.setLastModified(sourceFileInfo.getDate().getTime())) {
            LOG.warn("Failed to set the last modified date - {}", destinationFile);
        }
    }

    private void backup(DbCompareNode node, Source destination) {
        try {
            FileInfo sourceFileInfo = (FileInfo)node.getSource().getFSO();

            LOG.info("Process backup - {}", node);

            File sourceFile = fileSystemObjectManager.getFile(node.getSource().getFSO());
            File destinationFile = fileSystemObjectManager.getFileAtDestination(node.getSource().getFSO(), destination);

            LOG.info("Copy file from {} to {}", sourceFile, destinationFile);

            Files.copy(sourceFile.toPath(),
                    destinationFile.toPath(),
                    REPLACE_EXISTING);

            // Set the last modified date on the copied file to be the same as the source.
            if(!destinationFile.setLastModified(sourceFileInfo.getDate().getTime())) {
                LOG.warn("Failed to set the last modified date {}", destinationFile);
            }
        } catch(Exception ex) {
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to backup " + node.toString());
        }
    }

    private void removeSource(DbCompareNode node) throws MissingFileSystemObject {
        if(!(node.getSource() instanceof DbFile)) {
            LOG.warn("Remove Source called, but not a file");
            return;
        }

        DbFile dbFile = (DbFile)node.getSource();
        File file = fileSystemObjectManager.getFile(dbFile.getFSO());

        try {
            if(file.exists()) {
                Files.deleteIfExists(file.toPath());
            }
        } catch (IOException e) {
            LOG.warn("Failed to delete file {}", file);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,String.format("File should be deleted - %s %s", dbFile.getFSO().getName(), dbFile.getFSO().getIdAndType()));
        }
    }

    private void deleteFile(DbCompareNode node) throws MissingFileSystemObject {
        if(!(node.getDestination() instanceof DbFile)) {
            LOG.warn("Delete file called, but not a file");
            return;
        }

        DbFile dbFile = (DbFile)node.getDestination();

        // Delete the file specified in the node.
        backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format("File should be deleted - %s %s", dbFile.getFSO().getName(), dbFile.getFSO().getIdAndType()));
        actionManager.deleteFileIfConfirmed((FileInfo)dbFile.getFSO());
    }

    private void deleteDirectory(DbCompareNode node) throws MissingFileSystemObject {
        if(!(node.getDestination() instanceof DbDirectory)) {
            LOG.warn("Delete directory called, but not a directory");
            return;
        }

        DbDirectory dbDirectory = (DbDirectory)node.getDestination();

        // Delete the file specified in the node.
        backupManager.postWebLog(BackupManager.webLogLevel.INFO,String.format("File should be deleted - %s %s", dbDirectory.getFSO().getName(), dbDirectory.getFSO().getIdAndType()));
        actionManager.deleteFileIfConfirmed((FileInfo)dbDirectory.getFSO());
    }

    private void copyFile(DbCompareNode node, Source destination) throws MissingFileSystemObject {
        // Action depends on the sub action.
        switch(node.getSubActionType()) {
            case NONE:
                backup(node, destination);
                break;
            case DATE_UPDATE:
                equalizeDate(node);
                break;
            case WARN:
                warn(node);
                break;
            case REMOVE_SOURCE:
                removeSource(node);
                break;
        }
    }

    private void createDestinationDirectory(DbCompareNode node, Source destination) throws MissingFileSystemObject, IOException {
        // Create the directory at the destination.
        if(!(node.getSource() instanceof DbDirectory)) {
            LOG.warn("Delete directory called, but not a directory");
            return;
        }

        DbDirectory dbDirectory = (DbDirectory)node.getSource();

        // Create this directory.
        File directory = fileSystemObjectManager.getFileAtDestination(dbDirectory.getFSO(), destination);

        if(Files.exists(directory.toPath())) {
            return;
        }

        Files.createDirectories(directory.toPath());
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
            SectionNode.SectionNodeType section = SectionNode.SectionNodeType.UNKNOWN;
            for (FileTreeNode nextNode : dbTree.getOrderedNodeList()) {
                if (nextNode instanceof DbCompareNode) {
                    DbCompareNode compareNode = (DbCompareNode) nextNode;
                    switch (section) {
                        case FILE_FOR_REMOVE:
                            deleteFile(compareNode);
                            result.increment(SyncDataDTO.SyncDataCountType.FILES_DELETED);
                            break;
                        case DIRECTORY_FOR_REMOVE:
                            deleteDirectory(compareNode);
                            result.increment(SyncDataDTO.SyncDataCountType.DIRECTORIES_DELETED);
                            break;
                        case DIRECTORY_FOR_INSERT:
                            createDestinationDirectory(compareNode, nextSynchronize.getDestination());
                            result.increment(SyncDataDTO.SyncDataCountType.DIRECTORIES_COPIED);
                            break;
                        case FILE_FOR_INSERT:
                            copyFile(compareNode, nextSynchronize.getDestination());
                            result.increment(SyncDataDTO.SyncDataCountType.FILES_COPIED);
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
