package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.ImportRequestException;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.database.DbFile;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import com.jbr.middletier.backup.manager.*;
import com.jbr.middletier.backup.util.ImageSize;
import com.jbr.middletier.backup.util.LatLong;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import com.jbr.middletier.backup.manager.FileProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Comparator.comparing;

@Component
public class ImportManager extends FileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ImportManager.class);

    private static final String RECIPE_FILE_DESTINATION = "[** recipe **]";
    private final ImportFileRepository importFileRepository;
    private final IgnoreFileRepository ignoreFileRepository;
    private final ModelMapper modelMapper;
    private final ImportFileCache importFileCache;
    private final FileRepository fileRepository;
    private LocalDateTime currentTime;
    private LocalDateTime previousTime;

    private boolean valid;

    private final List<Pair<String,String>> equivilentFileTypes;

    @Autowired
    public ImportManager(ImportFileRepository importFileRepository,
                         AssociatedFileDataManager associatedFileDataManager,
                         FileSystemObjectManager fileSystemObjectManager,
                         IgnoreFileRepository ignoreFileRepository,
                         DbLoggingManager dbLoggingManager,
                         ActionManager actionManager,
                         FileSystem fileSystem,
                         ModelMapper modelMapper,
                         FileRepository fileRepository,
                         ImportFileCache importFileCache) {
        super(dbLoggingManager,actionManager,associatedFileDataManager,fileSystemObjectManager,fileSystem);
        this.importFileRepository = importFileRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.modelMapper = modelMapper;
        this.importFileCache = importFileCache;
        this.fileRepository = fileRepository;
        this.valid = false;
        this.equivilentFileTypes = new ArrayList<>();

        // Add the equivalence of file types (make sure these are lowercase.).
        this.equivilentFileTypes.add(Pair.of("mov","mp4"));
    }

    @PostConstruct
    private void init() {
        // Initialise the time.
        this.currentTime = LocalDateTime.now();
        this.previousTime = currentTime;

        File preImportDirectory = null;
        File importDirectory = null;
        File postImportDirectory = null;

        // check directories
        Optional<Source> preImportSource = findSource(FileSystemObjectType.FSO_PRE_IMPORT_SOURCE, this.associatedFileDataManager);
        if (preImportSource.isPresent()) {
            preImportDirectory = new File(preImportSource.get().getPath());

            if(!preImportDirectory.exists()) {
                preImportDirectory = null;
            }
        }

        Optional<Source>  importSource = findSource(FileSystemObjectType.FSO_IMPORT_SOURCE, this.associatedFileDataManager);
        if (importSource.isPresent()) {
            importDirectory = new File(importSource.get().getPath());

            if(!importDirectory.exists()) {
                importDirectory = null;
            }
        }

        Optional<Source> postImportSource = findSource(FileSystemObjectType.FSO_POST_IMPORT_SOURCE, this.associatedFileDataManager);
        if(postImportSource.isPresent()) {
            postImportDirectory = new File(postImportSource.get().getPath());

            if(!postImportDirectory.exists()) {
                postImportDirectory = null;
            }
        }

        // All 3 directories must exist.
        if(preImportDirectory == null || importDirectory == null || postImportDirectory == null) {
            LOG.info("Invalid import directories, import is not valid.");
            return;
        }

        this.valid = true;

        // Read the files currently in the import directories.
        updateCache();
    }

    @PreDestroy
    private void preDestroy() {
    }

    public boolean clearImportData() {
        // Clear the data from the import table.
        try {
            this.importFileRepository.deleteAll(this.importFileRepository.findAll());
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    public boolean clearCacheData() {
        // Clear the data from the import table.
        try {
            this.importFileCache.clear();
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    public static File getPreImportDirectory(AssociatedFileDataManager associatedFileDataManager) {
        Optional<Source> preImportSource = findSource(FileSystemObjectType.FSO_PRE_IMPORT_SOURCE, associatedFileDataManager);
        if (preImportSource.isEmpty()) {
            return null;
        }

        File preImportDirectory = new File(preImportSource.get().getPath());

        if(!preImportDirectory.exists()) {
            return null;
        }

        return preImportDirectory;
    }

    public static File getImportDirectory(AssociatedFileDataManager associatedFileDataManager) {
        Optional<Source> importSource = findSource(FileSystemObjectType.FSO_IMPORT_SOURCE, associatedFileDataManager);
        if (importSource.isEmpty()) {
            return null;
        }

        File importDirectory = new File(importSource.get().getPath());

        if(!importDirectory.exists()) {
            return null;
        }

        return importDirectory;
    }

    public static File getPostImportDirectory(AssociatedFileDataManager associatedFileDataManager) {
        Optional<Source> postImportSource = findSource(FileSystemObjectType.FSO_POST_IMPORT_SOURCE, associatedFileDataManager);
        if (postImportSource.isEmpty()) {
            return null;
        }

        File postImportDirectory = new File(postImportSource.get().getPath());

        if(!postImportDirectory.exists()) {
            return null;
        }

        return postImportDirectory;
    }

    private static List<Source> getSourceIterator(FileSystemObjectType sourceType, AssociatedFileDataManager associatedFileDataManager) {
        List<Source> result = new ArrayList<>();

        switch (sourceType) {
            case FSO_PRE_IMPORT_SOURCE -> result.addAll(associatedFileDataManager.findAllPreImportSource());
            case FSO_IMPORT_SOURCE -> result.addAll(associatedFileDataManager.findAllImportSource());
            case FSO_POST_IMPORT_SOURCE -> result.addAll(associatedFileDataManager.findAllPostImportSource());
        }

        return result;
    }

    private static Optional<Source> findSource(FileSystemObjectType sourceType, AssociatedFileDataManager associatedFileDataManager) {
        Optional<Source> result = Optional.empty();

        int count = 0;
        for(Source nextSource : getSourceIterator(sourceType, associatedFileDataManager)) {
            result = Optional.of(nextSource);

            if(count > 0) {
                LOG.warn("Too many sources specified, do not import.");
                return Optional.empty();
            }

            count++;
        }

        return result;
    }

    private boolean filenamesMatch(String lhs, String rhs) {
        if(lhs.equalsIgnoreCase(rhs)) {
            return true;
        }

        for(Pair<String, String> pair : this.equivilentFileTypes) {
            // Check first replaced by second.
            String tempLhs = lhs.toLowerCase().replace("." + pair.getFirst(), "." + pair.getSecond());
            if(tempLhs.equalsIgnoreCase(rhs)) {
                return true;
            }

            // Check the second replaced by first.
            tempLhs = lhs.toLowerCase().replace("." + pair.getSecond(), "." + pair.getFirst());
            if(tempLhs.equalsIgnoreCase(rhs)) {
                return true;
            }
        }

        return false;
    }

    private PreImportFileDTO getOrCreateCachedData(String nextFilename) {
        String lowerNextFilename = nextFilename.toLowerCase();

        if(this.importFileCache.containsKey(lowerNextFilename)) {
            return this.importFileCache.get(lowerNextFilename);
        }

        // Create a cache entry.
        PreImportFileDTO importFile = new PreImportFileDTO();
        importFile.setFilename(nextFilename);
        for(FileProcessingStepType step : FileProcessingStepType.getStepsInOrder()) {
            importFile.setStepStatus(step, TrafficLightType.TL_UNKNOWN);
        }

        this.importFileCache.put(lowerNextFilename, importFile);
        return importFile;
    }

    private void updateCache() {
        // Only perform this operation if the manager is valid.
        if(!this.valid) {
            return;
        }

        // Get a list of files from the 3 directories and the database.
        Set<String> preImportFiles = this.fileSystem.listFilesInDirectory(Objects.requireNonNull(getPreImportDirectory(this.associatedFileDataManager),"Pre Import Directory cannot be null."));
        Set<String> importFiles = this.fileSystem.listFilesInDirectory(Objects.requireNonNull(getImportDirectory(this.associatedFileDataManager), "Import Directory cannot be null."));
        Set<String> postImportFiles = this.fileSystem.listFilesInDirectory(Objects.requireNonNull(getPostImportDirectory(this.associatedFileDataManager), "Post Import Directory cannot be null."));
        Map<String,ImportFile> importFilesDb = new HashMap<>();
        for(ImportFile next: this.importFileRepository.findAll()) {
            // If the name is null, delete the record.
            if(next.getName() == null) {
                this.importFileRepository.delete(next);
            } else {
                importFilesDb.put(next.getName().toLowerCase(), next);
            }
        }

        // The id of the file is the name, however some files get a different name in the import and post
        // import directories.

        // There should be one row for each file in the pre-import directory.
        for(String nextPreImport : preImportFiles) {
            // Get cached data.
            PreImportFileDTO importFile = getOrCreateCachedData(nextPreImport);

            // Is this in the import directory?
            for(String nextImport : importFiles) {
                if(filenamesMatch(nextPreImport, nextImport)) {
                    importFile.setInImport(true);
                    break;
                }
            }

            // Is this in the import directory?
            for(String postImportFIle : postImportFiles) {
                if(filenamesMatch(nextPreImport, postImportFIle)) {
                    importFile.setInPostImport(true);
                    break;
                }
            }

            // Is this in the database?
            if(importFilesDb.containsKey(nextPreImport.toLowerCase())) {
                // Indicate that the file is in the database and copy the information.
                importFile.setInDatabase(true);

                // Transfer the data from the database.
                ImportFile dbFile = importFilesDb.get(nextPreImport.toLowerCase());

                importFile.setDestination(dbFile.getDestination());
                importFile.setId(dbFile.getIdAndType().getId());
                importFile.setDate(dbFile.getDate());
                importFile.setSize(dbFile.getSize());
                importFile.setMd5(new MD5(dbFile.getMD5()));
                importFile.setDuration(dbFile.getDuration());
                importFile.setImage(dbFile.getImage());
                if(dbFile.getImageHeight() != null && dbFile.getImageWidth() != null) {
                    importFile.setImageSize(new ImageSize(dbFile.getImageWidth(), dbFile.getImageHeight()));
                } else {
                    importFile.setImageSize(null);
                }
                importFile.setImportMd5(dbFile.getImportMd5());
                importFile.setImportName(dbFile.getImportName());
                importFile.setImportDate(dbFile.getImportDate());
                importFile.setImportSize(dbFile.getImportSize());
                if(dbFile.getLatitude() != null && dbFile.getLongitude() != null) {
                    importFile.setLocation(new LatLong(dbFile.getLatitude(),dbFile.getLongitude()));
                } else {
                    importFile.setLocation(null);
                }
                importFile.setProcessed(dbFile.getProcessed());
                importFile.setVideo(dbFile.getVideo());
            }
        }

        // Error states:
        //  (1) a file that is in the post import directory that is not in the pre-import directory.
        for(String nextPostImport : postImportFiles) {
            // Is this file in the pre-import directory?
            for(String  nextPreImport : preImportFiles) {
                if(filenamesMatch(nextPreImport, nextPostImport)) {
                    break;
                }

                // This is a problem.
                PreImportFileDTO importFileError = getOrCreateCachedData(nextPreImport);
                importFileError.setErrorInPostImport(true);
            }
        }

        //  (2) a file that is in the import directory that is not in the pre-import directory.
        for(String nextPostImport : postImportFiles) {
            // Is this file in the pre-import directory?
            for(String  nextPreImport : preImportFiles) {
                if(filenamesMatch(nextPreImport, nextPostImport)) {
                    break;
                }

                // This is a problem.
                PreImportFileDTO importFileError = getOrCreateCachedData(nextPreImport);
                importFileError.setErrorInImport(true);
            }
        }

        // Delete anything from the database that is not in the import directory.
        for(Map.Entry<String,ImportFile> next: importFilesDb.entrySet()) {
            boolean dbOK = false;
            for(String nextPreImport : preImportFiles) {
                if(next.getKey().equalsIgnoreCase(nextPreImport)) {
                    dbOK = true;
                    break;
                }
            }

            if(!dbOK) {
                // Delete the entry.
                this.importFileRepository.delete(next.getValue());
            }
        }
    }

    @Deprecated
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

    @Deprecated
    private boolean processClassification(ImportFile importFile, Path path, ImportDataDTO result) {
        if((importFile.getClassification() == null) || !importFile.getClassification().getAction().equals(ClassificationActionType.CA_BACKUP)) {
            result.increment(ImportDataDTO.ImportDataCountType.NON_BACKUP_CLASSIFICATIONS);
            LOG.info("{} not a backed up file, deleting", path);
            fileSystem.deleteFile(path.toFile(),result,importFile.getIdAndType().getId());
            return false;
        }

        return true;
    }

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

    @Deprecated
    private boolean md5StillMissing(Path path, FileInfo fileInfo, Classification classification) {
        fileInfo.setMD5(fileSystem.getClassifiedFileMD5(path,classification,fileInfo.getIdAndType().getId()));

        if(fileInfo.getMD5().isSet()) {
            fileSystemObjectManager.save(fileInfo);
            return false;
        }

        return true;
    }

    @Deprecated
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

    @Deprecated
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

    @Deprecated
    public List<ImportProcessDTO> convertImportFiles() {
        List<ImportProcessDTO> result = new ArrayList<>();
        ImportProcessDTO resultCount = new ImportProcessDTO();
        result.add(resultCount);

        try {
            // Find the pre-import details.
            // TODO
            Optional<PreImportSource> preImportSource = Optional.empty();// findPreImportSource();
            if(preImportSource.isEmpty()) {
                resultCount.setProblems();
                LOG.warn("Invalid Pre Import Source - skipping import.");
                return result;
            }

            // Find the import details
            // TODO
            Optional<ImportSource> importSource = Optional.empty(); //findImportSource();
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
        } catch (Exception e) {
            resultCount.setProblems();
            LOG.error("Problems",e);
        }

        LOG.info("Convert import files is complete.");

        return result;
    }

    @Deprecated
    public List<GatherDataDTO> importPhoto() throws ImportRequestException, IOException {
        List<GatherDataDTO> result = new ArrayList<>();

        // Find the import source
        //TODO
        Optional<ImportSource> importSource = Optional.empty();//findImportSource();
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

    @Deprecated
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

    @Deprecated
    private String removeFileExtension(String filename) {
        return filename.replaceFirst("[.][^.]+$","");
    }

    @Deprecated
    private boolean similarFileName(String lhs, String rhs) {
        return removeFileExtension(lhs).equalsIgnoreCase(removeFileExtension(rhs));
    }

    @Deprecated
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

    @Deprecated
    private void addSimilarFileData(List<ImportFileDTO> files) {
        try {
            //TODO
            Optional<ImportSource> importSource = Optional.empty();// findImportSource();
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

    @Deprecated
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

    @Deprecated
    public ImportFileDTO externalFindImportFile(Integer id) throws InvalidFileIdException {
        for(ImportFileDTO nextImportFile : externalFindImportFiles()) {
            if(nextImportFile.getId().equals(id)) {
                return nextImportFile;
            }
        }

        throw new InvalidFileIdException(id);
    }

    public List<PreImportFileDTO> getUpdates() {
        this.previousTime = this.currentTime.minusSeconds(1);
        this.currentTime = LocalDateTime.now();
        List<PreImportFileDTO> result = new ArrayList<>();

        // Return the list of files that have been updated.
        for(String filename: this.importFileCache.getFiles()){
            PreImportFileDTO next = this.importFileCache.get(filename.toLowerCase());

            if(next.updatedSince(this.previousTime)) {
                result.add(next);
            }
        }

        return result;
    }

    public PreImportFileDTO getImportFile(String name) {
        return this.importFileCache.get(name.toLowerCase());
    }

    public List<PreImportFileDTO> getImportFiles(int limit) {
        updateCache();

        // If the limit is zero, return all the files.
        if(limit == 0) {
            limit = this.importFileCache.getFiles().size();
        }

        // Get data from the pre-import directory.
        List<PreImportFileDTO> result = new ArrayList<>();

        // Get the top 'limit' files from the cache.
        for(String next: this.importFileCache.getFiles()) {
            PreImportFileDTO nextFile = this.importFileCache.get(next.toLowerCase());

            if(nextFile != null) {
                result.add(nextFile);
            }

            if(result.size() >= limit) {
                break;
            }
        }

        // Return the result.
        this.currentTime = LocalDateTime.now();
        return result;
    }

    @Deprecated
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

    @Deprecated
    public List<FileInfo> getSimilarIgnore(String filename, String md5) {
        // Return ignore files that match either the name or the MD5.
        List<FileInfo> result = new ArrayList<>(ignoreFileRepository.findByName(filename));

        if(md5 != null && !md5.isEmpty()) {
            result.addAll(ignoreFileRepository.findByMd5(md5));
        }

        return result;
    }

    @Deprecated
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

    @Deprecated
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

    @Deprecated
    public List<FileInfo> getImport(String filename) {
        // Return ignore files that match either the name or the MD5.
        List<FileInfo> result = new ArrayList<>();
        for(FileInfo next: importFileRepository.findByName(filename)) {
            result.add(next);
        }

        return result;
    }

    public boolean unIgnoreSelectedFile(String filename) {
        // This file must be in the cache for this action to be performed.
        if(importFileCache.containsKey(filename.toLowerCase())) {
            PreImportFileDTO file = importFileCache.get(filename.toLowerCase());

            // Cannot un-ignore a file unless all data is known.
            if(file.getMd5() == null || file.getMd5().isEmpty() || file.getSize() == null || file.getDate() == null) {
                LOG.info("{} Cannot remove from ignore table because md5, size and/or date is missing.", filename);
                return false;
            }

            // This must already be in the table
            for(IgnoreFile next: ignoreFileRepository.findByMd5(file.getMd5())) {
                if(next.getDate().equals(file.getDate()) &&
                        next.getSize().equals(file.getSize()) &&
                        next.getName().equals(filename)) {
                    // Delete this record.
                    LOG.info("{} has been removed from the ignore table.", filename);
                    ignoreFileRepository.delete(next);
                    return true;
                }
            }

            LOG.info("{} was not ignored so nothing has changed.", filename);
            return false;
        }

        LOG.info("Failed to un-ignore {} as its not in the cache.", filename);
        return false;
    }

    public boolean ignoreSelectedFile(String filename) {
        // This file must be in the cache for this action to be performed.
        if(importFileCache.containsKey(filename.toLowerCase())) {
            PreImportFileDTO file = importFileCache.get(filename.toLowerCase());

            // Insert the details of this file into the ignore table - we must have an MD5 to do this.
            if(file.getMd5() == null || file.getMd5().isEmpty() || file.getSize() == null || file.getDate() == null) {
                LOG.info("{} Cannot ignore this file because md5, size and/or date is missing.", filename);
                return false;
            }

            // Make sure this file is not ignored already
            for(IgnoreFile next: ignoreFileRepository.findByMd5(file.getMd5())) {
                if(next.getDate().equals(file.getDate()) &&
                    next.getSize().equals(file.getSize())) {
                    // Its already ignored.
                    LOG.info("{} this file is already ignored.", filename);
                    return false;
                }
            }

            // Insert the details into the ignore table.
            IgnoreFile ignoreFile = new IgnoreFile();
            ignoreFile.setName(filename);
            ignoreFile.setDate(file.getDate());
            ignoreFile.setSize(file.getSize());
            ignoreFile.setMD5(new MD5(file.getMd5()));

            LOG.info("{} has been inserted into the ignore table.", filename);
            ignoreFileRepository.save(ignoreFile);
            return true;
        }

        LOG.info("Failed to ignore {} as its not in the cache.", filename);
        return false;
    }

    public boolean recipeFile(String filename) {
        // This file must be in the cache for this action to be performed.
        if(importFileCache.containsKey(filename.toLowerCase())) {
            PreImportFileDTO file = importFileCache.get(filename.toLowerCase());

            // Has this already been marked as a recipe?
            if(file.getDestination().equalsIgnoreCase(RECIPE_FILE_DESTINATION)) {
                return true;
            }

            // Set the destination of the file and store in the database.
            for(FileInfo next: this.importFileRepository.findByName(filename)) {
                if(next instanceof ImportFile importFile) {
                    importFile.setDestination(RECIPE_FILE_DESTINATION);

                    importFileRepository.save(importFile);
                }
            }

            file.setDestination(RECIPE_FILE_DESTINATION);
            return true;
        }

        LOG.info("Failed to mark {} as a recipe as its not in the cache.", filename);
        return false;
    }

    @Deprecated
    public boolean removeDuplicates() {
        LOG.info("Remove any files in the import directory that are duplicates of files already in the system.");

        // Get the file that needs to be re-imported.
        // TODO
        Optional<PreImportSource> preImportSource = Optional.empty();// findPreImportSource();
        if(preImportSource.isEmpty()) {
            LOG.warn("Remove Duplicates: Invalid Pre Import Source for delete, returning empty list.");
            return false;
        }

        File source = new File(preImportSource.get().getPath());

        // Check that the source exists.
        if(!fileSystem.directoryExists(source.toPath())) {
            LOG.warn("Remove duplicates: Pre import does not exist, returning empty list.");
            return false;
        }

        List<String> removes = new ArrayList<>();
        // TODO
//        for(String nextFilename : fileSystem.listFilesInDirectory(preImportSource.get().getPath())) {
        for(String nextFilename : fileSystem.listFilesInDirectory(new File("xyz"))) {
            // This depends on the file having been imported (mov files are imported as mp4).
            List<FileInfo> imported = getImport(nextFilename.toLowerCase().replace(".mov",".mp4"));

            // Is this file in the ignored list?
            for(FileInfo nextImported : imported) {
                for(ImportFileBaseDTO nextSimilar : getSimilarImported(nextImported.getName(),nextImported.getMD5().toString())) {
                    // Does this file match on name, size, date and MD5?
                    if(!nextImported.getMD5().toString().equalsIgnoreCase(nextSimilar.getMd5())) {
                        continue;
                    }

                    if(!nextImported.getSize().equals(nextSimilar.getSize())) {
                        continue;
                    }

                    if(!nextImported.getDate().equals(nextSimilar.getDate())) {
                        continue;
                    }

                    if(!nextSimilar.getFilename().toLowerCase().endsWith(nextImported.getName().toLowerCase())) {
                        continue;
                    }

                    LOG.info("Will remove {}", nextFilename);
                    removes.add(nextFilename);
                }
            }
        }

        // Process the removes.
        removes.forEach(this::deletePreImportFile);

        return true;
    }

    @Deprecated
    public boolean removeIgnored() {
        LOG.info("Remove any files in the import directory that are ignored.");

        // Get the file that needs to be re-imported.
        // TODO
        Optional<PreImportSource> preImportSource = Optional.empty();// findPreImportSource();
        if(preImportSource.isEmpty()) {
            LOG.warn("Remove ignored: Invalid Pre Import Source for delete, returning empty list.");
            return false;
        }

        File source = new File(preImportSource.get().getPath());

        // Check that the source exists.
        if(!fileSystem.directoryExists(source.toPath())) {
            LOG.warn("Remove ignored: Pre import does not exist, returning empty list.");
            return false;
        }

        List<String> removes = new ArrayList<>();
        // TODO
//        for(String nextFilename : fileSystem.listFilesInDirectory(preImportSource.get().getPath())) {
        for(String nextFilename : fileSystem.listFilesInDirectory(new File("xyz"))) {
            // Is this file in the ignored list?
            for(FileInfo nextFile: getSimilarIgnore(nextFilename,null)) {
                // Need to validate that all the details are the same - date, size & MD5.
                File realWorldFile = new File(source.getPath(),nextFilename);

                // Check the size.
                if(nextFile.getSize() != realWorldFile.length()) {
                    LOG.info("{} different size {} {}", nextFilename, nextFile.getSize(), realWorldFile.length());
                    continue;
                }

                // Check the date.
                if(!nextFile.getDate().equals(FileProcessor.getFileLastModified(realWorldFile))) {
                    LOG.info("{} different date {} {}", nextFilename, nextFile.getDate(), realWorldFile.lastModified());
                    continue;
                }

                // Check the MD5
                Classification dummyClassification = new Classification();
                dummyClassification.setUseMD5(true);
                MD5 md5 = fileSystem.getClassifiedFileMD5(realWorldFile.toPath(), dummyClassification, 0);

                if(!nextFile.getMD5().equals(md5)) {
                    LOG.info("{} different MD5 {} {}", nextFilename, nextFile.getMD5(), md5);
                    continue;
                }

                LOG.info("Will remove {}", nextFilename);
                removes.add(nextFilename);
            }
        }

        // Process the removes.
        removes.forEach(this::deletePreImportFile);

        return true;
    }

    @Deprecated
    public boolean deletePreImportFile(String filename) {
        // Get the actual files that are in the pre-import directory.
        //TODO
        Optional<PreImportSource> preImportSource = Optional.empty();// findPreImportSource();
        if(preImportSource.isEmpty()) {
            LOG.warn("Invalid Pre Import Source for delete, returning empty list.");
            return false;
        }
        File preImportFile = new File(preImportSource.get().getPath().trim(), filename);

        //TODO
        Optional<ImportSource> importSource = Optional.empty();// findImportSource();
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
        this.importFileCache.remove(filename.toLowerCase());

        return true;
    }

    public byte[] getFileContent(String name) {
        try {
            if(!this.valid) {
                throw new IllegalStateException("The Import manager is in an invalid state.");
            }

            // Get the cached data of the file.
            PreImportFileDTO importFile = this.importFileCache.get(name);

            // Get the name of the import file.
            if(importFile == null) {
                throw new IllegalStateException("getFileContent - file not in cache.");
            }

            if(importFile.getImportName() == null || importFile.getImportName().trim().isEmpty()) {
                throw new IllegalStateException("getFileContent - import name not set.");
            }

            // read the specified file.
            LOG.info("Read file from {}", getImportDirectory(this.associatedFileDataManager));
            File source = new File(Objects.requireNonNull(getImportDirectory(this.associatedFileDataManager)).getPath(), importFile.getImportName());

            return fileSystem.readAllBytes(source);
        } catch (IOException e) {
            LOG.warn("Read files from {} failed", name, e);
        }

        return null;
    }

    public ImportFileSummaryDTO getImportSummary() {
        // Generate the summary.
        ImportFileSummaryDTO result = new ImportFileSummaryDTO();

        // Get a summary of the files.
        for(String nextName : importFileCache.getFiles()) {
            PreImportFileDTO importFile = this.importFileCache.get(nextName);

            // Update the counts.
            result.incrementTotalPreImportFiles();

            if(importFile.isInImport()) {
                result.incrementTotalImportFiles();
            }

            if(importFile.isInPostImport()) {
                result.incrementTotalPostImportFiles();
            }

            for(FileProcessingStepType nextStep : FileProcessingStepType.values()) {
                TrafficLightType status = importFile.getStepStatus(nextStep);

                result.incrementStepCount(nextStep, status);
            }
        }

        return result;
    }
}
