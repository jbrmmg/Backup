package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.dto.ImportDataDTO;
import com.jbr.middletier.backup.dto.ImportSourceDTO;
import com.jbr.middletier.backup.exception.ImportRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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
                         ActionManager actionManager,
                         FileSystem fileSystem) {
        super(backupManager,actionManager,associatedFileDataManager,fileSystemObjectManager,fileSystem);
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
        updateDatabase(importSource, new ArrayList<>(), true, gatherData);

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
            if( !nextFile.getSize().equals(importFile.getSize()) || !nextFile.getMD5().compare(importFile.getMD5(), false) ) {
                continue;
            }

            return true;
        }

        return false;
    }

    private boolean processClassification(ImportFile importFile, Path path, ImportDataDTO result) {
        if(importFile.getClassification() == null) {
            return false;
        }

        if(!importFile.getClassification().getAction().equals(ClassificationActionType.CA_BACKUP)) {
            result.increment(ImportDataDTO.ImportDataCountType.NON_BACKUP_CLASSIFICATIONS);
            LOG.info("{} not a backed up file, deleting", path);
            fileSystem.deleteFile(path.toFile(),result);
            return false;
        }

        return true;
    }

    private boolean processIgnored(ImportFile importFile, Path path, ImportDataDTO result) {
        if(ignoreFile(importFile)) {
            result.increment(ImportDataDTO.ImportDataCountType.IGNORED_IMPORTS);
            // Delete the file from import.
            LOG.info("{} marked for ignore, deleting", path);
            fileSystem.deleteFile(path.toFile(), result);
            return false;
        }

        return true;
    }

    private FileTestResultType processExisting(ImportFile importFile, Path path, List<FileInfo> files, ImportDataDTO result) {
        List<FileInfo> existingFiles = files.stream()
                .filter(file -> file.getName().equals(importFile.getName()))
                .collect(Collectors.toList());

        for(FileInfo nextFile: existingFiles) {
            LOG.info("{}", nextFile);

            // Get the details of the file - size & md5.
            FileTestResultType testResult = fileAlreadyExists(path,nextFile,importFile);
            if(testResult == FileTestResultType.EXACT) {
                result.increment(ImportDataDTO.ImportDataCountType.ALREADY_IMPORTED);

                // Delete the file from import.
                LOG.info("{} exists in source, deleting",path);
                fileSystem.deleteFile(path.toFile(),result);
                return testResult;
            }
        }

        return FileTestResultType.DIFFERENT;
    }

    private void processConfirmedAction(ImportFile importFile, Path path, List<ActionConfirm> confirmedActions, Source source, String parameter, ImportDataDTO result) throws IOException {
        actionManager.deleteActions(confirmedActions);

        // If the parameter value is IGNORE then add this file to the ignored list.
        if(parameter.equalsIgnoreCase("ignore")) {
            IgnoreFile ignoreFile = new IgnoreFile();
            ignoreFile.setDate(importFile.getDate());
            ignoreFile.setName(importFile.getName());
            ignoreFile.setSize(importFile.getSize());
            ignoreFile.setMD5(importFile.getMD5());

            result.increment(ImportDataDTO.ImportDataCountType.IGNORED);
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

        fileSystem.createDirectory(new File(newFilename).toPath());

        newFilename += "/" + path.getFileName();

        result.increment(ImportDataDTO.ImportDataCountType.IMPORTED);
        fileSystem.moveFile(path.toFile(), new File(newFilename), result);
    }

    private void processImportActions(ImportFile importFile, Path path, List<ActionConfirm> confirmedActions, Source source, ImportDataDTO result) throws IOException {
        boolean confirmed = false;
        String parameter = "";
        for(ActionConfirm nextConfirm: confirmedActions) {
            if(nextConfirm.confirmed() && nextConfirm.getParameter() != null && nextConfirm.getParameter().length() > 0) {
                parameter = nextConfirm.getParameter();
                confirmed = true;
            }
        }

        if(confirmed && parameter.length() > 0) {
            processConfirmedAction(importFile,path,confirmedActions,source,parameter,result);
        }
    }

    private void processImport(ImportFile importFile, Source source, List<FileInfo> files, ImportDataDTO result) throws IOException {
        // If this file is completed then exit.
        if(importFile.getStatus().equals(ImportFileStatusType.IFS_COMPLETE)) {
            return;
        }

        // Get the path to the import file.
        Path path = fileSystemObjectManager.getFile(importFile).toPath();

        // What is the classification? if yes, unless this is a backup file just remove it.
        if(!processClassification(importFile,path,result)) {
            return;
        }

        // Get details of the file to import.
        if(!importFile.getMD5().isSet()) {
            importFile.setMD5(fileSystem.getClassifiedFileMD5(path, importFile.getClassification()));

            fileSystemObjectManager.save(importFile);
        }

        // Is this file being ignored?
        if(!processIgnored(importFile,path,result)) {
            return;
        }

        // Does this file already exist in the source?
        FileTestResultType existingState = processExisting(importFile,path,files,result);
        if(FileTestResultType.EXACT == existingState) {
            return;
        }

        // We can import this file but need to know where.
        // Photos are in <source> / <year> / <month> / <event> / filename
        List<ActionConfirm> confirmedActions = actionManager.getConfirmedImportActionsForFile(importFile);
        if(!confirmedActions.isEmpty()) {
            processImportActions(importFile,path,confirmedActions,source,result);
        } else {
            // Create an action to be confirmed.
            actionManager.createFileImportAction(importFile,FileTestResultType.CLOSE == existingState ? "C" : null);
        }
    }

    public List<ImportDataDTO> importPhotoProcess() throws ImportRequestException {
        LOG.info("Import Photo Process");
        List<ImportDataDTO> result = new ArrayList<>();

        // Get the source.
        Optional<ImportSourceDTO> importSource = Optional.empty();
        for(ImportSourceDTO nextSource: associatedFileDataManager.externalFindAllImportSource()) {
            importSource = Optional.of(nextSource);
        }

        if(!importSource.isPresent()) {
            throw new ImportRequestException("There is no import source defined.");
        }
        ImportDataDTO resultItem = new ImportDataDTO(importSource.get().getId());
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
                LOG.info( "{} MD5: {}", nextFile.getName(), nextFile.getMD5());

                processImport(nextFile, destination.get(), files, resultItem);

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

        return result;
    }

    enum FileTestResultType {EXACT, CLOSE, DIFFERENT}

    private boolean md5StillMissing(Path path, FileInfo fileInfo, Classification classification) {
        fileInfo.setMD5(fileSystem.getClassifiedFileMD5(path,classification));

        if(fileInfo.getMD5().isSet()) {
            fileSystemObjectManager.save(fileInfo);
            return false;
        }

        return true;
    }

    private FileTestResultType fileAlreadyExists(Path path, FileInfo fileInfo, FileInfo importFile) {
        // Check the size.
        long size = path.toFile().length();
        if(fileInfo.getSize() != size) {
            return FileTestResultType.DIFFERENT;
        }

        // If the classification requires an MD5, and it's missing from one size or the other then
        // calculate it now.
        if(fileInfo.getClassification() != null && fileInfo.getClassification().getUseMD5()) {
            // Check if the import file has an MD5
            if(!importFile.getMD5().isSet() && md5StillMissing(path,importFile,fileInfo.getClassification())) {
                return FileTestResultType.CLOSE;
            }

            // Check the file.
            File sourceFile = fileSystemObjectManager.getFile(fileInfo);
            if(!fileInfo.getMD5().isSet() && md5StillMissing(sourceFile.toPath(), fileInfo, fileInfo.getClassification())) {
                return FileTestResultType.CLOSE;
            }

            if(!importFile.getMD5().compare(fileInfo.getMD5(),false)) {
                return FileTestResultType.CLOSE;
            }
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
