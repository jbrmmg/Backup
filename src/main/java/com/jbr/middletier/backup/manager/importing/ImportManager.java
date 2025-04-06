package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.manager.*;
import com.jbr.middletier.backup.util.ImageSize;
import com.jbr.middletier.backup.util.LatLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import com.jbr.middletier.backup.manager.FileProcessor;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class ImportManager extends FileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ImportManager.class);

    public static final String RECIPE_FILE_DESTINATION = "[** recipe **]";
    private final ImportFileRepository importFileRepository;
    private final IgnoreFileRepository ignoreFileRepository;
    private final ImportFileCache importFileCache;
    private final ImportSourceManager importSourceManager;
    private LocalDateTime currentTime;
    private LocalDateTime previousTime;

    private boolean valid;

    private final List<Pair<String,String>> equivalentFileTypes;

    @Autowired
    public ImportManager(ImportFileRepository importFileRepository,
                         AssociatedFileDataManager associatedFileDataManager,
                         FileSystemObjectManager fileSystemObjectManager,
                         IgnoreFileRepository ignoreFileRepository,
                         DbLoggingManager dbLoggingManager,
                         ActionManager actionManager,
                         FileSystem fileSystem,
                         ImportFileCache importFileCache,
                         ImportSourceManager importSourceManager) {
        super(dbLoggingManager,actionManager,associatedFileDataManager,fileSystemObjectManager,fileSystem);
        this.importFileRepository = importFileRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.importFileCache = importFileCache;
        this.importSourceManager = importSourceManager;
        this.valid = false;
        this.equivalentFileTypes = new ArrayList<>();

        // Add the equivalence of file types (make sure these are lowercase.).
        this.equivalentFileTypes.add(Pair.of("mov","mp4"));
    }

    @PostConstruct
    private void init() {
        try {
            // Initialise the time.
            this.currentTime = LocalDateTime.now();
            this.previousTime = currentTime;

            File preImportDirectory = null;
            File importDirectory = null;
            File postImportDirectory = null;

            // check directories
            Optional<Source> preImportSource = this.importSourceManager.findSource(FileSystemObjectType.FSO_PRE_IMPORT_SOURCE);
            if (preImportSource.isPresent()) {
                preImportDirectory = new File(preImportSource.get().getPath());

                if (!preImportDirectory.exists()) {
                    preImportDirectory = null;
                }
            }

            Optional<Source> importSource = this.importSourceManager.findSource(FileSystemObjectType.FSO_IMPORT_SOURCE);
            if (importSource.isPresent()) {
                importDirectory = new File(importSource.get().getPath());

                if (!importDirectory.exists()) {
                    importDirectory = null;
                }
            }

            Optional<Source> postImportSource = this.importSourceManager.findSource(FileSystemObjectType.FSO_POST_IMPORT_SOURCE);
            if (postImportSource.isPresent()) {
                postImportDirectory = new File(postImportSource.get().getPath());

                if (!postImportDirectory.exists()) {
                    postImportDirectory = null;
                }
            }

            // All 3 directories must exist.
            if (preImportDirectory == null || importDirectory == null || postImportDirectory == null) {
                LOG.info("Invalid import directories, import is not valid.");
                return;
            }

            this.valid = true;

            // Read the files currently in the import directories.
            updateCache();
        } catch (Exception e) {
            this.valid = false;
            LOG.info("Error while initializing ImportManager, imports will be disabled.", e);
        }
    }

    public boolean clearImportData() {
        // Clear the data from the import table.
        try {
            this.importFileRepository.deleteAll(this.importFileRepository.findAll());
            return true;
        } catch (Exception e) {
            // Exceptions are ignored, but logged.
            LOG.warn("Failed to clear import data",e);
        }

        return false;
    }

    public boolean clearCacheData() {
        // Clear the data from the import table.
        try {
            this.importFileCache.clear();
            return true;
        } catch (Exception e) {
            // Exceptions are ignored, but logged.
            LOG.warn("Failed to clear cache",e);
        }

        return false;
    }

    private boolean filenamesMatch(String lhs, String rhs) {
        if(lhs.equalsIgnoreCase(rhs)) {
            return true;
        }

        for(Pair<String, String> pair : this.equivalentFileTypes) {
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

    private PreImportFileDTO getOrCreateCachedData(String nextFilename, boolean resetStatus, Map<String,ImportFile> importFilesDb) {
        String lowerNextFilename = nextFilename.toLowerCase();

        if(this.importFileCache.containsKey(lowerNextFilename)) {
            PreImportFileDTO result =  this.importFileCache.get(lowerNextFilename);
            if(resetStatus) {
                result.setStatus(ImportFileStatusType.IFS_READ);
            }
            return result;
        }

        // Create a cache entry.
        PreImportFileDTO importFile = new PreImportFileDTO();
        importFile.setFilename(nextFilename);
        importFile.setStatus(ImportFileStatusType.IFS_READ);
        for(FileProcessingStepType step : FileProcessingStepType.getStepsInOrder()) {
            importFile.setStepStatus(step, TrafficLightType.TL_UNKNOWN);
        }

        // If the database information is provided and if this file is in it then initialise the values.
        if(importFilesDb != null && importFilesDb.containsKey(nextFilename.toLowerCase())) {
            // Indicate that the file is in the database and copy the information.
            importFile.setInDatabase(true);

            // Transfer the data from the database.
            ImportFile dbFile = importFilesDb.get(nextFilename.toLowerCase());

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

        this.importFileCache.put(lowerNextFilename, importFile);
        return importFile;
    }

    private Map<String,ImportFile> getImportFilesDb() {
        Map<String,ImportFile> result = new HashMap<>();

        for(ImportFile next: this.importFileRepository.findAll()) {
            // If the name is null, delete the record.
            if(next.getName() == null) {
                this.importFileRepository.delete(next);
            } else {
                result.put(next.getName().toLowerCase(), next);
            }
        }

        return result;
    }

    private void setDirectoryFlags(Set<String> preImportFiles,
                                   Set<String> importFiles,
                                   Set<String> postImportFiles,
                                   Map<String,ImportFile> importFilesDb) {
        for(String nextPreImport : preImportFiles) {
            // Get cached data.
            PreImportFileDTO importFile = getOrCreateCachedData(nextPreImport,true, importFilesDb);

            // Is this in the import directory?
            for(String nextImport : importFiles) {
                if(filenamesMatch(nextPreImport, nextImport)) {
                    importFile.setInImport(true);
                    break;
                }
            }

            // Is this in the import directory?
            for(String postImportFile : postImportFiles) {
                if(filenamesMatch(nextPreImport, postImportFile)) {
                    importFile.setInPostImport(true);
                    break;
                }
            }
        }
    }

    public void checkExtraImport(Set<String> preImportFiles, Set<String> importFiles, boolean postImport) {
        for(String nextPostImport : importFiles) {
            // Is this file in the pre-import directory?
            for(String  nextPreImport : preImportFiles) {
                if(filenamesMatch(nextPreImport, nextPostImport)) {
                    break;
                }

                // This is a problem.
                PreImportFileDTO importFileError = getOrCreateCachedData(nextPreImport,true, null);
                if(postImport) {
                    importFileError.setErrorInPostImport(true);
                } else {
                    importFileError.setErrorInImport(true);
                }
            }
        }
    }

    private void cleanUpDatabase(Set<String> preImportFiles, Map<String,ImportFile> importFilesDb) {
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

    private void cleanupCache() {
        List<String> remove = new ArrayList<>();
        for(String filename : this.importFileCache.getFiles()) {
            PreImportFileDTO importFile = getOrCreateCachedData(filename,false,null);

            if(importFile.getStatus().equalsIgnoreCase("removed")) {
                remove.add(filename);
            }
        }

        // Remove those marked as removed.
        for(String nextRemove: remove) {
            this.importFileCache.remove(nextRemove);
        }
    }

    private void updateCache() {
        // Only perform this operation if the manager is valid.
        if(!this.valid) {
            return;
        }

        // Get a list of files from the 3 directories and the database.
        Set<String> preImportFiles = this.fileSystem.listFilesInDirectory(Objects.requireNonNull(this.importSourceManager.getPreImportDirectory(),"Pre Import Directory cannot be null."));
        Set<String> importFiles = this.fileSystem.listFilesInDirectory(Objects.requireNonNull(this.importSourceManager.getImportDirectory(), "Import Directory cannot be null."));
        Set<String> postImportFiles = this.fileSystem.listFilesInDirectory(Objects.requireNonNull(this.importSourceManager.getPostImportDirectory(), "Post Import Directory cannot be null."));
        Map<String,ImportFile> importFilesDb = getImportFilesDb();

        // The id of the file is the name, however some files get a different name in the import and post
        // import directories.

        // There should be one row for each file in the pre-import directory.
        setDirectoryFlags(preImportFiles, importFiles, postImportFiles, importFilesDb);

        // Error states:
        //  (1) a file that is in the post import directory that is not in the pre-import directory.
        checkExtraImport(preImportFiles, importFiles,false);

        //  (2) a file that is in the import directory that is not in the pre-import directory.
        checkExtraImport(preImportFiles, postImportFiles,true);

        // Delete anything from the database that is not in the import directory.
        cleanUpDatabase(preImportFiles, importFilesDb);

        // If there is anything in the cache that is marked as removed, then remove it.
        cleanupCache();
    }

    @Override
    public FileInfo createNewFile() {
        ImportFile newFile = new ImportFile();
        newFile.setStatus(ImportFileStatusType.IFS_READ);
        return newFile;
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

    private boolean matchFilter(PreImportFileDTO file, FileProcessingStepType step, TrafficLightType status) {
        // Must have all values to be filtered.
        if(file == null || step == null || status == null) {
            return true;
        }

        // Does the file step match the step status.
        return file.getStepStatus(step).equals(status);
    }

    public List<PreImportFileDTO> getImportFiles(Integer limit, Integer page, String stepName, String statusName) {
        updateCache();

        // If the limit is zero, return all the files.
        if(limit == null || limit == 0) {
            limit = this.importFileCache.getFiles().size();
        }

        // If a page number is specified then skip the first few files.
        int skip = 0;
        if(page != null) {
            skip = page * limit;
        }

        // Translate the step & status into their respective enums.
        FileProcessingStepType step = null;
        if(stepName != null && !stepName.isEmpty()) {
            step = FileProcessingStepType.getFromName(stepName);
        }

        TrafficLightType status = null;
        if(statusName != null && !statusName.isEmpty()) {
            status = TrafficLightType.getFromName(statusName);
        }

        // Get data from the pre-import directory.
        List<PreImportFileDTO> result = new ArrayList<>();

        // Get the top 'limit' files from the cache.
        for(String next: this.importFileCache.getFiles()) {
            PreImportFileDTO nextFile = this.importFileCache.get(next.toLowerCase());

            if(skip == 0  && matchFilter(nextFile, step, status)) {
                result.add(nextFile);
            }

            if(result.size() >= limit) {
                break;
            }

            if(skip > 0) {
                skip--;
            }
        }

        // Return the result.
        this.currentTime = LocalDateTime.now();
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

                    // File ignore status will need to be re-evaluated.
                    file.setStepStatus(FileProcessingStepType.FPS_CHECK_FILE_IGNORED, TrafficLightType.TL_UNKNOWN);
                    importFileCache.queueForUpdates(file);
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

            // File ignore status will need to be re-evaluated.
            file.setStepStatus(FileProcessingStepType.FPS_CHECK_FILE_IGNORED, TrafficLightType.TL_UNKNOWN);
            importFileCache.queueForUpdates(file);
            return true;
        }

        LOG.info("Failed to ignore {} as its not in the cache.", filename);
        return false;
    }

    public boolean updateDestination(DestinationUpdateDTO destinationUpdate) {
        if(destinationUpdate.getFilename() == null ||
                destinationUpdate.getFilename().isEmpty() ||
                destinationUpdate.getDestination() == null ||
                destinationUpdate.getDestination().isEmpty()) {
            return false;
        }

        // This file must be in the cache for this action to be performed.
        if(importFileCache.containsKey(destinationUpdate.getFilename().toLowerCase())) {
            PreImportFileDTO file = importFileCache.get(destinationUpdate.getFilename().toLowerCase());

            // Check that not in the post import already
            if(file.isInPostImport()) {
                return false;
            }

            // Set the destination of the file and store in the database.
            for(FileInfo next: this.importFileRepository.findByName(destinationUpdate.getFilename())) {
                if(next instanceof ImportFile importFile) {
                    importFile.setDestination(destinationUpdate.getDestination());

                    importFileRepository.save(importFile);
                }
            }

            file.setDestination(destinationUpdate.getDestination());
            return true;
        }

        LOG.info("Failed to update destination on {} its not in the cache.", destinationUpdate.getFilename());
        return false;
    }

    public boolean recipeFile(String filename) {
        // This file must be in the cache for this action to be performed.
        if(importFileCache.containsKey(filename.toLowerCase())) {
            PreImportFileDTO file = importFileCache.get(filename.toLowerCase());

            // Has this already been marked as a recipe?
            if(file.getDestination() != null && file.getDestination().equalsIgnoreCase(RECIPE_FILE_DESTINATION)) {
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

    public boolean deleteConfirmedImports() {
        LOG.info("Remove any files in the import directory have already been imported.");

        // Any file in the cache that has a confirmed imported status of GREEN.
        for(String nextFile: this.importFileCache.getFiles()) {
            // Get the file.
            PreImportFileDTO file = importFileCache.get(nextFile);

            // Is this a confirmed import?
            TrafficLightType status = file.getStepStatus(FileProcessingStepType.FPS_CHECK_FILE_CONFIRMED_IMPORTED);
            if(status == TrafficLightType.TL_GREEN) {
                // Setup this file to be processed.
                file.setStatus(ImportFileStatusType.IFS_REMOVE_IMPORTED);
                file.setStepStatus(FileProcessingStepType.FPS_PROCESS_IMPORT, TrafficLightType.TL_UNKNOWN);
                importFileCache.queueForUpdates(file);
            }
        }

        return true;
    }

    public boolean importPhotos() {
        LOG.info("Process the photos that have been updated with a destination.");

        // Any file in the cache that has a confirmed imported status of GREEN.
        for(String nextFile: this.importFileCache.getFiles()) {
            // Get the file.
            PreImportFileDTO file = importFileCache.get(nextFile);

            // Is this imported and does it have a destination?
            TrafficLightType status = file.getStepStatus(FileProcessingStepType.FPS_CHECK_FILE_CONFIRMED_IMPORTED);
            if(status == TrafficLightType.TL_RED && file.getDestination() != null && !file.getDestination().isEmpty() && !file.isInPostImport()) {
                // Setup this file to be processed.
                file.setStatus(ImportFileStatusType.IFS_IMPORT_FILE);
                file.setStepStatus(FileProcessingStepType.FPS_PROCESS_IMPORT, TrafficLightType.TL_UNKNOWN);
                importFileCache.queueForUpdates(file);
            }
        }

        return true;
    }

    public boolean deleteActivePhotos() {
        LOG.info("Remove any files in the import directory that are Apple active photos.");

        // Any file in the cache that has a confirmed imported status of GREEN.
        for(String nextFile: this.importFileCache.getFiles()) {
            // Get the file.
            PreImportFileDTO file = importFileCache.get(nextFile);

            // Is this an ignored file?
            TrafficLightType status = file.getStepStatus(FileProcessingStepType.FPS_CHECK_ACTIVE_PHOTO_FILE);
            if(status == TrafficLightType.TL_RED) {
                // Setup this file to be processed.
                file.setStatus(ImportFileStatusType.IFS_REMOVE_ACTIVE_PHOTO);
                file.setStepStatus(FileProcessingStepType.FPS_PROCESS_IMPORT, TrafficLightType.TL_UNKNOWN);
                importFileCache.queueForUpdates(file);
            }
        }

        return true;
    }

    public boolean deleteImportFile(String filename) {
        LOG.info("Delete the import file {}", filename);

        // Get the file.
        PreImportFileDTO file = importFileCache.get(filename);

        if(file != null) {
            // Setup this file to be processed.
            file.setStatus(ImportFileStatusType.IFS_MANUAL_DELETE);
            file.setStepStatus(FileProcessingStepType.FPS_PROCESS_IMPORT, TrafficLightType.TL_UNKNOWN);
            importFileCache.queueForUpdates(file);
            return true;
        }

        return false;
    }

    public boolean deleteIgnored() {
        LOG.info("Remove any files in the import directory that are ignored.");

        // Any file in the cache that has a confirmed imported status of GREEN.
        for(String nextFile: this.importFileCache.getFiles()) {
            // Get the file.
            PreImportFileDTO file = importFileCache.get(nextFile);

            // Is this an ignored file?
            TrafficLightType status = file.getStepStatus(FileProcessingStepType.FPS_CHECK_FILE_IGNORED);
            if(status == TrafficLightType.TL_RED) {
                // Setup this file to be processed.
                file.setStatus(ImportFileStatusType.IFS_REMOVE_IGNORED);
                file.setStepStatus(FileProcessingStepType.FPS_PROCESS_IMPORT, TrafficLightType.TL_UNKNOWN);
                importFileCache.queueForUpdates(file);
            }
        }

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
            LOG.info("Read file from {}", this.importSourceManager.getImportDirectory());
            File source = new File(Objects.requireNonNull(this.importSourceManager.getImportDirectory()).getPath(), importFile.getImportName());

            return fileSystem.readAllBytes(source);
        } catch (IOException e) {
            LOG.warn("Read files from {} failed", name, e);
        }

        // Return empty array.
        return new byte[0];
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
        result.setQueued(this.importFileCache.inQueue());

        return result;
    }
}
