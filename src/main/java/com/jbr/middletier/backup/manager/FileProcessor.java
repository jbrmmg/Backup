package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.filetree.*;
import com.jbr.middletier.backup.filetree.compare.RwDbTree;
import com.jbr.middletier.backup.filetree.compare.node.RwDbCompareNode;
import com.jbr.middletier.backup.filetree.compare.node.SectionNode;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import com.jbr.middletier.backup.filetree.realworld.RwFile;
import com.jbr.middletier.backup.filetree.realworld.RwNode;
import com.jbr.middletier.backup.filetree.realworld.RwRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

abstract class FileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FileProcessor.class);

    final FileSystemObjectManager fileSystemObjectManager;
    final DbLoggingManager dbLoggingManager;
    final ActionManager actionManager;
    final AssociatedFileDataManager associatedFileDataManager;
    final FileSystem fileSystem;

    FileProcessor(DbLoggingManager dbLoggingManager,
                  ActionManager actionManager,
                  AssociatedFileDataManager associatedFileDataManager,
                  FileSystemObjectManager fileSystemObjectManager,
                  FileSystem fileSystem) {
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.dbLoggingManager = dbLoggingManager;
        this.actionManager = actionManager;
        this.associatedFileDataManager = associatedFileDataManager;
        this.fileSystem = fileSystem;
    }

    abstract FileInfo createNewFile();

    private void processDeletesIteratively(FileTreeNode node, List<ActionConfirm> deletes, List<ActionConfirm> performed, GatherDataDTO gatherData) {
        // Process the children.
        for(FileTreeNode next: node.getChildren()) {
            processDeletesIteratively(next,deletes,performed,gatherData);
        }

        // Only process if RW DB Compare.
        if (!(node instanceof RwDbCompareNode)) {
            return;
        }

        // Only nodes that are the same as the DB can be deleted
        if(((RwDbCompareNode) node).getActionType() != RwDbCompareNode.ActionType.NONE) {
            return;
        }

        // Get details of the file
        RwDbCompareNode compareNode = (RwDbCompareNode)node;

        // Is this file marked for delete?
        for(ActionConfirm next : deletes) {
            if(next.confirmed() &&
                    compareNode.getDatabaseObjectId().getId().equals(next.getPath().getIdAndType().getId())) {
                Optional<File> fileToDelete = compareNode.getFileForDelete();
                if(fileToDelete.isPresent()) {
                    fileSystem.deleteFile(fileToDelete.get(), gatherData);

                    if (!fileSystem.fileExists(fileToDelete.get())) {
                        // If the file has been removed, then remove the action.
                        actionManager.actionPerformed(next);
                        performed.add(next);
                        gatherData.increment(GatherDataDTO.GatherDataCountType.DELETES);
                    }
                }
            }
        }
    }

    private void processDeletes(RootFileTreeNode details, List<ActionConfirm> deletes, GatherDataDTO gatherData) {
        // If there are no deletes then there is nothing to do.
        if(deletes == null || deletes.isEmpty()) {
            return;
        }

        List<ActionConfirm> performedActions = new ArrayList<>();

        processDeletesIteratively(details,deletes,performedActions, gatherData);
        deletes.removeAll(performedActions);
    }

    private void processFileRemoval(RwDbCompareNode node) {
        // Delete this file from the database.
        Optional<FileSystemObject> existingFile = fileSystemObjectManager.findFileSystemObject(node.getDatabaseObjectId());

        existingFile.ifPresent(fileSystemObjectManager::delete);
    }

    private void processDirectoryRemoval(RwDbCompareNode node) {
        Optional<FileSystemObject> existingDirectory = fileSystemObjectManager.findFileSystemObject(node.getDatabaseObjectId());

        existingDirectory.ifPresent(fileSystemObjectManager::delete);
    }

    private RwNode getRwNode(RwDbCompareNode node) {
        Objects.requireNonNull(node.getRealWorldNode(),"NPE: Cannot add or update directory with no real world object.");
        return node.getRealWorldNode();
    }

    private Optional<FileSystemObjectId> getParentId(RwDbCompareNode node) {
        FileTreeNode parentNode = node.getParent();
        Optional<FileSystemObjectId> parentId = Optional.empty();
        if(parentNode instanceof RwDbCompareNode) {
            RwDbCompareNode rwDbParentNode = (RwDbCompareNode)parentNode;

            Objects.requireNonNull(rwDbParentNode.getDatabaseObjectId(),"NPE: cannot add or update directory with no known parent.");

            parentId = Optional.of(rwDbParentNode.getDatabaseObjectId());
        } else if(parentNode instanceof RwDbTree) {
            RwDbTree rwDbSource = (RwDbTree)parentNode;

            parentId = Optional.of(rwDbSource.getDbSource().getSource().getIdAndType());
        }

        Objects.requireNonNull(parentId,"NPE: Unable to determine the directory parent id.");

        return parentId;
    }

    private void processDirectoryAddUpdate(RwDbCompareNode node) {
        // If there is a database object then read it first.
        Optional<FileSystemObject> existingDirectory = Optional.empty();
        if(node.getDatabaseObjectId() != null) {
            existingDirectory = fileSystemObjectManager.findFileSystemObject(node.getDatabaseObjectId());
        }

        if(!existingDirectory.isPresent()) {
            existingDirectory = Optional.of(new DirectoryInfo());
        }

        // Get the real world object.
        RwNode rwNode = getRwNode(node);

        // Insert a new directory.
        if(!rwNode.getName().isPresent())
            throw new IllegalStateException("Cannot insert a directory with no name.");

        DirectoryInfo directory = (DirectoryInfo) existingDirectory.get();
        directory.setName(rwNode.getName().orElse(""));
        directory.setParentId(getParentId(node).orElse(null));

        fileSystemObjectManager.save(directory);

        // Store the id of this item.
        node.setDatabaseObjectId(directory);
    }

    private void processFileAddUpdate(RwDbCompareNode node, boolean skipMD5) {
        // If there is a database object then read it first.
        Optional<FileSystemObject> existingFile = Optional.empty();
        if(node.getDatabaseObjectId() != null) {
            existingFile = fileSystemObjectManager.findFileSystemObject(node.getDatabaseObjectId());
        }

        if(!existingFile.isPresent()) {
            existingFile = Optional.of(createNewFile());
        }

        // Get the real world object.
        RwFile rwNode = (RwFile)getRwNode(node);

        if(!rwNode.getName().isPresent())
            throw new IllegalStateException("Cannot insert a file with no name.");

        FileInfo file = (FileInfo) existingFile.get();
        file.setName(rwNode.getName().orElse(""));
        file.setParentId(getParentId(node).orElse(null));

        if(file.getClassification() == null) {
            Optional<Classification> newClassification = associatedFileDataManager.classifyFile(file);
            newClassification.ifPresent(file::setClassification);
        }

        LocalDateTime fileDate = Instant.ofEpochMilli(rwNode.getFile().lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime();

        long timeDifference = 100;
        if(file.getDate() != null) {
            timeDifference = ChronoUnit.SECONDS.between(fileDate, file.getDate());
        }

        if((file.getSize() == null) || (file.getSize().compareTo(rwNode.getFile().length()) != 0) || (Math.abs(timeDifference) > 1)) {
            file.setSize(rwNode.getFile().length());
            file.setDate(fileDate);
            if(!skipMD5) {
                file.setMD5(fileSystem.getClassifiedFileMD5(rwNode.getFile().toPath(), file.getClassification()));
            }
        }

        fileSystemObjectManager.save(file);

        // Store the id of this item.
        node.setDatabaseObjectId(existingFile.get());
    }

    protected void updateDatabase(Source source, List<ActionConfirm> deletes, boolean skipMD5, GatherDataDTO gatherData) throws IOException {
        // Read the files structure from the real world.
        LOG.info("Read the real world {}", source.getPath());
        RwRoot realWorld = new RwRoot(source.getPath(), fileSystem);
        realWorld.removeFilteredChildren(source.getFilter());

        // Read the same from the database.
        LOG.info("Read the database {}", source);
        DbRoot database = fileSystemObjectManager.createDbRoot(source);

        // Compare the real world with the database.
        LOG.info("Perform the compare.");
        RwDbTree compare = new RwDbTree(realWorld, database);
        compare.compare();

        // Perform deletes
        LOG.info("Process the deletes.");
        processDeletes(compare,deletes, gatherData);

        // Process the actions.
        SectionNode.SectionNodeType section = null;
        List<FileTreeNode> orderedNodeList = compare.getOrderedNodeList();
        LOG.info("Actions {}", orderedNodeList.size());
        for(FileTreeNode nextNode : orderedNodeList) {
            if(nextNode instanceof RwDbCompareNode) {
                RwDbCompareNode compareNode = (RwDbCompareNode)nextNode;
                switch(Objects.requireNonNull(section,"Section has not been initialised")) {
                    case FILE_FOR_REMOVE:
                        processFileRemoval(compareNode);
                        gatherData.increment(GatherDataDTO.GatherDataCountType.FILES_REMOVED);
                        break;
                    case DIRECTORY_FOR_REMOVE:
                        processDirectoryRemoval(compareNode);
                        gatherData.increment(GatherDataDTO.GatherDataCountType.DIRECTORIES_REMOVED);
                        break;
                    case DIRECTORY_FOR_INSERT:
                        processDirectoryAddUpdate(compareNode);
                        gatherData.increment(GatherDataDTO.GatherDataCountType.DIRECTORIES_INSERTED);
                        break;
                    case FILE_FOR_INSERT:
                        processFileAddUpdate(compareNode, skipMD5);
                        gatherData.increment(GatherDataDTO.GatherDataCountType.FILES_INSERTED);
                        break;
                }
            } else {
                SectionNode sectionNode = (SectionNode)nextNode;
                section = sectionNode.getSection();
                LOG.info("Check the next section {}", section);
            }
        }
        LOG.info("Source complete.");
    }
}
