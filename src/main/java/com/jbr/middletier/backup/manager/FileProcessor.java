package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.filetree.*;
import com.jbr.middletier.backup.filetree.compare.RwDbTree;
import com.jbr.middletier.backup.filetree.compare.node.RwDbCompareNode;
import com.jbr.middletier.backup.filetree.database.DbRoot;
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

    final DirectoryRepository directoryRepository;
    final FileRepository fileRepository;
    final BackupManager backupManager;
    final ActionManager actionManager;
    final AssociatedFileDataManager associatedFileDataManager;

    FileProcessor(DirectoryRepository directoryRepository,
                  FileRepository fileRepository,
                  BackupManager backupManager,
                  ActionManager actionManager,
                  AssociatedFileDataManager associatedFileDataManager) {
        this.directoryRepository = directoryRepository;
        this.fileRepository = fileRepository;
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

    protected String getMD5(Path path, Classification classification) {
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

    abstract void newFileInserted(FileInfo newFile);

    private void createFile(Path path, FileSystemObject parent, boolean skipMD5) {
        Date fileDate = new Date(path.toFile().lastModified());

        // Get the file
        FileInfo newFile = new FileInfo();
        newFile.setName(path.getFileName().toString());
        newFile.setParentId(parent);
        newFile.setClassification(associatedFileDataManager.classifyFile(newFile));
        newFile.setDate(fileDate);
        newFile.setSize(path.toFile().length());
        if(!skipMD5) {
            newFile.setMD5(getMD5(path, newFile.getClassification()));
        }
        newFile.clearRemoved();

        fileRepository.save(newFile);

        newFileInserted(newFile);
    }

    private void updateFile(Path path, FileInfo file, boolean skipMD5) {
        Date fileDate = new Date(path.toFile().lastModified());

        if(file.getClassification() == null) {
            Classification newClassification = associatedFileDataManager.classifyFile(file);

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

    private void performDatabaseRemove(FileTreeNode toBeRemoved) {
        // Remove the item from the database (either a file or a directory)
        if(toBeRemoved.isDirectory()) {
            Optional<DirectoryInfo> existingDirectory = directoryRepository.findById(toBeRemoved.getId());

            existingDirectory.ifPresent(directoryRepository::delete);
        } else {
            Optional<FileInfo> existingFile = fileRepository.findById(toBeRemoved.getId());

            existingFile.ifPresent(fileRepository::delete);
        }
    }

    private FileSystemObject getParentDirectory(FileTreeNode node, Source source) {
        // If there is no parent, then return the source.
        if(node.getParent() == null) {
            return source;
        }

        if(node.getParent().isDirectory() && node.getParent().hasValidId()) {
            Optional<DirectoryInfo> existing = directoryRepository.findById(node.getParent().getId());
            if(existing.isPresent()) {
                return existing.get();
            }
        }

        // If no parent then return the source.
        return source;
    }

    private void performDbAddOrUpdDirectory(Source source, FileTreeNode toBeUpdated) {
        if(toBeUpdated.hasValidId() && toBeUpdated.getCompareStatus() == FileTreeNode.CompareStatusType.UPDATED) {
            Optional<DirectoryInfo> existingDirectory = directoryRepository.findById(toBeUpdated.getId());

            if(existingDirectory.isPresent()) {
                //TODO - perform the update.
                directoryRepository.save(existingDirectory.get());
                return;
            }
        }

        // Insert a new directory.
        DirectoryInfo newDirectoryInfo = new DirectoryInfo();
        newDirectoryInfo.setName(toBeUpdated.getName());
        newDirectoryInfo.setParentId(getParentDirectory(toBeUpdated,source));
        newDirectoryInfo.clearRemoved();

        directoryRepository.save(newDirectoryInfo);

        toBeUpdated.setId(newDirectoryInfo);
    }

    private void performDatabaseAddOrUpdate(Source source, FileTreeNode toBeUpdated, boolean skipMD5) {
        if(toBeUpdated.isDirectory()) {
            performDbAddOrUpdDirectory(source, toBeUpdated);
            return;
        }

        // Is this a create or update?
        Optional<FileInfo> existingFile = Optional.empty();

        if(toBeUpdated.hasValidId()) {
            existingFile = fileRepository.findById(toBeUpdated.getId());
        }

        if(existingFile.isPresent()) {
            updateFile(toBeUpdated.getPath(),existingFile.get(),skipMD5);
        } else {
            createFile(toBeUpdated.getPath(),getParentDirectory(toBeUpdated, source),skipMD5);
        }
    }

    private void performDatabaseUpdate(Source source, FileTreeNode compare, boolean skipMD5) {
        // If adding, then add now and then the children.
        if((compare.getCompareStatus() == FileTreeNode.CompareStatusType.ADDED) ||
                (compare.getCompareStatus() == FileTreeNode.CompareStatusType.UPDATED) ||
                (compare.getCompareStatus() == FileTreeNode.CompareStatusType.CHANGE_TO_FILE) ||
                (compare.getCompareStatus() == FileTreeNode.CompareStatusType.CHANGE_TO_DIRECTORY)) {
            // Insert or update
            performDatabaseAddOrUpdate(source, compare, skipMD5);
        }

        // Process the children.
        for(FileTreeNode next: compare.getChildren()) {
            // If the child compare status is unknown then copy from parent.
            if(next.getCompareStatus() == FileTreeNode.CompareStatusType.UNKNOWN) {
                next.setCompareStatus(compare.getCompareStatus());
            }

            performDatabaseUpdate(source, next, skipMD5);
        }

        // If removing, then remove after the children.
        if((compare.getCompareStatus() == FileTreeNode.CompareStatusType.REMOVED) ||
                (compare.getCompareStatus() == FileTreeNode.CompareStatusType.CHANGE_TO_FILE) ||
                (compare.getCompareStatus() == FileTreeNode.CompareStatusType.CHANGE_TO_DIRECTORY)) {
            performDatabaseRemove(compare);
        }
    }

    private void processDeletesIteratively(FileTreeNode node, List<ActionConfirm> deletes, List<ActionConfirm> performed) {
        // Only process nodes of type RwDbCompareNode
        if(!(node instanceof RwDbCompareNode)) {
            return;
        }

        // Process the children.
        for(FileTreeNode next: node.getChildren()) {
            processDeletesIteratively(next,deletes,performed);
        }

        // Only nodes that are the same as the DB can be deleted
        if(((RwDbCompareNode) node).getActionType() != RwDbCompareNode.ActionType.NONE) {
            return;
        }

        // Get details of the file
        RwDbCompareNode compareNode = (RwDbCompareNode)node;

        // Is this file marked for delete?
        for(ActionConfirm next : deletes) {
            if(next.confirmed() && compareNode.getDatabaseObjectId().getId() == next.getPath().getIdAndType().getId()) {
                // Get details of the file that needs to be deleted.
                if (compareNode.deleteRwFile()) {
                    // Remove the action.
                    actionManager.actionPerformed(next);
                    performed.add(next);
                }
            }
        }
    }

    private void processDeletes(RootFileTreeNode details, List<ActionConfirm> deletes) {
        // If there are no deletes then there is nothing to do.
        if(deletes == null || deletes.size() == 0) {
            return;
        }

        List<ActionConfirm> performedActions = new ArrayList<>();

        processDeletesIteratively(details,deletes,performedActions);
        deletes.removeAll(performedActions);

        // If there are still confirmed deletes to perform then they are invalid, so delete them anyway.
        for(ActionConfirm next: deletes) {
            if(next.confirmed()) {
                LOG.warn("Action cannot be performed: " + next);
                actionManager.actionPerformed(next);
            }
        }
    }

    private void processFileRemoval(List<FileTreeNode> compareNodeList) {
        for(FileTreeNode nextNode: compareNodeList) {
            if(nextNode instanceof RwDbCompareNode) {
                // Delete this file from the database.
            }
        }
    }

    private void processDirectoryRemoval(List<FileTreeNode> compareNodeList) {
        for(FileTreeNode nextNode: compareNodeList) {
            if(nextNode instanceof DelDbDirectory) {
                // Delete this direct from the database.
            }
        }
    }

    private void processDirectoryAddUpdate(List<FileTreeNode> compareNodeList) {
        for(FileTreeNode nextNode: compareNodeList) {
            if(nextNode instanceof AddUpdDbDirectory) {
                // Delete this direct from the database.
            }
        }
    }

    private void processFileAddUpdate(List<FileTreeNode> compareNodeList) {
        for(FileTreeNode nextNode: compareNodeList) {
            if(nextNode instanceof AddUpdDbFile) {
                // Delete this direct from the database.
            }
        }
    }

    protected void updateDatabase(Source source, List<ActionConfirm> deletes, boolean skipMD5) throws IOException {
        // Read the files structure from the real world.
        RwRoot realWorld = new RwRoot(source.getPath(), backupManager);
        realWorld.removeFilteredChildren(source.getFilter());

        // Read the same from the database.
        DbRoot database = new DbRoot(source, fileRepository, directoryRepository);

        // Compare the real world with the database.
        RwDbTree compare = new RwDbTree();
        compare.compare(realWorld, database);

        // Perform deletes
        processDeletes(compare,deletes);

        // Get the ordered node list (they will be in the order they should be processed)
        List<FileTreeNode> orderedNodeList = compare.getOrderedNodeList();

        // Process the deletes
        processFileRemoval(compareNodeList);
        processDirectoryRemoval(compareNodeList);

        // Process the updates and adds
        processDirectoryAddUpdate(compareNodeList);
        processFileAddUpdate(compareNodeList);
    }

    protected void createDirectory(String path) {
        File directory = new File(path);
        if(!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
    }
}
