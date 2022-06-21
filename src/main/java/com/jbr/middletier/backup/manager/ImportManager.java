package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.dto.ImportSourceDTO;
import com.jbr.middletier.backup.exception.FileProcessException;
import com.jbr.middletier.backup.exception.ImportRequestException;
import com.jbr.middletier.backup.exception.MissingFileSystemObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class ImportManager extends FileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ImportManager.class);

    private final ImportFileRepository importFileRepository;
    private final IgnoreFileRepository ignoreFileRepository;

    @Autowired
    public ImportManager(ImportFileRepository importFileRepository,
                         AssociatedFileDataManager associatedFileDataManager,
                         FileSystemObjectManager fileSystemObjectManager,
                         IgnoreFileRepository ignoreFileRepository,
                         BackupManager backupManager,
                         ActionManager actionManager) {
        super(backupManager,actionManager,associatedFileDataManager,fileSystemObjectManager);
        this.importFileRepository = importFileRepository;
        this.ignoreFileRepository = ignoreFileRepository;
    }

    public List<GatherDataDTO> importPhoto(ImportRequest importRequest) throws ImportRequestException, IOException {
        List<GatherDataDTO> result = new ArrayList<>();

        // Remove any existing import data.
        clearImports();

        // Check the path exists
        File importPath = new File(importRequest.getPath());
        if (!importPath.exists()) {
            throw new ImportRequestException("The path does not exist - " + importPath);
        }

        // Validate the source.
        Optional<Source> source = associatedFileDataManager.internalFindSourceByIdIfExists(importRequest.getSource());
        if (!source.isPresent()) {
            throw new ImportRequestException("The source does not exist - " + importRequest.getSource());
        }

        // Find the location.
        Optional<Location> importLocation = associatedFileDataManager.internalFindImportLocationIfExists();
        if (!importLocation.isPresent()) {
            throw new ImportRequestException("Cannot find import location.");
        }

        // Create a source to match this import
        ImportSource importSource = associatedFileDataManager.createImportSource(importRequest.getPath(), source.get(), importLocation.get());

        // Perform the import, find all the files to import and take action.
        // Read directory structure into the database.
        GatherDataDTO gatherData = new GatherDataDTO(importSource.getIdAndType().getId());
        try {
            updateDatabase(importSource, new ArrayList<>(), true, gatherData);
        } catch (FileProcessException | MissingFileSystemObject e) {
            gatherData.setProblems();
        }
        result.add(gatherData);

        return result;
    }

    public void clearImports() {
        actionManager.clearImportActions();

        // Remove the files associated with imports - first remove files, then directories then source.
        for(ImportSourceDTO nextSource: associatedFileDataManager.externalFindAllImportSource()) {
            List<DirectoryInfo> directories = new ArrayList<>();
            List<FileInfo> files = new ArrayList<>();

            fileSystemObjectManager.loadByParent(nextSource.getId(),directories,files);

            for(FileInfo nextFile: files) {
                fileSystemObjectManager.delete(nextFile);
            }

            Collections.reverse(directories);
            for(DirectoryInfo nextDirectory: directories) {
                fileSystemObjectManager.delete(nextDirectory);
            }

            associatedFileDataManager.deleteImportSource(nextSource);
        }
    }

    private boolean ignoreFile(FileInfo importFile) {
        // Is this a file to ignore?
        List<IgnoreFile> ignoreFiles = ignoreFileRepository.findByName(importFile.getName());

        for(IgnoreFile nextFile: ignoreFiles) {
            if( !nextFile.getSize().equals(importFile.getSize()) || !nextFile.getMD5().equals(importFile.getMD5()) ) {
                continue;
            }

            return true;
        }

        return false;
    }

    private boolean processClassification(ImportFile importFile, Path path) {
        if(importFile.getClassification() == null) {
            return false;
        }

        if(!importFile.getClassification().getAction().equals(ClassificationActionType.CA_BACKUP)) {
            LOG.info("{} not a backed up file, deleting", path);
            try {
                Files.delete(path);
            } catch (IOException e) {
                LOG.warn("Failed to delete {}, not a backup classification.", path);
            }
            return false;
        }

        return true;
    }

    private boolean processIgnored(ImportFile importFile, Path path) {
        if(ignoreFile(importFile)) {
            // Delete the file from import.
            LOG.info("{} marked for ignore, deleting", path);
            try {
                Files.delete(path);
            } catch (IOException e) {
                LOG.warn("Failed to delete {}, ignored file.", path);
            }
            return false;
        }

        return true;
    }

    private FileTestResultType processExisting(ImportFile importFile, Path path, List<FileInfo> files) throws MissingFileSystemObject {
        List<FileInfo> existingFiles = files.stream()
                .filter(file -> file.getName().equals(importFile.getName()))
                .collect(Collectors.toList());

        for(FileInfo nextFile: existingFiles) {
            LOG.info("{}", nextFile);

            // Get the details of the file - size & md5.
            FileTestResultType testResult = fileAlreadyExists(path,nextFile,importFile);
            if(testResult == FileTestResultType.EXACT) {
                // Delete the file from import.
                LOG.info("{} exists in source, deleting",path);
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    LOG.warn("Failed to delete {}, already imported", path);
                }

                return testResult;
            }
        }

        return FileTestResultType.DIFFERENT;
    }

    private void processConfirmedAction(ImportFile importFile, Path path, List<ActionConfirm> confirmedActions, Source source, String parameter) {
        actionManager.deleteActions(confirmedActions);

        // If the parameter value is IGNORE then add this file to the ignore list.
        if(parameter.equalsIgnoreCase("ignore")) {
            IgnoreFile ignoreFile = new IgnoreFile();
            ignoreFile.setDate(importFile.getDate());
            ignoreFile.setName(importFile.getName());
            ignoreFile.setSize(importFile.getSize());
            ignoreFile.setMD5(importFile.getMD5());

            ignoreFileRepository.save(ignoreFile);

            return;
        }

        // The file can be copied.
        String newFilename = source.getPath();

        // Use the date of the file.
        Date fileDate = new Date(path.toFile().lastModified());

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy");
        SimpleDateFormat sdf2 = new SimpleDateFormat("MMMM");

        newFilename += "/" + sdf1.format(fileDate);
        newFilename += "/" + sdf2.format(fileDate);
        newFilename += "/" + parameter;

        createDirectory(newFilename);

        newFilename += "/" + path.getFileName();

        try {
            LOG.info("Importing file {} to {}", path, newFilename);
            Files.move(path,
                    Paths.get(newFilename),
                    REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.error("Unable to import {}", path);
        }
    }

    private void processImportActions(ImportFile importFile, Path path, List<ActionConfirm> confirmedActions, Source source) {
        boolean confirmed = false;
        String parameter = "";
        for(ActionConfirm nextConfirm: confirmedActions) {
            if(nextConfirm.confirmed() && nextConfirm.getParameter() != null && nextConfirm.getParameter().length() > 0) {
                parameter = nextConfirm.getParameter();
                confirmed = true;
            }
        }

        if(confirmed && parameter.length() > 0) {
            processConfirmedAction(importFile,path,confirmedActions,source,parameter);
        }
    }

    private void processImport(ImportFile importFile, Source source, List<FileInfo> files) throws MissingFileSystemObject {
        // If this file is completed then exit.
        if(importFile.getStatus().equals(ImportFileStatusType.IFS_COMPLETE)) {
            return;
        }

        // Get the path to the import file.
        Path path = fileSystemObjectManager.getFile(importFile).toPath();

        // What is the classification? if yes, unless this is a backup file just remove it.
        if(!processClassification(importFile,path)) {
            return;
        }

        // Get details of the file to import.
        if(!importFile.getMD5().isSet()) {
            importFile.setMD5(getMD5(path, importFile.getClassification()));

            fileSystemObjectManager.save(importFile);
        }

        // Is this file being ignored?
        if(!processIgnored(importFile,path)) {
            return;
        }

        // Does this file already exist in the source?
        FileTestResultType existingState = processExisting(importFile,path,files);
        if(FileTestResultType.EXACT == existingState) {
            return;
        }

        // We can import this file but need to know where.
        // Photos are in <source> / <year> / <month> / <event> / filename
        List<ActionConfirm> confirmedActions = actionManager.getConfirmedImportActionsForFile(importFile);
        if(!confirmedActions.isEmpty()) {
            processImportActions(importFile,path,confirmedActions,source);
        } else {
            // Create an action to be confirmed.
            actionManager.createFileImportAction(importFile,FileTestResultType.CLOSE == existingState ? "C" : null);
        }
    }

    public List<GatherDataDTO> importPhotoProcess() throws ImportRequestException {
        LOG.info("Import Photo Process");
        List<GatherDataDTO> result = new ArrayList<>();

        // Get the source.
        Optional<ImportSourceDTO> importSource = Optional.empty();
        for(ImportSourceDTO nextSource: associatedFileDataManager.externalFindAllImportSource()) {
            importSource = Optional.of(nextSource);
        }

        if(!importSource.isPresent()) {
            throw new ImportRequestException("There is no import source defined.");
        }
        GatherDataDTO resultItem = new GatherDataDTO(importSource.get().getId());
        result.add(resultItem);

        try {
            // Get the place they are to be imported to.
            Optional<Source> destination = associatedFileDataManager.internalFindSourceByIdIfExists(importSource.get().getDestinationId());
            if (!destination.isPresent()) {
                throw new ImportRequestException("Destination for import is not found.");
            }

            // Get all the files that are in the destination source
            List<DirectoryInfo> directories = new ArrayList<>();
            List<FileInfo> files = new ArrayList<>();
            fileSystemObjectManager.loadByParent(destination.get().getIdAndType().getId(), directories, files);


            for (ImportFile nextFile : importFileRepository.findAll()) {
                LOG.info(nextFile.getName() + " MD5: " + nextFile.getMD5());

                processImport(nextFile, destination.get(), files);
                resultItem.increment(GatherDataDTO.GatherDataCountType.FILES_INSERTED);

                nextFile.setStatus(ImportFileStatusType.IFS_COMPLETE);
                importFileRepository.save(nextFile);
            }
        } catch (Exception e) {
            resultItem.setProblems();
        }

        return result;
    }

    public List<GatherDataDTO> removeEntries() {
        List<GatherDataDTO> result = new ArrayList<>();
        GatherDataDTO resultItem = new GatherDataDTO(-1);
        result.add(resultItem);

        // Remove entries from import table if they are no longer present.
        try {
            for (ImportFile nextFile : importFileRepository.findAll()) {
                // Does this file still exist?
                File existingFile = fileSystemObjectManager.getFile(nextFile);
                resultItem.increment(GatherDataDTO.GatherDataCountType.FILES_INSERTED);

                if (!existingFile.exists()) {
                    LOG.info("Remove this import file - {}", existingFile);
                    importFileRepository.delete(nextFile);
                    resultItem.increment(GatherDataDTO.GatherDataCountType.DELETES);
                } else {
                    LOG.info("Keeping {}", existingFile);
                }
            }
        } catch(MissingFileSystemObject e) {
            resultItem.setProblems();
        }

        return result;
    }


    enum FileTestResultType {EXACT, CLOSE, DIFFERENT}

    private FileTestResultType fileAlreadyExists(Path path, FileInfo fileInfo, FileInfo importFile) throws MissingFileSystemObject {
        if(!path.getFileName().toString().equals(fileInfo.getName())) {
            return FileTestResultType.DIFFERENT;
        }

        // Check the size.
        long size = path.toFile().length();
        if(fileInfo.getSize() != size) {
            return FileTestResultType.DIFFERENT;
        }

        // Check MD 5
        if((importFile.getMD5().isSet())) {
            if(!fileInfo.getMD5().isSet()) {
                // Source filename
                File sourceFile = fileSystemObjectManager.getFile(fileInfo);

                // Need to get the MD5.
                fileInfo.setMD5(getMD5(sourceFile.toPath(),fileInfo.getClassification()));

                if(!fileInfo.getMD5().isSet()) {
                    return FileTestResultType.DIFFERENT;
                } else {
                    fileSystemObjectManager.save(fileInfo);
                }
            }

            if(importFile.getMD5().equals(fileInfo.getMD5())) {
                return FileTestResultType.EXACT;
            }

            return FileTestResultType.CLOSE;
        }

        return FileTestResultType.EXACT;
    }

    public Iterable<ImportFile> findImportFiles() {
        return importFileRepository.findAllByOrderByIdAsc();
    }

    public Iterable<ImportFile> resetFiles() {
        Iterable<ImportFile> result = importFileRepository.findAll();

        for(ImportFile nextImport: result) {
            nextImport.setStatus(ImportFileStatusType.IFS_READ);
            importFileRepository.save(nextImport);
        }

        return findImportFiles();
    }

    @Override
    public FileInfo createNewFile() {
        ImportFile newFile = new ImportFile();
        newFile.setStatus(ImportFileStatusType.IFS_READ);
        return newFile;
    }
}
