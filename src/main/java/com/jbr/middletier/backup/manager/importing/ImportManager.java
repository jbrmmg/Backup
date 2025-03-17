package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.ImportRequestException;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.database.DbFile;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import com.jbr.middletier.backup.manager.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.jbr.middletier.backup.manager.FileProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Comparator.comparing;

@Component
public class ImportManager extends FileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ImportManager.class);

    private final ImportFileRepository importFileRepository;
    private final IgnoreFileRepository ignoreFileRepository;
    private final ApplicationProperties applicationProperties;
    private final ModelMapper modelMapper;
    private final ImportFileCache importFileCache;
    private final FileRepository fileRepository;
    private final ImportFileWorkQueue importFileWorkQueue;
    private LocalDateTime currentTime;
    private LocalDateTime previousTime;

    @Autowired
    public ImportManager(ImportFileRepository importFileRepository,
                         AssociatedFileDataManager associatedFileDataManager,
                         FileSystemObjectManager fileSystemObjectManager,
                         IgnoreFileRepository ignoreFileRepository,
                         DbLoggingManager dbLoggingManager,
                         ActionManager actionManager,
                         FileSystem fileSystem,
                         ApplicationProperties applicationProperties,
                         ModelMapper modelMapper,
                         FileRepository fileRepository,
                         ImportFileCache importFileCache,
                         ImportFileWorkQueue importFileWorkQueue) {
        super(dbLoggingManager,actionManager,associatedFileDataManager,fileSystemObjectManager,fileSystem);
        this.importFileRepository = importFileRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.applicationProperties = applicationProperties;
        this.modelMapper = modelMapper;
        this.importFileCache = importFileCache;
        this.fileRepository = fileRepository;
        this.importFileWorkQueue = importFileWorkQueue;
        this.currentTime = LocalDateTime.now();
        this.previousTime = currentTime;
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
            fileSystem.deleteFile(path.toFile(),result,importFile.getIdAndType().getId());
            return false;
        }

        return true;
    }

    private boolean processIgnored(ImportFile importFile, Path path, ImportDataDTO result) {
        if(ignoreFile(importFile)) {
            result.increment(ImportDataDTO.ImportDataCountType.IGNORED_IMPORTS);
            // Delete the file from import.
            LOG.info("{} marked for ignore, deleting", path);
            fileSystem.deleteFile(path.toFile(), result,importFile.getClassification().getId());
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
                fileSystem.deleteFile(path.toFile(),result,importFile.getIdAndType().getId());
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
            if(nextConfirm.confirmed() && nextConfirm.getParameter() != null && !nextConfirm.getParameter().isEmpty()) {
                parameter = nextConfirm.getParameter();
                confirmed = true;
            }
        }

        if(confirmed && !parameter.isEmpty()) {
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
            importFile.setMD5(fileSystem.getClassifiedFileMD5(path, importFile.getClassification(), importFile.getIdAndType().getId()));

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
        fileInfo.setMD5(fileSystem.getClassifiedFileMD5(path,classification,fileInfo.getIdAndType().getId()));

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

    public Optional<PreImportSource> findPreImportSource() {
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
            if(preImportSource.isEmpty()) {
                resultCount.setProblems();
                LOG.warn("Invalid Pre Import Source - skipping import.");
                return result;
            }

            // Find the import details
            Optional<ImportSource> importSource = findImportSource();
            if(importSource.isEmpty()) {
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
        if(importSource.isEmpty()) {
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

        if(importSource.isEmpty()) {
            throw new ImportRequestException("There is no import source defined.");
        }
        ImportDataDTO resultItem = new ImportDataDTO(importSource.get().getIdAndType().getId());
        result.add(resultItem);

        try {
            // Get the place they are to be imported to.
            Optional<Source> destination = associatedFileDataManager.findSourceIfExists(importSource.get().getDestination().getIdAndType().getId());
            if (destination.isEmpty()) {
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

    private String removeFileExtension(String filename) {
        return filename.replaceFirst("[.][^.]+$","");
    }

    private boolean similarFileName(String lhs, String rhs) {
        return removeFileExtension(lhs).equalsIgnoreCase(removeFileExtension(rhs));
    }

    private void searchSimilarFileData(ImportFileDTO file, FileTreeNode node) {
        // Is this node a file?
        if(node instanceof DbFile dbFile) {

            Optional<String> name = dbFile.getName();
            if(name.isPresent() && similarFileName(file.getFilename(),name.get())) {
                file.addSimilarFile(modelMapper.map(dbFile.getFSO(), ImportFileBaseDTO.class));
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
            if(importSource.isEmpty()) {
                return;
            }

            DbRoot database = fileSystemObjectManager.createDbRoot(importSource.get().getDestination());

            for(ImportFileDTO nextFile : files) {
                searchSimilarFileData(nextFile, database);
            }
        } catch (Exception ex) {
            dbLoggingManager.error("Failed to add similar data " + ex,null,null);
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

    public ImportFileDTO externalFindImportFile(Integer id) throws InvalidFileIdException {
        for(ImportFileDTO nextImportFile : externalFindImportFiles()) {
            if(nextImportFile.getId().equals(id)) {
                return nextImportFile;
            }
        }

        throw new InvalidFileIdException(id);
    }

    public void restartQueue() {
        this.importFileWorkQueue.restart();
    }

    public List<PreImportFileDTO> getUpdates() {
        this.previousTime = this.currentTime.minusSeconds(1);
        this.currentTime = LocalDateTime.now();
        List<PreImportFileDTO> result = new ArrayList<>();

        // Return the list of files that have been updated.
        for(String filename: this.importFileCache.getFiles()){
            PreImportFileDTO next = this.importFileCache.get(filename);

            if(next.updatedSince(this.previousTime)) {
                result.add(next);
            }
        }

        return result;
    }

    public void queueForUpdates(PreImportFileDTO importFile) {
        if( importFile.getImmediateImported().equals(TrafficLightType.TL_UNKNOWN) ||
                importFile.getImported().equals(TrafficLightType.TL_UNKNOWN) ||
                importFile.getDuplicated().equals(TrafficLightType.TL_UNKNOWN) ||
                importFile.getIgnored().equals(TrafficLightType.TL_UNKNOWN) ) {
            this.importFileWorkQueue.add(importFile);
        }
    }

    public List<PreImportFileDTO> externalFindPreImportFiles() {
        this.importFileWorkQueue.clear();
        this.currentTime = LocalDateTime.now();

        // Set up the sources that we will restrict results to.
        List<Source> validSources = new ArrayList<>();
        for(Synchronize synchronize: this.associatedFileDataManager.findAllSynchronize()) {
            if(!validSources.contains(synchronize.getSource())) {
                validSources.add(synchronize.getSource());
            }
        }

        for(Source source: this.associatedFileDataManager.findAllSource()) {
            if(source.getIdAndType().getType().equals(FileSystemObjectType.FSO_PRE_IMPORT_SOURCE) ||
               source.getIdAndType().getType().equals(FileSystemObjectType.FSO_IMPORT_SOURCE)) {
                if(!validSources.contains(source)) {
                    validSources.add(source);
                }
            }
        }

        // Get data from the pre-import directory.
        List<PreImportFileDTO> result = new ArrayList<>();

        // Get the actual files that are in the pre-import directory.
        Optional<PreImportSource> preImportSource = findPreImportSource();
        if(preImportSource.isEmpty()) {
            LOG.warn("Invalid Pre Import Source, returning empty list.");
            return result;
        }

        LOG.info("Read files from {}", preImportSource.get().getPath());
        File source = new File(preImportSource.get().getPath());

        // Check that the source exists.
        if(!fileSystem.directoryExists(source.toPath())) {
            LOG.warn("Pre import does not exist, returning empty list.");
            return result;
        }

        int tempCount = 0;
        for(String nextFilename : fileSystem.listFilesInDirectory(preImportSource.get().getPath())) {
            // TODO - when completed remove this
            if(tempCount++ > 30) { // Temporary limit
                break;
            }

            // Is this file in the cache?
            PreImportFileDTO importFile;
            if(this.importFileCache.containsKey(nextFilename)) {
                importFile = this.importFileCache.get(nextFilename);
            } else {
                // Lookup the data.
                importFile = new PreImportFileDTO();

                importFile.setFilename(nextFilename);
                importFile.setStatus(ImportFileStatusType.IFS_READ);
                importFile.setId(-1);
                importFile.setSize(0L);
                importFile.setImmediateImported(TrafficLightType.TL_UNKNOWN);
                importFile.setImported(TrafficLightType.TL_UNKNOWN);
                importFile.setDuplicated(TrafficLightType.TL_UNKNOWN);
                importFile.setIgnored(TrafficLightType.TL_UNKNOWN);

                this.importFileCache.put(nextFilename,importFile);
            }

            result.add(importFile);

            // If required queue the file to get updated.
            queueForUpdates(importFile);
        }

        return result;
    }

    private ImportFileBaseDTO getSimilar(FileInfo fileInfo, List<Source> validSources) {
        ImportFileBaseDTO similar = new ImportFileBaseDTO();
        similar.setFilename(fileInfo.getName() + " [" + fileInfo.getIdAndType().getType().getTypeName() + "]");
        similar.setSize(fileInfo.getSize());
        similar.setMd5(fileInfo.getMD5());
        similar.setDate(fileInfo.getDate());

        // Get the full filename.
        File file = fileSystemObjectManager.getFile(fileInfo);
        if(!file.getPath().equalsIgnoreCase(file.getName())) {
            // Only accept file if its from the valid source.
            AtomicBoolean accept = new AtomicBoolean(false);
            validSources.forEach(source -> {
                if(file.getPath().contains(source.getPath())) {
                    accept.set(true);
                }
            });

            if(!accept.get()) {
                return null;
            }

            similar.setFilename(file.getPath());
        }

        return similar;
    }

    public List<FileInfo> getSimilarIgnore(String filename, String md5) {
        // Return ignore files that match either the name or the MD5.
        List<FileInfo> result = new ArrayList<>(ignoreFileRepository.findByName(filename));

        if(md5 != null && !md5.isEmpty()) {
            result.addAll(ignoreFileRepository.findByMd5(md5));
        }

        return result;
    }

    private void addFileToListIfRequired(FileInfo file, List<Source> validSources, List<ImportFileBaseDTO> result) {
        if (!file.getIdAndType().getType().equals(FileSystemObjectType.FSO_IGNORE_FILE) &&
                !file.getIdAndType().getType().equals(FileSystemObjectType.FSO_IMPORT_FILE)) {

            ImportFileBaseDTO similar = getSimilar(file,validSources);
            if(similar != null) {
                // Make sure this is not already in the list.
                for(ImportFileBaseDTO next : result) {
                    if(similar.getFilename().equalsIgnoreCase(next.getFilename())) {
                        return;
                    }
                }

                result.add(similar);
            }
        }
    }

    public List<ImportFileBaseDTO> getSimilarImported(String filename, String md5) {
        List<ImportFileBaseDTO> result = new ArrayList<>();

        // Set up the sources that we will restrict results to.
        List<Source> validSources = new ArrayList<>();
        for(Synchronize synchronize: this.associatedFileDataManager.findAllSynchronize()) {
            if(!validSources.contains(synchronize.getSource())) {
                validSources.add(synchronize.getSource());
            }
        }

        // Find files that are not ignored and not imported but match either on the name or the MD5
        for (FileSystemObject next : fileSystemObjectManager.findFileSystemObjectByName(filename, FileSystemObjectType.FSO_FILE)) {
            // Not including ignored or import.
            if(next instanceof FileInfo nextFI) {
                addFileToListIfRequired(nextFI,validSources,result);
            }
        }
        for (FileSystemObject next : fileSystemObjectManager.findFileSystemObjectByMd5(md5, FileSystemObjectType.FSO_FILE)) {
            // Not including ignored or import.
            if(next instanceof FileInfo nextFI) {
                addFileToListIfRequired(nextFI,validSources,result);
            }
        }

        return result;
    }

    public List<FileInfo> getImport(String filename) {
        // Return ignore files that match either the name or the MD5.
        List<FileInfo> result = new ArrayList<>();
        for(FileInfo next: importFileRepository.findByName(filename)) {
            result.add(next);
        }

        return result;
    }

    public boolean deletePreImportFile(String filename) {
        // Get the actual files that are in the pre-import directory.
        Optional<PreImportSource> preImportSource = findPreImportSource();
        if(preImportSource.isEmpty()) {
            LOG.warn("Invalid Pre Import Source for delete, returning empty list.");
            return false;
        }
        File preImportFile = new File(preImportSource.get().getPath().trim(), filename);

        Optional<ImportSource> importSource = findImportSource();
        if(importSource.isEmpty()) {
            return false;
        }
        File importFile = new File(importSource.get().getPath().trim(), filename);

        // Delete the file named from the pre-import directory, the import directory and the import table.

        // (1) remove from the file import table.
        fileRepository.deleteAll(importFileRepository.findByName(filename));

        // (2) remove from the import directory.
        ProcessResultDTO deleteResult = new ImportProcessDTO();
        fileSystem.deleteFile(importFile, deleteResult,0);

        if(deleteResult.hasProblems()) {
            LOG.warn("Failed to delete the file from import - {}", filename);
            return false;
        }

        // (3) remove from the pre-import directory
        deleteResult = new ImportProcessDTO();
        fileSystem.deleteFile(preImportFile, deleteResult, 0);

        if(deleteResult.hasProblems()) {
            LOG.warn("Failed to delete the file from pre-import - {}", filename);
            return false;
        }

        // Remove from the cache
        this.importFileCache.remove(filename);

        return true;
    }

    public byte[] getImage(String name) {
        try {
            // read the specified file.
            Optional<PreImportSource> preImportSource = findPreImportSource();
            if (preImportSource.isEmpty()) {
                LOG.warn("Invalid Pre Import Source, returning empty list.");
                return null;
            }

            LOG.info("Read files from {}", preImportSource.get().getPath());
            File source = new File(preImportSource.get().getPath(), name);

            return fileSystem.readAllBytes(source);
        } catch (IOException e) {
            LOG.warn("Read files from {} failed", name, e);
        }

        return null;
    }
}
