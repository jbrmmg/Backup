package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.exception.ImportRequestException;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class ImportManager extends FileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ImportManager.class);

    private final ImportFileRepository importFileRepository;
    private final SourceRepository sourceRepository;
    private final ActionConfirmRepository actionConfirmRepository;
    private final ClassificationRepository classificationRepository;
    private final LocationRepository locationRepository;
    private final IgnoreFileRepository ignoreFileRepository;

    @Autowired
    public ImportManager(ImportFileRepository importFileRepository,
                         SourceRepository sourceRepository,
                         ActionConfirmRepository actionConfirmRepository,
                         ClassificationRepository classificationRepository,
                         LocationRepository locationRepository,
                         DirectoryRepository directoryRepository,
                         FileRepository fileRepository,
                         IgnoreFileRepository ignoreFileRepository,
                         BackupManager backupManager,
                         ActionManager actionManager) {
        super(directoryRepository,fileRepository,backupManager,actionManager);
        this.importFileRepository = importFileRepository;
        this.sourceRepository = sourceRepository;
        this.actionConfirmRepository = actionConfirmRepository;
        this.classificationRepository = classificationRepository;
        this.locationRepository = locationRepository;
        this.ignoreFileRepository = ignoreFileRepository;
    }

    public void importPhoto(ImportRequest importRequest) throws ImportRequestException, IOException {
        // Get the classifications
        Iterable<Classification> classifications = classificationRepository.findAll();

        // Remove any existing import data.
        clearImports();

        // Check the path exists
        File importPath = new File(importRequest.getPath());
        if(!importPath.exists()) {
            throw new ImportRequestException("The path does not exist - " + importPath);
        }

        // Validate the source.
        Optional<Source> source = sourceRepository.findById(importRequest.getSource());
        if(!source.isPresent()) {
            throw new  ImportRequestException("The source does not exist - " + importRequest.getSource());
        }

        int nextId = 0;
        for(Source nextSource: sourceRepository.findAll()) {
            if(nextSource.getId() >= nextId) {
                nextId = nextSource.getId() + 1;
            }
        }

        // Find the location.
        Optional<Location> importLocation = Optional.empty();
        for(Location nextLocation: locationRepository.findAll()) {
            if(nextLocation.getName().equalsIgnoreCase("import")) {
                importLocation = Optional.of(nextLocation);
            }
        }

        if(!importLocation.isPresent()) {
            throw new IOException("Cannot find import location.");
        }

        // Create a source to match this import
        Source importSource = new Source(nextId,importRequest.getPath());
        importSource.setTypeEnum(Source.SourceTypeType.IMPORT);
        importSource.setDestinationId(source.get().getId());
        importSource.setLocation(importLocation.get());

        sourceRepository.save(importSource);

        // Perform the import, find all the files to import and take action.
        // Read directory structure into the database.
        updateDatabase(importSource, new ArrayList<>(), classifications, true);
    }

    public void clearImports() {
        actionConfirmRepository.clearImports(false);

        // Clear out the imported file data.
        importFileRepository.deleteAll();

        // Remove the files associated with imports - first remove files, then directories then source.
        for(Source nextSource: sourceRepository.findAll()) {
            if(nextSource.getTypeEnum() == Source.SourceTypeType.IMPORT) {
                for(DirectoryInfo nextDirectory: directoryRepository.findBySource(nextSource)) {
                    for(FileInfo nextFile: fileRepository.findByDirectoryInfoId(nextDirectory.getId())) {
                        fileRepository.delete(nextFile);
                    }

                    directoryRepository.delete(nextDirectory);
                }

                sourceRepository.delete(nextSource);
            }
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
        if(importFile.getFileInfo().getClassification() == null) {
            return false;
        }

        if(!importFile.getFileInfo().getClassification().getAction().equalsIgnoreCase("backup")) {
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
        if(ignoreFile(importFile.getFileInfo())) {
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

    private FileTestResultType processExisting(ImportFile importFile, Path path, Source source) {
        List<FileInfo> existingFiles = fileRepository.findByName(path.getFileName().toString());

        for(FileInfo nextFile: existingFiles) {
            LOG.info("{}", nextFile);

            // Make sure this file is from the same source.
            if(nextFile.getDirectoryInfo().getSource().getId() != source.getId()) {
                continue;
            }

            // Get the details of the file - size & md5.
            FileTestResultType testResult = fileAlreadyExists(path,nextFile,importFile.getFileInfo());
            if(testResult == FileTestResultType.EXACT) {
                // Delete the file from import.
                LOG.info("{} exists in source, deleting",path);
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    LOG.warn("Failed to delete {}, already imported", path);
                }
            }

            return testResult;
        }

        return FileTestResultType.DIFFERENT;
    }

    private void processConfirmedAction(ImportFile importFile, Path path, List<ActionConfirm> confirmedActions, Source source, String parameter) {
        for(ActionConfirm nextConfirm: confirmedActions) {
            actionConfirmRepository.delete(nextConfirm);
        }

        // If the parameter value is IGNORE then add this file to the ignore list.
        if(parameter.equalsIgnoreCase("ignore")) {
            IgnoreFile ignoreFile = new IgnoreFile();
            ignoreFile.setDate(importFile.getFileInfo().getDate());
            ignoreFile.setName(importFile.getFileInfo().getName());
            ignoreFile.setSize(importFile.getFileInfo().getSize());
            ignoreFile.setMD5(importFile.getFileInfo().getMD5());

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

    private void processImport(ImportFile importFile, Source source) {
        // If this file is completed then exit.
        if(importFile.getStatus().equalsIgnoreCase("complete")) {
            return;
        }

        // Get the path to the import file.
        Path path = new File(importFile.getFileInfo().getFullFilename()).toPath();

        // What is the classification? if yes, unless this is a backup file just remove it.
        if(!processClassification(importFile,path)) {
            return;
        }

        // Get details of the file to import.
        if(importFile.getFileInfo().getMD5() == null || importFile.getFileInfo().getMD5().length() <= 0) {
            importFile.getFileInfo().setMD5(getMD5(path, importFile.getFileInfo().getClassification()));

            fileRepository.save(importFile.getFileInfo());
        }

        // Is this file being ignored?
        if(!processIgnored(importFile,path)) {
            return;
        }

        // Does this file already exist in the source?
        FileTestResultType existingState = processExisting(importFile,path,source);
        if(FileTestResultType.EXACT == existingState) {
            return;
        }

        // We can import this file but need to know where.
        // Photos are in <source> / <year> / <month> / <event> / filename
        List<ActionConfirm> confirmedActions = actionConfirmRepository.findByFileInfoAndAction(importFile.getFileInfo(),"IMPORT");
        if(!confirmedActions.isEmpty()) {
            processImportActions(importFile,path,confirmedActions,source);
        } else {
            // Create an action to be confirmed.
            ActionConfirm actionConfirm = new ActionConfirm();
            actionConfirm.setFileInfo(importFile.getFileInfo());
            actionConfirm.setAction("IMPORT");
            actionConfirm.setConfirmed(false);
            actionConfirm.setParameterRequired(true);
            actionConfirm.setFlags(FileTestResultType.CLOSE == existingState ? "C" : null);

            actionConfirmRepository.save(actionConfirm);
        }
    }

    public void importPhotoProcess() throws ImportRequestException {
        LOG.info("Import Photo Process");

        // Get the source.
        Optional<Source> source = Optional.empty();

        for(Source nextSource: sourceRepository.findAll()) {
            if(nextSource.getTypeEnum() == Source.SourceTypeType.IMPORT) {
                source = Optional.of(nextSource);
            }
        }

        if(!source.isPresent()) {
            throw new ImportRequestException("There is no import source defined.");
        }

        // Get the place they are to be imported to.
        Optional<Source> destination = sourceRepository.findById(source.get().getDestinationId());
        if(!destination.isPresent()) {
            throw new ImportRequestException("Destination for import is not found.");
        }

        for(ImportFile nextFile: importFileRepository.findAll()) {
            LOG.info(nextFile.getFileInfo().getFullFilename());

            processImport(nextFile,destination.get());

            nextFile.setStatus("COMPLETE");
            importFileRepository.save(nextFile);
        }
    }

    public void removeEntries() {
        // Remove entries from import table if they are no longer present.
        for (ImportFile nextFile : importFileRepository.findAll()) {
            // Does this file still exist?
            File existingFile = new File(nextFile.getFileInfo().getFullFilename());

            if(!existingFile.exists()) {
                LOG.info("Remove this import file - {}", nextFile.getFileInfo().getFullFilename());
                importFileRepository.delete(nextFile);
            } else {
                LOG.info("Keeping {}", nextFile.getFileInfo().getFullFilename());
            }
        }
    }


    enum FileTestResultType {EXACT, CLOSE, DIFFERENT}

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
                String sourceFilename = fileInfo.getFullFilename();

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

    @Override
    void newFileInserted(FileInfo newFile) {
        ImportFile newImportFile = new ImportFile();
        newImportFile.setStatus("READ");
        newImportFile.setFileInfo(newFile);

        importFileRepository.save(newImportFile);
    }
}
