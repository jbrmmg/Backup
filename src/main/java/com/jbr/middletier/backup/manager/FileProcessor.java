package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

abstract class FileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FileProcessor.class);

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

    private void createFile(Path path, FileSystemObject parent, Iterable<Classification> classifications, boolean skipMD5) {
        Date fileDate = new Date(path.toFile().lastModified());

        // Get the file
        FileInfo newFile = new FileInfo();
        newFile.setName(path.getFileName().toString());
        newFile.setParentId(parent);
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

    private void updateFile(Path path, FileInfo file, Iterable<Classification> classifications, boolean skipMD5) {
        Date fileDate = new Date(path.toFile().lastModified());

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

    private void performDatabaseAddOrUpdate(Source source, FileTreeNode toBeUpdated, Iterable<Classification> classifications, boolean skipMD5) {
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
            updateFile(toBeUpdated.getPath(),existingFile.get(),classifications,skipMD5);
        } else {
            createFile(toBeUpdated.getPath(),getParentDirectory(toBeUpdated, source),classifications,skipMD5);
        }
    }

    private void performDatabaseUpdate(Source source, FileTreeNode compare, Iterable<Classification> classifications, boolean skipMD5) {
        // If adding, then add now and then the children.
        if((compare.getCompareStatus() == FileTreeNode.CompareStatusType.ADDED) ||
                (compare.getCompareStatus() == FileTreeNode.CompareStatusType.UPDATED) ||
                (compare.getCompareStatus() == FileTreeNode.CompareStatusType.CHANGE_TO_FILE) ||
                (compare.getCompareStatus() == FileTreeNode.CompareStatusType.CHANGE_TO_DIRECTORY)) {
            // Insert or update
            performDatabaseAddOrUpdate(source, compare, classifications, skipMD5);
        }

        // Process the children.
        for(FileTreeNode next: compare.getChildren()) {
            // If the child compare status is unknown then copy from parent.
            if(next.getCompareStatus() == FileTreeNode.CompareStatusType.UNKNOWN) {
                next.setCompareStatus(compare.getCompareStatus());
            }

            performDatabaseUpdate(source, next, classifications, skipMD5);
        }

        // If removing, then remove after the children.
        if((compare.getCompareStatus() == FileTreeNode.CompareStatusType.REMOVED) ||
                (compare.getCompareStatus() == FileTreeNode.CompareStatusType.CHANGE_TO_FILE) ||
                (compare.getCompareStatus() == FileTreeNode.CompareStatusType.CHANGE_TO_DIRECTORY)) {
            performDatabaseRemove(compare);
        }
    }

    private void processDeletesIteratively(FileTreeNode node, List<ActionConfirm> deletes, List<ActionConfirm> performed) {
        // Only nodes that are the same as the DB can be deleted
        if(node.getCompareStatus() != FileTreeNode.CompareStatusType.EQUAL) {
            return;
        }

        for(FileTreeNode next: node.getChildren()) {
            processDeletesIteratively ( next, deletes, performed );
        }

        // Does this node have a source file?
        Path sourcePath = node.getPath();
        if(node.hasValidId() || sourcePath == null) {
            LOG.warn("CANNOT process the delete as the node does not have a db entry equal to real world.");
            LOG.warn("> " + node);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"CANNOT process the delete as the node does not have a db entry equal to real world.");
            return;
        }

        // Is this file marked for delete?
        for(ActionConfirm next : deletes) {
            if(next.confirmed() && next.getPath().getIdAndType().equals(node.getId())) {
                LOG.info("Deleting the file {}", sourcePath);

                try {
                    Files.deleteIfExists(sourcePath);
                    node.setCompareStatus(FileTreeNode.CompareStatusType.REMOVED);

                    LOG.info("Deleted.");

                    // Remove the action.
                    actionManager.actionPerformed(next);
                    performed.add(next);
                } catch (IOException e) {
                    LOG.warn("Failed to delete file {}", sourcePath);
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

        processDeletesIteratively ( details, deletes, performedActions );
        deletes.removeAll(performedActions);

        // If there are still confirmed deletes to perform then they are invalid, so delete them anyway.
        for(ActionConfirm next: deletes) {
            if(next.confirmed()) {
                LOG.warn("Action cannot be performed: " + next);
                actionManager.actionPerformed(next);
            }
        }
    }

    protected void updateDatabase(Source source, List<ActionConfirm> deletes, Iterable<Classification> classifications, boolean skipMD5) throws IOException {
        // Read the files structure from the real world.
        RootFileTreeNode realWorld = getFileDetails(source);
        realWorld.removeFilteredChildren(source);

        // Read the same from the database.
        RootFileTreeNode database = getDatabaseDetails(source);

        // Compare the real world with the database.
        RootFileTreeNode compare = realWorld.compare(database);

        // Process the deletes
        processDeletes(compare, deletes);

        // Update the database with the real world.
        for(FileTreeNode next: compare.getChildren()) {
            performDatabaseUpdate(source, next, classifications, skipMD5);
        }
    }

    private RootFileTreeNode getFileDetails(Source root) throws IOException {
        Path rootDirectory = Paths.get(root.getPath());
        RootFileTreeNode result = new RootFileTreeNode(rootDirectory);

        try(Stream<Path> fileDetails = Files.walk(rootDirectory)) {
            fileDetails.forEach(path -> {
                if(path.getNameCount() > rootDirectory.getNameCount()) {
                    FileTreeNode nextIterator = result;

                    for(int directoryIdx = rootDirectory.getNameCount(); directoryIdx < path.getNameCount() - 1; directoryIdx++) {
                        nextIterator = nextIterator.getNamedChild(path.getName(directoryIdx).toString());
                    }

                    nextIterator.addChild(path);
                }
            });
        } catch (IOException e) {
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to read + " + rootDirectory);
            throw e;
        }

        return result;
    }

    private void getDatabaseDetails(FileTreeNode result, FileSystemObject parent) {
        List<DirectoryInfo> directories = directoryRepository.findByParentId(parent.getIdAndType().getId());
        for(DirectoryInfo next: directories) {
            FileTreeNode nextNode = result.addChild(next);

            List<FileInfo> files = fileRepository.findByParentId(next.getIdAndType().getId());

            for(FileInfo nextFile: files) {
                nextNode.addChild(nextFile);
            }

            // Process the next level.
            getDatabaseDetails(nextNode, next);
        }
    }

    private RootFileTreeNode getDatabaseDetails(Source source) {
        RootFileTreeNode result = new RootFileTreeNode(source);

        getDatabaseDetails(result, source);

        return result;
    }

    protected void createDirectory(String path) {
        File directory = new File(path);
        if(!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
    }
}
