package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.exception.FileProcessException;
import com.jbr.middletier.backup.filetree.*;
import com.jbr.middletier.backup.filetree.compare.RwDbTree;
import com.jbr.middletier.backup.filetree.compare.node.RwDbCompareNode;
import com.jbr.middletier.backup.filetree.compare.node.RwDbSectionNode;
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

    private void processDeletesIteratively(FileTreeNode node, List<ActionConfirm> deletes, List<ActionConfirm> performed, GatherDataDTO gatherData) {
        // Only process nodes of type RwDbCompareNode
        if(!(node instanceof RwDbCompareNode)) {
            return;
        }

        // Process the children.
        for(FileTreeNode next: node.getChildren()) {
            processDeletesIteratively(next,deletes,performed,gatherData);
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

    private void processFileRemoval(RwDbCompareNode node) {
        // Delete this file from the database.
        Optional<FileInfo> existingFile = fileRepository.findById(node.getDatabaseObjectId().getId());

        existingFile.ifPresent(fileRepository::delete);
    }

    private void processDirectoryRemoval(RwDbCompareNode node) {
        Optional<DirectoryInfo> existingDirectory = directoryRepository.findById(node.getDatabaseObjectId().getId());

        existingDirectory.ifPresent(directoryRepository::delete);
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

    private void processDirectoryAddUpdate(RwDbCompareNode node) throws FileProcessException {
        // If there is a database object then read it first.
        Optional<DirectoryInfo> existingDirectory = Optional.empty();
        if(node.getDatabaseObjectId() != null) {
            existingDirectory = directoryRepository.findById(node.getDatabaseObjectId().getId());
        }

        if(!existingDirectory.isPresent()) {
            existingDirectory = Optional.of(new DirectoryInfo());
        }

        // Get the real world object.
        RwNode rwNode = getRwNode(node);

        // Insert a new directory.
        existingDirectory.get().setName(rwNode.getName());
        existingDirectory.get().setParentId(getParentIt(node));
        existingDirectory.get().clearRemoved();

        directoryRepository.save(existingDirectory.get());

        // Store the id of this item.
        node.setDatabaseObjectId(existingDirectory.get());
    }

    private void processFileAddUpdate(RwDbCompareNode node, boolean skipMD5) throws FileProcessException {
        // If there is a database object then read it first.
        boolean newFile = false;
        Optional<FileInfo> existingFile = Optional.empty();
        if(node.getDatabaseObjectId() != null) {
            existingFile = fileRepository.findById(node.getDatabaseObjectId().getId());
        }

        if(!existingFile.isPresent()) {
            existingFile = Optional.of(new FileInfo());
            newFile = true;
        }

        // Get the real world object.
        RwFile rwNode = (RwFile)getRwNode(node);

        Date fileDate = new Date(rwNode.getFile().lastModified());

        if(existingFile.get().getClassification() == null) {
            Classification newClassification = associatedFileDataManager.classifyFile(existingFile.get());

            if(newClassification != null) {
                existingFile.get().setClassification(newClassification);
            }
        }

        long dbTime = existingFile.get().getDate().getTime() / 1000;
        long fileTime = fileDate.getTime() / 1000;

        if((existingFile.get().getSize() == null) || (existingFile.get().getSize().compareTo(rwNode.getFile().length()) != 0) || (Math.abs(dbTime - fileTime) > 1)) {
            existingFile.get().setSize(rwNode.getFile().length());
            existingFile.get().setDate(fileDate);
            if(!skipMD5) {
                existingFile.get().setMD5(getMD5(rwNode.getFile().toPath(), existingFile.get().getClassification()));
            }
        }

        // Insert a new file.
        existingFile.get().setName(rwNode.getName());
        existingFile.get().setParentId(getParentIt(node));
        existingFile.get().clearRemoved();

        fileRepository.save(existingFile.get());
        if(newFile) {
            newFileInserted(existingFile.get());
        }

        // Store the id of this item.
        node.setDatabaseObjectId(existingFile.get());
    }

    protected void updateDatabase(Source source, List<ActionConfirm> deletes, boolean skipMD5, GatherDataDTO gatherData) throws IOException, FileProcessException {
        // Read the files structure from the real world.
        RwRoot realWorld = new RwRoot(source.getPath(), backupManager);
        realWorld.removeFilteredChildren(source.getFilter());

        // Read the same from the database.
        DbRoot database = new DbRoot(source, fileRepository, directoryRepository);

        // Compare the real world with the database.
        RwDbTree compare = new RwDbTree(realWorld, database);
        compare.compare();

        // Perform deletes
        processDeletes(compare,deletes, gatherData);

        // Process the actions.
        RwDbSectionNode.RwDbSectionNodeType section = RwDbSectionNode.RwDbSectionNodeType.UNKNOWN;
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
            } else if (nextNode instanceof RwDbSectionNode) {
                RwDbSectionNode sectionNode = (RwDbSectionNode)nextNode;
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
