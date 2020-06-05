package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class DriveManager {
    final static private Logger LOG = LoggerFactory.getLogger(DriveManager.class);

    final private DirectoryRepository directoryRepository;
    final private FileRepository fileRepository;
    final private SourceRepository sourceRepository;
    final private ClassificationRepository classificationRepository;
    final private SynchronizeRepository synchronizeRepository;
    final private BackupManager backupManager;
    final private ApplicationProperties applicationProperties;
    final private ActionConfirmRepository actionConfirmRepository;
    final private IgnoreFileRepository ignoreFileRepository;

    @Autowired
    public DriveManager(DirectoryRepository directoryRepository,
                        FileRepository fileRepository,
                        SourceRepository sourceRepository,
                        ClassificationRepository classificationRepository,
                        SynchronizeRepository synchronizeRepository,
                        BackupManager backupManager,
                        ApplicationProperties applicationProperties,
                        ActionConfirmRepository actionConfirmRepository,
                        IgnoreFileRepository ignoreFileRepository) {
        this.directoryRepository = directoryRepository;
        this.fileRepository = fileRepository;
        this.sourceRepository = sourceRepository;
        this.classificationRepository = classificationRepository;
        this.synchronizeRepository = synchronizeRepository;
        this.backupManager = backupManager;
        this.applicationProperties = applicationProperties;
        this.actionConfirmRepository = actionConfirmRepository;
        this.ignoreFileRepository = ignoreFileRepository;
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

    private String getMD5(Path path, Classification classification) {
        if(classification == null || !classification.getUseMD5()) {
            return "";
        }

        try {
            // Calculate the MD5 for the file.
            MessageDigest md = MessageDigest.getInstance("MD5");

            DigestInputStream dis = new DigestInputStream(Files.newInputStream(path),md);
            //noinspection StatementWithEmptyBody
            while(dis.read() != -1);
            md = dis.getMessageDigest();

            return bytesToHex(md.digest());
        } catch (Exception ex) {
            LOG.error("Failed to get MD5, ",ex);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Cannot get MD5 - " + path.toString());
        }

        return "";
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

            if(filterFolder.length() > 0) {
                // Does it meet the filter?
                if(!filterFolder.matches(nextSource.getFilter())) {
                    LOG.trace("here");
                    return false;
                }
            }
        }

        return true;
    }

    private void processPath(Path path, Source nextSource, Iterable<Classification> classifications) {
        String directoryName;

        if(path.toFile().isDirectory()) {
            if(path.toAbsolutePath().toString().equals(nextSource.getPath())) {
                directoryName = "";
            } else {
                directoryName = path.toAbsolutePath().toString().replace(nextSource.getPath(), "");
            }
        } else {
            directoryName = path.toAbsolutePath().getParent().toString().replace(nextSource.getPath(),"");
        }

        if(!passFilter(directoryName,nextSource)) {
            return;
        }

        // Get the directory.
        Optional<DirectoryInfo> directory = directoryRepository.findBySourceAndPath(nextSource,directoryName);

        if(!directory.isPresent()) {
            DirectoryInfo newDirectoryInfo = new DirectoryInfo();
            newDirectoryInfo.setPath(directoryName);
            newDirectoryInfo.setSource(nextSource);

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
            // Get the file
            FileInfo newFile = new FileInfo();
            newFile.setName(path.getFileName().toString());
            newFile.setDirectoryInfo(directory.get());
            newFile.setClassification(classifyFile(newFile,classifications));
            newFile.setDate(fileDate);
            newFile.setSize(path.toFile().length());
            newFile.setMD5(getMD5(path,newFile.getClassification()));
            newFile.clearRemoved();

            fileRepository.save(newFile);
        } else {
            if(file.get().getClassification() == null) {
                Classification newClassification = classifyFile(file.get(),classifications);

                if(newClassification != null) {
                    file.get().setClassification(newClassification);
                }
            }

            if((file.get().getSize() != path.toFile().length()) ||
                    (file.get().getDate().compareTo(fileDate) != 0)) {
                file.get().setSize(path.toFile().length());
                file.get().setDate(fileDate);
                file.get().setMD5(getMD5(path,file.get().getClassification()));
            }

            file.get().clearRemoved();
            fileRepository.save(file.get());
        }

        LOG.info(path.toString());
    }

    @Scheduled(cron = "#{@applicationProperties.gatherSchedule}")
    public void gatherCron() {
        if(applicationProperties.getSynchronizeEnabled()) {
            try {
                gather();

                duplicateCheck();
            } catch (Exception ex) {
                LOG.error("Failed to gather",ex);
            }
        }
    }

    private void setSourceStatus(Source source, String status) {
        try {
            source.setStatus(status);
            sourceRepository.save(source);
        } catch(Exception ignored) {

        }
    }

    private void processDuplicate(FileInfo potentialDuplicate) {
        String name = "duplicate - " + potentialDuplicate.getDirectoryInfo().getSource().getPath() + potentialDuplicate.getDirectoryInfo().getPath() + "//" + potentialDuplicate.getName();

        if(checkAction(name,"DELETE_DUP")) {
            LOG.info("Delete duplicate file - " + potentialDuplicate.toString());

            // TODO - actually delete the file.
        }
    }

    private void checkDuplicateOfFile(List<FileInfo> files, Source source) {
        // Get list of files from the original source.
        List<FileInfo> checkFiles = new ArrayList<>();

        for(FileInfo nextFile: files) {
            if(nextFile.getDirectoryInfo().getSource().getId() == source.getId()) {
                checkFiles.add(nextFile);
            }
        }

        if(checkFiles.size() <= 1) {
            return;
        }

        // If files have the same size and MD5 then they are potential duplicates.
        for(FileInfo nextFile: checkFiles) {
            for(FileInfo nextFile2: checkFiles) {
                if(nextFile.duplicate(nextFile2)) {
                    LOG.info("Duplicate - " + nextFile.toString());

                    processDuplicate(nextFile);
                    processDuplicate(nextFile2);
                }
            }
        }
    }

    public void duplicateCheck() {
        actionConfirmRepository.clearDuplicateDelete();

        Iterable<Source> sources = sourceRepository.findAll();

        // Check for duplicates in sources.
        for(Source nextSource: sources) {
            if(nextSource.getLocation().getCheckDuplicates()) {
                // Find files that have the same name and size.
                List<String> files = fileRepository.findDuplicates(nextSource.getId());

                // Are these files really duplicates.
                for(String nextFile: files) {
                    List<FileInfo> duplicates = fileRepository.findByName(nextFile);
                    LOG.info("Check: " + nextFile);
                    checkDuplicateOfFile(duplicates, nextSource);
                }
            }
        }
    }

    public void gather() throws IOException {
        Iterable<Source> sources = sourceRepository.findAll();

        Iterable<Classification> classifications = classificationRepository.findAll();

        fileRepository.markAllRemoved();
        directoryRepository.markAllRemoved();

        for(Source nextSource: sources) {
            if(nextSource.getStatus() != null && nextSource.getStatus().equals("GATHERING")) {
                continue;
            }

            if(!nextSource.getAutoGather() ) {
                continue;
            }

            setSourceStatus(nextSource,"GATHERING");

            backupManager.postWebLog(BackupManager.webLogLevel.INFO, "Gather - " + nextSource.getPath());

            // If the source does not exist, create it.
            createDirectory(nextSource.getPath());

            // Read directory structure into the database.
            try (Stream<Path> paths = Files.walk(Paths.get(nextSource.getPath()))) {
                paths
                   .forEach(path -> processPath(path,nextSource,classifications));
            } catch (IOException e) {
                setSourceStatus(nextSource,"ERROR");
                backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to gather + " + e.toString());
                throw e;
            }

            setSourceStatus(nextSource,"OK");
        }

        fileRepository.deleteRemoved();
        directoryRepository.deleteRemoved();
    }

    @Scheduled(cron = "#{@applicationProperties.synchronizeSchedule}")
    public void synchronizeCron()  {
        if(applicationProperties.getSynchronizeEnabled()) {
            try {
                synchronize();
            } catch (Exception ex) {
                LOG.error("Failed to syncrhonize",ex);
            }
        }
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

        return source.getSize() != destination.getSize();
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
        if(source.getSize() != destination.getSize()) {
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

        LOG.info("Updating date " + source.getDate().getTime() + " - " + destination.getDate().getTime());
        // Make the date of the destination, equal to the source.
        File destinationFile = new File(destination.getDirectoryInfo().getSource().getPath() + "/" +
                destination.getDirectoryInfo().getPath() + "/" +
                destination.getName() );
        //noinspection ResultOfMethodCallIgnored
        destinationFile.setLastModified(source.getDate().getTime());
    }

    private void createDirectory(String path) {
        File directory = new File(path);
        if(!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
    }

    private void backup(SynchronizeStatus status) {
        try {
            LOG.info("Process backup - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());

            // Update the last modified time if necessary.
            equalizeDate(status.sourceFile, status.destinationFile);

            // Does the file need to be copied?
            if (copyFile(status.sourceFile, status.destinationFile)) {
                LOG.info("Copy File - " + status.sourceFile.toString());

                String sourceFilename = status.source.getPath() + "/" + status.sourceFile.getDirectoryInfo().getPath() + "/" + status.sourceFile.getName();
                String destinationFilename = status.destination.getPath() + "/" + status.sourceFile.getDirectoryInfo().getPath() + "/" + status.sourceFile.getName();

                createDirectory(status.destination.getPath() + "/" + status.sourceFile.getDirectoryInfo().getPath());

                LOG.info("Copy file from " + sourceFilename + " to " + destinationFilename);

                Files.copy(Paths.get(sourceFilename),
                        Paths.get(destinationFilename),
                        REPLACE_EXISTING);

                // Set the last modified date on the copied file to be the same as the source.
                File destinationFile = new File(destinationFilename);
                //noinspection ResultOfMethodCallIgnored
                destinationFile.setLastModified(status.sourceFile.getDate().getTime());
            }
        } catch(Exception ex) {
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to backup " + status.toString());
        }
    }

    private void warn(SynchronizeStatus status) {
        LOG.warn("File warning- " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
        backupManager.postWebLog(BackupManager.webLogLevel.WARN,"File warning - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
    }

    private void removeIfBackedup(SynchronizeStatus status) {
        // Does this exist on the remote server?
        if(status.destinationFile != null) {
            status.sourceFile = status.destinationFile;
            delete(status,false, true);
        }
    }

    private boolean checkAction(String name, String action) {
        List<ActionConfirm> confirmedActions = actionConfirmRepository.findByPathAndAction(name,action);

        if(confirmedActions.size() > 0) {
            boolean confirmed = false;
            for(ActionConfirm nextConfirm: confirmedActions) {
                if(nextConfirm.confirmed()) {
                    confirmed = true;
                }
            }

            if(confirmed) {
                for(ActionConfirm nextConfirm: confirmedActions) {
                    actionConfirmRepository.delete(nextConfirm);
                }

                return true;
            }
        } else {
            // Create an action to be confirmed.
            ActionConfirm actionConfirm = new ActionConfirm();
            actionConfirm.setPath(name);
            actionConfirm.setAction(action);
            actionConfirm.setConfirmed(false);
            actionConfirm.setParameterRequired(false);

            actionConfirmRepository.save(actionConfirm);
        }

        return false;
    }

    private void deleteFileIfConfirmed(String name) {
        File file = new File(name);

        if(file.exists()) {
            // Has this action been confirmed?
            if(checkAction(name, "DELETE")) {
                LOG.info("Delete the file - " + file );

                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    private void deleteDestinationFolder(SynchronizeStatus status) {
        LOG.info("Delete the destination folder - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
        String folderName = status.sourceFile.getDirectoryInfo().getSource().getPath() + "/" + status.sourceFile.getDirectoryInfo().getPath();

        deleteFileIfConfirmed(folderName);
    }

    private void delete(SynchronizeStatus status, boolean standard, boolean classified) {
        LOG.info("File should be deleted - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());

        if(!standard) {
            LOG.info("File should be deleted (should not have been copied) - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
            backupManager.postWebLog(BackupManager.webLogLevel.INFO,"File should be deleted (should not be there) - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
        }
        if(!classified) {
            LOG.info("File should be deleted (should not have been copied - unclassified) - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
            backupManager.postWebLog(BackupManager.webLogLevel.INFO,"File should be deleted (should not be there - unclassified) - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());
        }
        backupManager.postWebLog(BackupManager.webLogLevel.INFO,"File should be deleted - " + status.sourceDirectory.getPath() + "/" + status.sourceFile.getName());

        String filename = status.sourceFile.getDirectoryInfo().getSource().getPath() + "/" + status.sourceFile.getDirectoryInfo().getPath() + "/" + status.sourceFile.getName();
        deleteFileIfConfirmed(filename);
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
                // Perform the appropriate actions
                switch(nextStatus.classification.getAction()) {
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
                        LOG.warn("Unexpected action - " + nextStatus.classification.getAction() + " " + nextStatus.sourceDirectory.getPath() + " " + nextStatus.sourceFile.getName());
                        backupManager.postWebLog(BackupManager.webLogLevel.WARN,"Unexpected action - " + nextStatus.classification.getAction());
                }
            }

            for(SynchronizeStatus nextStatus: fileRepository.findSynchronizeExtraFiles(nextSynchronize.getId())) {
                // These are files that should not exists.
                if(nextStatus.classification == null) {
                    delete(nextStatus,false, false);
                    continue;
                }

                // Perform the appropriate actions
                switch(nextStatus.classification.getAction()) {
                    case "BACKUP":
                    case "IGNORE":
                    case "DELETE":
                    case "WARN":
                        delete(nextStatus,false, true);
                        break;

                    case "FOLDER":
                        deleteDestinationFolder(nextStatus);
                        break;

                    default:
                        LOG.warn("Unexpected action - " + nextStatus.classification.getAction() + " " + nextStatus.sourceDirectory.getPath() + " " + nextStatus.sourceFile.getName());
                        backupManager.postWebLog(BackupManager.webLogLevel.WARN,"Unexpected action - " + nextStatus.classification.getAction());
                }
            }

            LOG.info(nextSynchronize.getSource().getPath() + " -> " + nextSynchronize.getDestination().getPath());
        }
    }

    private boolean ignoreFile(FileInfo importFile) {
        // Is this a file to ignore?
        List<IgnoreFile> ignoreFiles = ignoreFileRepository.findByName(importFile.getName());

        for(IgnoreFile nextFile: ignoreFiles) {
            if(!nextFile.getSize().equals(importFile.getSize())) {
                continue;
            }

            if(!nextFile.getMD5().equals(importFile.getMD5())) {
                continue;
            }

            return true;
        }

        return false;
    }

    public enum FileTestResultType {EXACT, CLOSE, DIFFERENT}

    private FileTestResultType fileAlreadyExists(Path path, FileInfo fileInfo, FileInfo importFile) {
        if(!path.getFileName().toString().equals(fileInfo.getName())) {
            return FileTestResultType.DIFFERENT;
        }

        // Check the size.
        long size = path.toFile().length();
        if(fileInfo.getSize() != size) {
            return FileTestResultType.DIFFERENT;
        }

        // Check MD 5
        if((importFile.getMD5() != null) && importFile.getMD5().length() > 0) {
            if(fileInfo.getMD5() == null || fileInfo.getMD5().length() == 0) {
                // Source filename
                String sourceFilename = fileInfo.getDirectoryInfo().getSource().getPath() + fileInfo.getDirectoryInfo().getPath() + "/" + fileInfo.getName();

                // Need to get the MD5.
                fileInfo.setMD5(getMD5(new File(sourceFilename).toPath(),fileInfo.getClassification()));

                if(fileInfo.getMD5() == null || fileInfo.getMD5().length() == 0) {
                    return FileTestResultType.DIFFERENT;
                } else {
                    fileRepository.save(fileInfo);
                }
            }

            if(importFile.getMD5().equals(fileInfo.getMD5())) {
                return FileTestResultType.EXACT;
            }

            return FileTestResultType.CLOSE;
        }

        return FileTestResultType.EXACT;
    }

    private void processImport(Path path, Source source, Iterable<Classification> classifications, String reviewDirectory) {
        // Get details of the file to import.
        FileInfo importFileInfo = new FileInfo();
        importFileInfo.setName(path.getFileName().toString());
        importFileInfo.setSize(path.toFile().length());
        importFileInfo.setDate(new Date(path.toFile().lastModified()));
        importFileInfo.setClassification(classifyFile(importFileInfo,classifications));
        importFileInfo.setMD5(getMD5(path,importFileInfo.getClassification()));

        // Is this file being ignored?
        if(ignoreFile(importFileInfo)) {
            // Delete the file from import.
            LOG.info(path.toString() + " marked for ignore, deleting");
            //noinspection ResultOfMethodCallIgnored
            path.toFile().delete();
            return;
        }

        // Does this file already exist in the source?
        List<FileInfo> existingFiles = fileRepository.findByName(path.getFileName().toString());
        boolean closeMatch = false;

        for(FileInfo nextFile: existingFiles) {
            LOG.info(nextFile.toString());

            // Make sure this file is from the same source.
            if(nextFile.getDirectoryInfo().getSource().getId() != source.getId()) {
                continue;
            }

            // Get the details of the file - size & md5.
            FileTestResultType testResult = fileAlreadyExists(path,nextFile,importFileInfo);
            if(testResult == FileTestResultType.EXACT) {
                // Delete the file from import.
                LOG.info(path.toString() + " exists in source, deleting");
                //noinspection ResultOfMethodCallIgnored
                path.toFile().delete();
                return;
            }

            if(testResult == FileTestResultType.CLOSE) {
                closeMatch = true;
            }
        }

        // We can import this file but need to know where.
        // Photos are in <source> / <year> / <month> / <event> / filename

        // Do we have the event name?
        String actionString = path.toString();

        if(closeMatch) {
            actionString += "(CLOSE)";
        }

        List<ActionConfirm> confirmedActions = actionConfirmRepository.findByPathAndAction(actionString,"IMPORT");
        if(confirmedActions.size() > 0) {
            boolean confirmed = false;
            String parameter = "";
            for(ActionConfirm nextConfirm: confirmedActions) {
                if(nextConfirm.confirmed() && nextConfirm.getParameter() != null && nextConfirm.getParameter().length() > 0) {
                    parameter = nextConfirm.getParameter();
                    confirmed = true;
                }
            }

            if(confirmed && parameter.length() > 0) {
                for(ActionConfirm nextConfirm: confirmedActions) {
                    actionConfirmRepository.delete(nextConfirm);
                }

                // If the parameter value is IGNORE then add this file to the ignore list.
                if(parameter.equalsIgnoreCase("ignore")) {
                    IgnoreFile ignoreFile = new IgnoreFile();
                    ignoreFile.setDate(importFileInfo.getDate());
                    ignoreFile.setName(importFileInfo.getName());
                    ignoreFile.setSize(importFileInfo.getSize());
                    ignoreFile.setMD5(importFileInfo.getMD5());

                    ignoreFileRepository.save(ignoreFile);

                    return;
                }

                // The file can be copied.
                String newFilename = source.getPath();

                // Use the date of the file.
                Date fileDate = new Date(path.toFile().lastModified());

                SimpleDateFormat sdf1 = new SimpleDateFormat("YYYY");
                SimpleDateFormat sdf2 = new SimpleDateFormat("MMMM");

                newFilename += "/" + sdf1.format(fileDate);
                newFilename += "/" + sdf2.format(fileDate);
                newFilename += "/" + parameter;

                createDirectory(newFilename);

                newFilename += "/" + path.getFileName();

                try {
                    LOG.info("Importing file " + path.toString() + " to " + newFilename);
                    Files.move(path,
                            Paths.get(newFilename),
                            REPLACE_EXISTING);
                } catch (IOException e) {
                    LOG.error("Unable to import " + path);
                }
            }
        } else {
            // Create an action to be confirmed.
            ActionConfirm actionConfirm = new ActionConfirm();
            actionConfirm.setPath(actionString);
            actionConfirm.setAction("IMPORT");
            actionConfirm.setConfirmed(false);
            actionConfirm.setParameterRequired(true);

            actionConfirmRepository.save(actionConfirm);

            // Create a link for review.
            try {
                File linkFile = new File(reviewDirectory + "/" + (closeMatch ? "close." : "") + path.getFileName());

                if (Files.exists(linkFile.toPath())) {
                    Files.delete(linkFile.toPath());
                }

                Files.createSymbolicLink(linkFile.toPath(), path);
            } catch(Exception ex) {
                LOG.error("Failed to create link - " + path.toString());
            }
        }
    }

    public void importPhoto(ImportRequest importRequest) throws IOException {
        actionConfirmRepository.clearImports();

        Iterable<Classification> classifications = classificationRepository.findAll();

        // Check the path exists
        File importPath = new File(importRequest.path);
        if(!importPath.exists()) {
            throw new IOException("The path does not exist - " + importPath);
        }

        // Validate the source.
        Optional<Source> source = sourceRepository.findById(importRequest.source);
        if(!source.isPresent()) {
            throw new  IOException("The source does not exist - " + importRequest.source);
        }

        // Perform the import, find all the files to import and take action.
        // Read directory structure into the database.
        try (Stream<Path> paths = Files.walk(Paths.get(importRequest.path))) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(path -> processImport(path,source.get(),classifications, importRequest.reviewDirectory));
        } catch (IOException e) {
            LOG.error("Failed to process import - ",e);
            throw e;
        }
    }
}
