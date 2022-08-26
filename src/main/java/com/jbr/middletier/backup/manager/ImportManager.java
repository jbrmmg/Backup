package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.dto.ImportDataDTO;
import com.jbr.middletier.backup.dto.ImportFileDTO;
import com.jbr.middletier.backup.dto.ImportProcessDTO;
import com.jbr.middletier.backup.exception.ImportRequestException;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.database.DbFile;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Comparator.comparing;

@Component
public class ImportManager extends FileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ImportManager.class);

    private final ImportFileRepository importFileRepository;
    private final IgnoreFileRepository ignoreFileRepository;
    private final ApplicationProperties applicationProperties;
    private final ModelMapper modelMapper;

    @Autowired
    public ImportManager(ImportFileRepository importFileRepository,
                         AssociatedFileDataManager associatedFileDataManager,
                         FileSystemObjectManager fileSystemObjectManager,
                         IgnoreFileRepository ignoreFileRepository,
                         DbLoggingManager dbLoggingManager,
                         ActionManager actionManager,
                         FileSystem fileSystem,
                         ApplicationProperties applicationProperties,
                         ModelMapper modelMapper) {
        super(dbLoggingManager,actionManager,associatedFileDataManager,fileSystemObjectManager,fileSystem);
        this.importFileRepository = importFileRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.applicationProperties = applicationProperties;
        this.modelMapper = modelMapper;
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
        if((importFile.getClassification() == null) || !importFile.getClassification().getAction().equals(ClassificationActionType.CA_BACKUP)) {
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

    private FileTestResultType processExisting(ImportFile importFile, Path path, ImportDataDTO result) {
        // Find files with the same name.
        Iterable<FileSystemObject> existingFiles = fileSystemObjectManager.findFileSystemObjectByName(importFile.getName(), FileSystemObjectType.FSO_FILE);

        for(FileSystemObject nextFile: existingFiles) {
            if(nextFile.getIdAndType().getType() != FileSystemObjectType.FSO_FILE)
                continue;

            LOG.info("{}", nextFile);

            // Get the details of the file - size & md5.
            FileTestResultType testResult = fileAlreadyExists(path,(FileInfo)nextFile,importFile);
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

        if(parameter.equalsIgnoreCase("<recipe>")) {
            newFilename += "/0000/recipe";
        } else {
            // Use the date of the file.
            Date fileDate = new Date(path.toFile().lastModified());

            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy");
            SimpleDateFormat sdf2 = new SimpleDateFormat("MMMM");

            newFilename += "/" + sdf1.format(fileDate);
            newFilename += "/" + sdf2.format(fileDate);
            newFilename += "/" + parameter;
        }

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

    private ImportFileStatusType processImport(ImportFile importFile, Source source, ImportDataDTO result) throws IOException {
        if(importFile.getStatus() == ImportFileStatusType.IFS_REMOVED) {
            return ImportFileStatusType.IFS_REMOVED;
        }

        // Get the path to the import file.
        Path path = fileSystemObjectManager.getFile(importFile).toPath();

        // What is the classification? if yes, unless this is a backup file just remove it.
        if(!processClassification(importFile,path,result)) {
            return ImportFileStatusType.IFS_REMOVED;
        }

        // Get details of the file to import.
        if(!importFile.getMD5().isSet()) {
            importFile.setMD5(fileSystem.getClassifiedFileMD5(path, importFile.getClassification()));

            fileSystemObjectManager.save(importFile);
        }

        // Is this file being ignored?
        if(!processIgnored(importFile,path,result)) {
            return ImportFileStatusType.IFS_REMOVED;
        }

        // Does this file already exist in the source?
        FileTestResultType existingState = processExisting(importFile,path,result);
        if(FileTestResultType.EXACT == existingState) {
            return ImportFileStatusType.IFS_REMOVED;
        }

        // We can import this file but need to know where.
        // Photos are in <source> / <year> / <month> / <event> / filename
        List<ActionConfirm> confirmedActions = actionManager.getActionsForFile(importFile);
        if(!confirmedActions.isEmpty()) {
            processImportActions(importFile,path,confirmedActions,source,result);
            return ImportFileStatusType.IFS_COMPLETE;
        }

        // Create an action to be confirmed.
        actionManager.createFileImportAction(importFile,FileTestResultType.CLOSE == existingState ? "C" : null);
        return ImportFileStatusType.IFS_AWAITING_ACTION;
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

        // If the classification requires an MD5, and it's missing from one side or the other,
        //  then calculate it now.
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

    @Override
    public FileInfo createNewFile() {
        ImportFile newFile = new ImportFile();
        newFile.setStatus(ImportFileStatusType.IFS_READ);
        return newFile;
    }

    private enum ProcessType { USE_EXIF_DATE, CONVERT_QUICKTIME, NORMAL }

    private String getDestinationFilename(String filename, ProcessType processType) {
        if(processType.equals(ProcessType.CONVERT_QUICKTIME)) {
            return filename.replace(".MOV", ".mp4");
        }

        return filename;
    }

    private ProcessType getFileType(Optional<FileSystemImageData> imageData) {
        if(imageData.isPresent() && imageData.get().isValid()) {
            if(imageData.get().getDateSourceType().equals(ImageDataDirectoryType.IDD_QUICKTIME)) {
                return ProcessType.CONVERT_QUICKTIME;
            }

            return ProcessType.USE_EXIF_DATE;
        }

        // Everything else is a normal copy
        return ProcessType.NORMAL;
    }

    private void copyFileWithExifMetadata(String source, String filename, String destination, Optional<FileSystemImageData> imageData, ImportProcessDTO data) {
        File imageFile = new File(source,filename);

        File destinationImageFile = new File(destination, filename);
        fileSystem.copyFile(imageFile, destinationImageFile, data);
        data.increment(ImportProcessDTO.ImportProcessCountType.IMAGE_FILES);

        if(imageData.isPresent() && imageData.get().getDateTime() != null) {
            ZonedDateTime zonedFileTime = imageData.get().getDateTime().atZone(ZoneId.systemDefault());
            fileSystem.setFileDateTime(destinationImageFile, zonedFileTime.toInstant().toEpochMilli());
        }
    }

    private void copyAndConvertQuicktime(String source, String filename, String destination, ImportProcessDTO data) {
        try {
            File movFile = new File(source, filename);
            File mp4File = new File(destination, filename.replace(".MOV", ".mp4"));

            long fileTime = movFile.lastModified();
            Optional<FileSystemImageData> imageData = fileSystem.readImageMetaData(movFile);
            if(imageData.isPresent() && imageData.get().getDateTime() != null) {
                ZonedDateTime zonedFileTime = imageData.get().getDateTime().atZone(ZoneId.systemDefault());
                fileTime = zonedFileTime.toInstant().toEpochMilli();
            }

            String copyCommand = applicationProperties.getFfmpegCommand();
            copyCommand = copyCommand.replace("%%INPUT%%", movFile.toString().replace(" ", "\\ "));
            copyCommand = copyCommand.replace("%%OUTPUT%%", mp4File.toString().replace(" ", "\\ "));

            LOG.info("Command: {}", copyCommand);

            String[] cmd = new String[]{"bash", "-c", copyCommand};
            final Process backupProcess = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            backupProcess.waitFor(20L, TimeUnit.MINUTES);
            backupProcess.destroyForcibly();

            data.increment(ImportProcessDTO.ImportProcessCountType.MOV_FILES);
            fileSystem.setFileDateTime(mp4File, fileTime);
        } catch (Exception e) {
            LOG.error("Failed to copy MOV file", e);
            data.setProblems();
            Thread.currentThread().interrupt();
        }
    }

    private void copyFile(String source, String filename, String destination, ImportProcessDTO data) {
        File sourceFile = new File(source, filename);
        File destinationFile = new File(destination, filename);
        long fileTime = sourceFile.lastModified();

        fileSystem.copyFile(sourceFile, destinationFile, data);

        fileSystem.setFileDateTime(destinationFile, fileTime);
    }

    private void processFile(String source, String filename, String destination, ImportProcessDTO data) {
        Optional<FileSystemImageData> imageData = fileSystem.readImageMetaData(new File(source,filename));

        ProcessType processType = getFileType(imageData);
        String destinationFilename = getDestinationFilename(filename, processType);

        // If the destination already exists then we are done.
        if(fileSystem.fileExists(new File(destination, destinationFilename))) {
            data.increment(ImportProcessDTO.ImportProcessCountType.ALREADY_PRESENT);
            return;
        }

        switch(processType) {
            case USE_EXIF_DATE:
                copyFileWithExifMetadata(source, filename, destination, imageData, data);
                break;
            case CONVERT_QUICKTIME:
                copyAndConvertQuicktime(source, filename, destination, data);
                break;
            case NORMAL:
                copyFile(source, filename, destination, data);
                break;
        }
        data.increment(ImportProcessDTO.ImportProcessCountType.FILES_PROCESSED);
    }

    private Optional<PreImportSource> findPreImportSource() {
        Optional<PreImportSource> result = Optional.empty();

        int count = 0;
        for(PreImportSource nextSource : associatedFileDataManager.findAllPreImportSource()) {
            result = Optional.of(nextSource);

            if(count > 0) {
                LOG.warn("Too many pre import source, do not import.");
                return Optional.empty();
            }

            count++;
        }

        return result;
    }

    private Optional<ImportSource> findImportSource() {
        Optional<ImportSource> result = Optional.empty();

        int count = 0;
        for(ImportSource nextSource : associatedFileDataManager.findAllImportSource()) {
            result = Optional.of(nextSource);

            if(count > 0) {
                LOG.warn("Too many import source, do not import.");
                return Optional.empty();
            }

            count++;
        }

        return result;
    }

    public List<ImportProcessDTO> convertImportFiles() {
        List<ImportProcessDTO> result = new ArrayList<>();
        ImportProcessDTO resultCount = new ImportProcessDTO();
        result.add(resultCount);

        try {
            // Find the pre-import details.
            Optional<PreImportSource> preImportSource = findPreImportSource();
            if(!preImportSource.isPresent()) {
                resultCount.setProblems();
                LOG.warn("Invalid Pre Import Source - skipping import.");
                return result;
            }

            // Find the import details
            Optional<ImportSource> importSource = findImportSource();
            if(!importSource.isPresent()) {
                resultCount.setProblems();
                LOG.warn("Invalid Import Source - skipping import.");
                return result;
            }

            LOG.info("Process Files from {}", preImportSource.get().getPath());
            LOG.info("Into {}", importSource.get().getPath());

            // Set up the files.
            File source = new File(preImportSource.get().getPath());
            File destination = new File(importSource.get().getPath());

            // Check that the source exists.
            if(!fileSystem.directoryExists(source.toPath())) {
                throw new IllegalStateException(preImportSource.get().getPath() + " does not exist.");
            }

            // Check that the destination exists.
            if(!fileSystem.directoryExists(destination.toPath())) {
                throw new IllegalStateException(importSource.get().getPath() + " does not exist.");
            }

            for(String nextFilename : fileSystem.listFilesInDirectory(preImportSource.get().getPath())) {
                processFile(preImportSource.get().getPath(),
                        nextFilename,
                        importSource.get().getPath(),
                        resultCount );
            }
        } catch (Exception e) {
            resultCount.setProblems();
            LOG.error("Problems",e);
        }

        LOG.info("Convert import files is complete.");

        return result;
    }

    public List<GatherDataDTO> importPhoto() throws ImportRequestException, IOException {
        List<GatherDataDTO> result = new ArrayList<>();

        // Find the import source
        Optional<ImportSource> importSource = findImportSource();
        if(!importSource.isPresent()) {
            throw new ImportRequestException("No import source is defined.");
        }

        // Check the path exists
        File importPath = new File(importSource.get().getPath());
        if (!importPath.exists()) {
            throw new ImportRequestException("The path does not exist - " + importPath);
        }

        // Remove any import actions.
        actionManager.clearImportActions();

        // Perform the import, find all the files to import and take action.
        // Read directory structure into the database.
        GatherDataDTO gatherData = new GatherDataDTO(importSource.get().getIdAndType().getId());
        updateDatabase(importSource.get(), new ArrayList<>(), true, gatherData);

        result.add(gatherData);
        LOG.info("Import photo is complete.");

        return result;
    }

    public List<ImportDataDTO> processImportFiles() throws ImportRequestException {
        LOG.info("Import Photo Process");
        List<ImportDataDTO> result = new ArrayList<>();

        // Get the source.
        Optional<ImportSource> importSource = Optional.empty();
        for(ImportSource nextSource: associatedFileDataManager.findAllImportSource()) {
            importSource = Optional.of(nextSource);
        }

        if(!importSource.isPresent()) {
            throw new ImportRequestException("There is no import source defined.");
        }
        ImportDataDTO resultItem = new ImportDataDTO(importSource.get().getIdAndType().getId());
        result.add(resultItem);

        try {
            // Get the place they are to be imported to.
            Optional<Source> destination = associatedFileDataManager.findSourceIfExists(importSource.get().getDestination().getIdAndType().getId());
            if (!destination.isPresent()) {
                throw new ImportRequestException("Destination for import is not found.");
            }

            for (ImportFile nextFile : importFileRepository.findAll()) {
                LOG.info( "{} MD5: {}", nextFile.getName(), nextFile.getMD5());

                ImportFileStatusType newStatus = processImport(nextFile, destination.get(), resultItem);

                if(nextFile.getStatus() != newStatus) {
                    nextFile.setStatus(newStatus);
                    importFileRepository.save(nextFile);
                }
            }
        } catch (Exception e) {
            resultItem.setProblems();
        }
        LOG.info("Process import files is complete.");

        return result;
    }

    private void searchSimilarFileData(ImportFileDTO file, FileTreeNode node) {
        // Is this node a file?
        if(node instanceof DbFile) {
            DbFile dbFile = (DbFile) node;

            Optional<String> name = dbFile.getName();
            if(name.isPresent() && file.getFilename().equalsIgnoreCase(name.get())) {
                file.addSimilarFile((FileInfo)dbFile.getFSO());
            }
        }

        // Check the children.
        for(FileTreeNode nextChild : node.getChildren()) {
            searchSimilarFileData(file,nextChild);
        }
    }

    private void addSimilarFileData(List<ImportFileDTO> files) {
        try {
            Optional<ImportSource> importSource = findImportSource();
            if(!importSource.isPresent()) {
                return;
            }

            DbRoot database = fileSystemObjectManager.createDbRoot(importSource.get());

            for(ImportFileDTO nextFile : files) {
                searchSimilarFileData(nextFile, database);
            }
        } catch (Exception ex) {
            dbLoggingManager.error("Failed to add similar data " + ex);
        }
    }

    public List<ImportFileDTO> externalFindImportFiles() {
        List<ImportFileDTO> result = new ArrayList<>();
        for(ImportFile nextFile: findImportFiles()) {
            result.add(modelMapper.map(nextFile, ImportFileDTO.class));
        }

        // Update with the similar files from the destination source.
        addSimilarFileData(result);

        // Sort the result
        result.sort(comparing(ImportFileDTO::getFilename));

        return result;
    }
}
