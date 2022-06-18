package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.exception.FileProcessException;
import com.jbr.middletier.backup.exception.MissingFileSystemObject;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

abstract class FileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FileProcessor.class);

    final FileSystemObjectManager fileSystemObjectManager;
    final BackupManager backupManager;
    final ActionManager actionManager;
    final AssociatedFileDataManager associatedFileDataManager;

    FileProcessor(BackupManager backupManager,
                  ActionManager actionManager,
                  AssociatedFileDataManager associatedFileDataManager,
                  FileSystemObjectManager fileSystemObjectManager) {
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.backupManager = backupManager;
        this.actionManager = actionManager;
        this.associatedFileDataManager = associatedFileDataManager;
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

    protected MD5 getMD5(Path path, Classification classification) {
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
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Cannot get MD5 - " + path.toString());
        }

        return new MD5();
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
            if(next.confirmed() && compareNode.getDatabaseObjectId().getId().equals(next.getPath().getIdAndType().getId())) {
                // Get details of the file that needs to be deleted.
                if (compareNode.deleteRwFile()) {
                    // Remove the action.
                    actionManager.actionPerformed(next);
                    performed.add(next);
                    gatherData.incrementDeletes();
                }
            }
        }
    }

    private void processDeletes(RootFileTreeNode details, List<ActionConfirm> deletes, GatherDataDTO gatherData) {
        // If there are no deletes then there is nothing to do.
        if(deletes == null || deletes.size() == 0) {
            return;
        }

        List<ActionConfirm> performedActions = new ArrayList<>();

        processDeletesIteratively(details,deletes,performedActions, gatherData);
        deletes.removeAll(performedActions);

        // If there are still confirmed deletes to perform then they are invalid, so delete them anyway.
        for(ActionConfirm next: deletes) {
            if(next.confirmed()) {
                LOG.warn("Action cannot be performed: " + next);
                actionManager.actionPerformed(next);
            }
        }
    }

    private void processFileRemoval(RwDbCompareNode node) throws MissingFileSystemObject {
        // Delete this file from the database.
        Optional<FileSystemObject> existingFile = fileSystemObjectManager.findFileSystemObject(node.getDatabaseObjectId(), false);

        existingFile.ifPresent(fileSystemObjectManager::delete);
    }

    private void processDirectoryRemoval(RwDbCompareNode node) throws MissingFileSystemObject {
        Optional<FileSystemObject> existingDirectory = fileSystemObjectManager.findFileSystemObject(node.getDatabaseObjectId(), false);

        existingDirectory.ifPresent(fileSystemObjectManager::delete);
    }

    private RwNode getRwNode(RwDbCompareNode node) throws FileProcessException {
        if(node.getRealWorldNode() == null) {
            throw new FileProcessException("Cannot add or update directory with no real world object.");
        }

        return node.getRealWorldNode();
    }

    private FileSystemObjectId getParentIt(RwDbCompareNode node) throws FileProcessException {
        FileTreeNode parentNode = node.getParent();
        FileSystemObjectId parentId = null;
        if(parentNode instanceof RwDbCompareNode) {
            RwDbCompareNode rwDbParentNode = (RwDbCompareNode)parentNode;

            if(rwDbParentNode.getDatabaseObjectId() == null) {
                throw new FileProcessException("Cannot add or update directory with no known parent.");
            }

            parentId = rwDbParentNode.getDatabaseObjectId();
        } else if(parentNode instanceof RwDbTree) {
            RwDbTree rwDbSource = (RwDbTree)parentNode;

            parentId = rwDbSource.getDbSource().getSource().getIdAndType();
        }

        if(parentId == null) {
            throw new FileProcessException("Unable to determine the directory parent id.");
        }

        return parentId;
    }

    private void processDirectoryAddUpdate(RwDbCompareNode node) throws FileProcessException, MissingFileSystemObject {
        // If there is a database object then read it first.
        Optional<FileSystemObject> existingDirectory = Optional.empty();
        if(node.getDatabaseObjectId() != null) {
            existingDirectory = fileSystemObjectManager.findFileSystemObject(node.getDatabaseObjectId(), false);
        }

        if(!existingDirectory.isPresent()) {
            existingDirectory = Optional.of(new DirectoryInfo());
        }

        // Get the real world object.
        RwNode rwNode = getRwNode(node);

        // Insert a new directory.
        DirectoryInfo directory = (DirectoryInfo) existingDirectory.get();
        directory.setName(rwNode.getName());
        directory.setParentId(getParentIt(node));
        directory.clearRemoved();

        fileSystemObjectManager.save(directory);

        // Store the id of this item.
        node.setDatabaseObjectId(directory);
    }

    private void processFileAddUpdate(RwDbCompareNode node, boolean skipMD5) throws FileProcessException, MissingFileSystemObject {
        // If there is a database object then read it first.
        Optional<FileSystemObject> existingFile = Optional.empty();
        if(node.getDatabaseObjectId() != null) {
            existingFile = fileSystemObjectManager.findFileSystemObject(node.getDatabaseObjectId(), false);
        }

        if(!existingFile.isPresent()) {
            existingFile = Optional.of(createNewFile());
        }

        // Get the real world object.
        RwFile rwNode = (RwFile)getRwNode(node);

        FileInfo file = (FileInfo) existingFile.get();
        file.setName(rwNode.getName());
        file.setParentId(getParentIt(node));
        file.clearRemoved();

        if(file.getClassification() == null) {
            Classification newClassification = associatedFileDataManager.classifyFile(file);

            if(newClassification != null) {
                file.setClassification(newClassification);
            }
        }

        Date fileDate = new Date(rwNode.getFile().lastModified());
        long dbTime = file.getDate() == null ? 0 : file.getDate().getTime() / 1000;
        long fileTime = fileDate.getTime() / 1000;

        if((file.getSize() == null) || (file.getSize().compareTo(rwNode.getFile().length()) != 0) || (Math.abs(dbTime - fileTime) > 1)) {
            file.setSize(rwNode.getFile().length());
            file.setDate(fileDate);
            if(!skipMD5) {
                file.setMD5(getMD5(rwNode.getFile().toPath(), file.getClassification()));
            }
        }

        fileSystemObjectManager.save(file);

        // Store the id of this item.
        node.setDatabaseObjectId(existingFile.get());
    }

    protected void updateDatabase(Source source, List<ActionConfirm> deletes, boolean skipMD5, GatherDataDTO gatherData) throws IOException, FileProcessException, MissingFileSystemObject {
        // Read the files structure from the real world.
        RwRoot realWorld = new RwRoot(source.getPath(), backupManager);
        realWorld.removeFilteredChildren(source.getFilter());

        // Read the same from the database.
        DbRoot database = fileSystemObjectManager.createDbRoot(source);

        // Compare the real world with the database.
        RwDbTree compare = new RwDbTree(realWorld, database);
        compare.compare();

        // Perform deletes
        processDeletes(compare,deletes, gatherData);

        // Process the actions.
        SectionNode.SectionNodeType section = SectionNode.SectionNodeType.UNKNOWN;
        for(FileTreeNode nextNode : compare.getOrderedNodeList()) {
            if(nextNode instanceof RwDbCompareNode) {
                RwDbCompareNode compareNode = (RwDbCompareNode)nextNode;
                switch(section) {
                    case FILE_FOR_REMOVE:
                        processFileRemoval(compareNode);
                        gatherData.incrementFilesRemoved();
                        break;
                    case DIRECTORY_FOR_REMOVE:
                        processDirectoryRemoval(compareNode);
                        gatherData.incrementDirectoriesRemoved();
                        break;
                    case DIRECTORY_FOR_INSERT:
                        processDirectoryAddUpdate(compareNode);
                        gatherData.incrementDirectoriesInserted();
                        break;
                    case FILE_FOR_INSERT:
                        processFileAddUpdate(compareNode, skipMD5);
                        gatherData.incrementFilesInserted();
                        break;
                }
            } else if (nextNode instanceof SectionNode) {
                SectionNode sectionNode = (SectionNode)nextNode;
                section = sectionNode.getSection();
            }
        }
    }

    protected void createDirectory(String path) {
        File directory = new File(path);
        if(!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
    }
}
