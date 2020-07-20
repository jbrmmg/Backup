package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
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
    private final static Logger LOG = LoggerFactory.getLogger(SynchronizeManager.class);

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

    @Transactional
    public void importPhoto(ImportRequest importRequest) throws Exception {
        actionConfirmRepository.clearImports();

        // Get the classifications
        Iterable<Classification> classifications = classificationRepository.findAll();

        // Remove any existing import data.
        clearImports();

        // Check the path exists
        File importPath = new File(importRequest.getPath());
        if(!importPath.exists()) {
            throw new IOException("The path does not exist - " + importPath);
        }

        // Validate the source.
        Optional<Source> source = sourceRepository.findById(importRequest.getSource());
        if(!source.isPresent()) {
            throw new  IOException("The source does not exist - " + importRequest.getSource());
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
        importSource.setTypeEnum(Source.SourceTypeType.Import);
        importSource.setDestinationId(source.get().getId());
        importSource.setLocation(importLocation.get());

        sourceRepository.save(importSource);

        // Perform the import, find all the files to import and take action.
        // Read directory structure into the database.
        try (Stream<Path> paths = Files.walk(Paths.get(importRequest.getPath()))) {
            paths
                    .forEach(path -> processPath(path,new ArrayList<ActionConfirm>(),importSource,classifications,true));
        } catch (IOException e) {
            LOG.error("Failed to process import - ",e);
            throw e;
        }
    }

    @Transactional
    public void clearImports() throws Exception {
        // Remove the files associated with imports - first remove files, then directories then source.
        for(Source nextSource: sourceRepository.findAll()) {
            if(nextSource.getTypeEnum() == Source.SourceTypeType.Import) {
                for(DirectoryInfo nextDirectory: directoryRepository.findBySource(nextSource)) {
                    for(FileInfo nextFile: fileRepository.findByDirectoryInfoId(nextDirectory.getId())) {
                        fileRepository.delete(nextFile);
                    }

                    directoryRepository.delete(nextDirectory);
                }

                sourceRepository.delete(nextSource);
            }
        }

        importFileRepository.deleteAll();
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

    private void processImport(ImportFile importFile, Source source) {
        // If this file is completed then exit.
        if(importFile.getStatus().equalsIgnoreCase("complete")) {
            return;
        }

        // Get the path to the import file.
        Path path = new File(importFile.getFileInfo().getFullFilename()).toPath();

        // What is the classification? if yes, unless this is a backup file just remove it.
        if(importFile.getFileInfo().getClassification() != null) {
            if(!importFile.getFileInfo().getClassification().getAction().equalsIgnoreCase("backup")) {
                LOG.info(path.toString() + " not a backed up file, deleting");
                if(!path.toFile().delete()) {
                    LOG.warn("Failed to delete file " + path.toString());
                }
                return;
            }
        }

        // Get details of the file to import.
        if(importFile.getFileInfo().getMD5() == null || importFile.getFileInfo().getMD5().length() <= 0) {
            importFile.getFileInfo().setMD5(getMD5(path, importFile.getFileInfo().getClassification()));

            fileRepository.save(importFile.getFileInfo());
        }

        // Is this file being ignored?
        if(ignoreFile(importFile.getFileInfo())) {
            // Delete the file from import.
            LOG.info(path.toString() + " marked for ignore, deleting");
            if(!path.toFile().delete()) {
                LOG.warn("Failed to delete file " + path.toString());
            }
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
            FileTestResultType testResult = fileAlreadyExists(path,nextFile,importFile.getFileInfo());
            if(testResult == FileTestResultType.EXACT) {
                // Delete the file from import.
                LOG.info("{} exists in source, deleting",path.toString());
                if(!path.toFile().delete()) {
                    LOG.warn("Failed to delete file: {}", path.toString());
                }
                return;
            }

            if(testResult == FileTestResultType.CLOSE) {
                closeMatch = true;
            }
        }

        // We can import this file but need to know where.
        // Photos are in <source> / <year> / <month> / <event> / filename

        List<ActionConfirm> confirmedActions = actionConfirmRepository.findByFileInfoAndAction(importFile.getFileInfo(),"IMPORT");
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
            actionConfirm.setFileInfo(importFile.getFileInfo());
            actionConfirm.setAction("IMPORT");
            actionConfirm.setConfirmed(false);
            actionConfirm.setParameterRequired(true);
            if(closeMatch) {
                // Indicate it was a close match.
                actionConfirm.setFlags("C");
            }

            actionConfirmRepository.save(actionConfirm);
        }
    }

    public void importPhotoProcess() throws Exception {
        LOG.info("Import Photo Process");

        // Get the source.
        Optional<Source> source = Optional.empty();

        for(Source nextSource: sourceRepository.findAll()) {
            if(nextSource.getTypeEnum() == Source.SourceTypeType.Import) {
                source = Optional.of(nextSource);
            }
        }

        if(!source.isPresent()) {
            throw new Exception("There is no import source defined.");
        }

        // Get the place they are to be imported to.
        Optional<Source> destination = sourceRepository.findById(source.get().getDestinationId());
        if(!destination.isPresent()) {
            throw new Exception("The destination is invalid.");
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
                LOG.info("Remove this import file - " + nextFile.getFileInfo().getFullFilename());
                importFileRepository.delete(nextFile);
            } else {
                LOG.info("Keeping " + nextFile.getFileInfo().getFullFilename());
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

    @Override
    void newFileInserted(FileInfo newFile) {
        ImportFile newImportFile = new ImportFile();
        newImportFile.setStatus("READ");
        newImportFile.setFileInfo(newFile);

        importFileRepository.save(newImportFile);
    }
}
