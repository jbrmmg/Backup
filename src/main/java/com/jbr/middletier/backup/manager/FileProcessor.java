package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;
import org.apache.commons.io.FileUtils;
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

            return (filterFolder.length() <= 0) || (filterFolder.matches(nextSource.getFilter()));
        }

        return true;
    }

    private Classification classifyFile(FileInfo file, Iterable<Classification> classifications)  {
        for(Classification nextClassification : classifications) {
            if(nextClassification.fileMatches(file)) {
                return nextClassification;
            }
        }

        return null;
    }

    private boolean deleteFileIfRequired(FileInfo file, List<ActionConfirm> deletes) {
        for(ActionConfirm nextAction: deletes) {
            if(nextAction.getPath().getId().equals(file.getId())) {
                LOG.info("Deleteing the file {}", file.getFullFilename());
                actionManager.deleteFileIfConfirmed(file);
                return true;
            }
        }

        return false;
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

    private String determineDirectoryName(Path path, Source nextSource) {
        if(path.toFile().isDirectory()) {
            if(path.toAbsolutePath().toString().equals(nextSource.getPath())) {
                // This is the source
                return "";
            }

            // Return the path
            return path.toAbsolutePath().toString().replace(nextSource.getPath(), "");
        }

        // Directory name from file.
        return path.toAbsolutePath().getParent().toString().replace(nextSource.getPath(),"");
    }

    abstract void newFileInserted(FileInfo newFile);

    private void createFile(Path path, DirectoryInfo directory, Iterable<Classification> classifications, boolean skipMD5) {
        Date fileDate = new Date(path.toFile().lastModified());

        // Get the file
        FileInfo newFile = new FileInfo();
        newFile.setName(path.getFileName().toString());
        newFile.setDirectoryInfo(directory);
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
        if(toBeRemoved.getSourceDbDirectory() != null) {
            directoryRepository.delete(toBeRemoved.getSourceDbDirectory());
        } else if(toBeRemoved.getSourceDbFile() != null) {
            fileRepository.delete(toBeRemoved.getSourceDbFile());
        }
    }

    private DirectoryInfo getParentDirectory(FileTreeNode node) {
        if(node.getParent() == null) {
            return null;
        }

        if(node.getParent().getCreatedDirectory() == null) {
            return node.getParent().getSourceDbDirectory();
        }

        return node.getParent().getCreatedDirectory();
    }

    private void performDbAddOrUpdDirectory(Source source, FileTreeNode toBeUpdated) {
        if(toBeUpdated.getSourceDbDirectory() != null && toBeUpdated.getCompareStatus() == FileTreeNode.CompareStatusType.UPDATED) {
            directoryRepository.save(toBeUpdated.getSourceDbDirectory());
            return;
        }

        // Insert a new directory.
        DirectoryInfo newDirectoryInfo = new DirectoryInfo();
        newDirectoryInfo.setName(toBeUpdated.getName());
        newDirectoryInfo.setSource(source);
        newDirectoryInfo.setParent(getParentDirectory(toBeUpdated));
        newDirectoryInfo.clearRemoved();

        directoryRepository.save(newDirectoryInfo);

        toBeUpdated.setCreatedDirectory(newDirectoryInfo);
    }

    private void performDatabaseAddOrUpdate(Source source, FileTreeNode toBeUpdated, Iterable<Classification> classifications, boolean skipMD5) {
        if(toBeUpdated.isDirectory()) {
            performDbAddOrUpdDirectory(source, toBeUpdated);
            return;
        }

        // Is this a create or update?
        if(toBeUpdated.getSourceDbFile() != null) {
            createFile(toBeUpdated.getSourcePath(),getParentDirectory(toBeUpdated),classifications,skipMD5);
        } else {
            updateFile(toBeUpdated.getSourcePath(),toBeUpdated.getSourceDbFile(),classifications,skipMD5);
        }
    }

    private void performDatabaseUpdate(Source source, FileTreeNode compare, Iterable<Classification> classifications, boolean skipMD5) {
        // If they are equal then nothing more to do on this node.
        if(compare.getCompareStatus() == FileTreeNode.CompareStatusType.EQUAL) {
            return;
        }

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

    private void updateDatabase(Source source, FileTreeNode compare, Iterable<Classification> classifications, boolean skipMD5) {
        performDatabaseUpdate(source, compare, classifications, skipMD5);

        // Process the children
        for(FileTreeNode next: compare.getChildren()) {
            updateDatabase(source, next, classifications, skipMD5);
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
        FileInfo sourceFile = node.getSourceDbFile();
        Path sourcePath = node.getSourcePath();
        if(sourceFile == null || sourcePath == null) {
            LOG.warn("CANNOT process the delete as the node does not have a db entry equal to real world.");
            LOG.warn("> " + node);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"CANNOT process the delete as the node does not have a db entry equal to real world.");
            return;
        }

        // Is this file marked for delete?
        for(ActionConfirm next : deletes) {
            if(next.confirmed() && next.getPath().getId().equals(sourceFile.getId())) {
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
        RootFileTreeNode realworld = getFileDetails(source);

        // Read the same from the database.
        RootFileTreeNode database = getDatabaseDetails(source);

        // Compare the real world with the database.
        RootFileTreeNode compare = realworld.compare(database);

        // Process the deletes
        processDeletes(compare, deletes);

        // Update the database with the real world.
        updateDatabase(source,compare,classifications,skipMD5);
    }

    /*
    void processPathx(Path path, List<ActionConfirm> deletes, Source nextSource, Iterable<Classification> classifications, boolean skipMD5) {
        String directoryName = determineDirectoryName(path,nextSource);

        if(!passFilter(directoryName,nextSource)) {
            return;
        }

        // Get the directory.
        Optional<DirectoryInfo> directory = directoryRepository.findBySourceAndPath(nextSource,directoryName);

        if(!directory.isPresent()) {
            DirectoryInfo newDirectoryInfo = new DirectoryInfo();
            newDirectoryInfo.setPath(directoryName);
            newDirectoryInfo.setSource(nextSource);
            newDirectoryInfo.clearRemoved();

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
            createFile(path,directory.get(),classifications,fileDate,skipMD5);
        } else {
            updateFile(path,file.get(),fileDate,deletes,classifications,skipMD5);
        }

        LOG.info("{}", path);
    }
     */

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
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to read + " + rootDirectory.toString());
            throw e;
        }

        return result;
    }

    private void getDatabaseDetails(FileTreeNode result, Source source, DirectoryInfo parent) {
        List<DirectoryInfo> directories = directoryRepository.findByDirectoryAndParent(source, parent);
        for(DirectoryInfo next: directories) {
            FileTreeNode nextNode = result.addChild(next);

            List<FileInfo> files = fileRepository.findByDirectoryInfo(next);

            for(FileInfo nextFile: files) {
                nextNode.addChild(nextFile);
            }

            // Process the next level.
            getDatabaseDetails(nextNode, source, next);
        }
    }

    private RootFileTreeNode getDatabaseDetails(Source source) {
        RootFileTreeNode result = new RootFileTreeNode(source);

        getDatabaseDetails(result, source, null);

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
