package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import com.jbr.middletier.backup.exception.FileMetaDataMissingException;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.exception.InvalidMediaTypeException;
import com.jbr.middletier.backup.manager.*;
import com.jbr.middletier.backup.schedule.SummaryCtrl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import static java.util.Comparator.comparing;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Files", description = "File system browsing, search, media serving, and sync operations")
public class FileController {
    private static final Logger LOG = LoggerFactory.getLogger(FileController.class);

    private final DriveManager driveManager;
    private final AssociatedFileDataManager associatedFileDataManager;
    private final ActionManager actionManager;
    private final DuplicateManager duplicateManager;
    private final SynchronizeManager synchronizeManager;
    private final FileSystemObjectManager fileSystemObjectManager;
    private final FileSystem fileSystem;
    private final SummaryCtrl summaryCtrl;

    @Contract(pure = true)
    @Autowired
    public FileController(DriveManager driverManager,
                          AssociatedFileDataManager associatedFileDataManager,
                          ActionManager actionManager,
                          DuplicateManager duplicateManager,
                          SynchronizeManager synchronizeManager,
                          FileSystemObjectManager fileSystemObjectManager,
                          FileSystem fileSystem,
                          SummaryCtrl summaryCtrl) {
        this.driveManager = driverManager;
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.associatedFileDataManager = associatedFileDataManager;
        this.actionManager = actionManager;
        this.duplicateManager = duplicateManager;
        this.synchronizeManager = synchronizeManager;
        this.fileSystem = fileSystem;
        this.summaryCtrl = summaryCtrl;
    }

    @GetMapping(path="/files")
    public List<FileInfoDTO> getFiles() {
        List<FileInfoDTO> result = new ArrayList<>();
        for(FileSystemObject nextFile: fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_FILE)) {
            result.add(fileSystemObjectManager.convertToDTO((FileInfo)nextFile));
        }

        result.sort(comparing(FileInfoDTO::getFilename));

        return result;
    }

    @Operation(summary = "Scan source directories and update the database with current file system state")
    @PostMapping(path="/gather")
    public List<GatherDataDTO> gather(@RequestParam(name="sourceId", required = false) Integer sourceId) {
        LOG.info("Gather");
        List<GatherDataDTO> result = driveManager.gather(sourceId);
        summaryCtrl.refreshSummary();
        return result;
    }

    @Operation(summary = "Identify duplicate files across all tracked locations")
    @PostMapping(path="/duplicates")
    public List<DuplicateDataDTO> duplicate() {
        LOG.info("Duplicate check");
        return duplicateManager.duplicateCheck();
    }

    @Operation(summary = "Run directory synchronisation, optionally for a single sync pair")
    @PostMapping(path="/sync/run")
    public List<SyncDataDTO> synchronize(@RequestParam(name="syncId", required = false) Integer syncId) {
        LOG.info("Synchronize");
        List<SyncDataDTO> result = synchronizeManager.synchronize(syncId);
        summaryCtrl.refreshSummary();
        return result;
    }

    private int getParentId(Optional<FileSystemObject> optParent) {
        if(optParent.isEmpty()) {
            return -1;
        }

        FileSystemObject parent = optParent.get();
        return parent.getParentId().map(FileSystemObjectId::getId).orElse(-1);
    }

    @Operation(summary = "Navigate the directory hierarchy; pass id=-1 to list roots")
    @PostMapping(path="/hierarchy")
    public List<HierarchyResponse> hierarchy( @RequestBody HierarchyResponse lastResponse ) {
        List<HierarchyResponse> result = new ArrayList<>();

        // Get the options for directory and their ids.
        if(lastResponse.getId() == -1) {
            List<Integer> sourceIds = new ArrayList<>();

            // Level 1 - get those sources that are the left-hand side of synchronisation.
            for(Synchronize nextSynchronize: associatedFileDataManager.findAllSynchronize()) {
                if(sourceIds.contains(nextSynchronize.getSource().getIdAndType().getId())) {
                    continue;
                }

                // Generate the response.
                sourceIds.add(nextSynchronize.getSource().getIdAndType().getId());
                HierarchyResponse response = new HierarchyResponse();
                response.setId(nextSynchronize.getSource().getIdAndType().getId());
                response.setDirectory(true);
                response.setDisplayName("/");
                response.setUnderlyingId(nextSynchronize.getSource().getIdAndType().getId());

                String[] directories = nextSynchronize.getSource().getPath().split("/");

                response.setDisplayName(directories[directories.length-1]);

                result.add(response);
            }

            return result;
        }

        // The first item is the backup.
        Optional<FileSystemObject> parent = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(lastResponse.getId(), FileSystemObjectType.FSO_DIRECTORY));

        HierarchyResponse response = new HierarchyResponse();
        response.setId(getParentId(parent));
        response.setDirectory(true);
        response.setUnderlyingId(lastResponse.getId());
        response.setBackup(true);

        result.add(response);

        // Get everything that has a parent of the id provided.
        List<DirectoryInfo> directories = new ArrayList<>();
        List<FileInfo> files = new ArrayList<>();
        fileSystemObjectManager.loadImmediateByParent(lastResponse.getId(), directories, files);

        for(DirectoryInfo nextDirectory : directories) {
            response = new HierarchyResponse();
            response.setId(nextDirectory.getIdAndType().getId());
            response.setDirectory(true);
            response.setPath(nextDirectory.getName());
            response.setDisplayName(nextDirectory.getName());
            response.setUnderlyingId(nextDirectory.getIdAndType().getId());

            result.add(response);
        }

        for(FileInfo nextFile : files) {
            response = new HierarchyResponse();
            response.setId(nextFile.getIdAndType().getId());
            response.setDirectory(false);
            response.setPath(nextFile.getName());
            response.setDisplayName(nextFile.getName());
            response.setUnderlyingId(nextFile.getIdAndType().getId());
            response.setSize(nextFile.getSize());
            response.setMd5(nextFile.getMd5().isPresent() ? nextFile.getMd5().get() : null);
            response.setDateTime(nextFile.getDate());

            result.add(response);
        }

        result.sort(Comparator.comparingInt(HierarchyResponse::getOrderingIndex)
                .thenComparingInt(HierarchyResponse::getNumericValue)
                .thenComparing(HierarchyResponse::getCompareName));

        return result;
    }

    @GetMapping(path="/files/detail")
    public FileInfoExtra getFile(@RequestParam("id") Integer id) throws InvalidFileIdException {
        return fileSystemObjectManager.getFileExtra(id);
    }

    @Operation(summary = "Re-read EXIF metadata and update the database record for a file")
    @PostMapping(path="/files/refresh")
    public FileInfoExtra refreshFileData(@RequestParam("id") Integer id) throws InvalidFileIdException {
        return fileSystemObjectManager.refreshFileData(id);
    }

    @GetMapping(path="/files/search")
    public List<String> findFile(@RequestParam("search") String search) {
        return fileSystemObjectManager.findFiles(search);
    }

    @Operation(summary = "Set or clear the expiry date on a file (expired files are candidates for deletion)")
    @PutMapping(path="/files/expire")
    public FileInfoExtra expireFile(@RequestBody FileExpiryDTO expiry) throws InvalidFileIdException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(expiry.getId(),FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(expiry.getId());
        }
        fileSystemObjectManager.setFileExpiry(file.get().getIdAndType(), expiry.getExpiry());

        return getFile(expiry.getId());
    }

    @GetMapping(path="/files/image",produces= MediaType.IMAGE_JPEG_VALUE)
    public byte[] getFileImage(@RequestParam("id") Integer id) throws InvalidFileIdException, InvalidMediaTypeException, IOException, InterruptedException, NoSuchAlgorithmException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(id);
        }

        // Is this an image file?
        FileInfo loadedFile = (FileInfo)file.get();
        if(loadedFile.getClassification() == null || !loadedFile.getClassification().getIsImage()) {
            throw new InvalidMediaTypeException("image");
        }

        File imgPath = fileSystemObjectManager.getFile(loadedFile);
        LOG.info("Get file: {}", imgPath);

        String transformer = loadedFile.getClassification().getImageTransformer();
        if(transformer != null && !transformer.isEmpty()) {
            imgPath = fileSystem.getTransformedImageFile(imgPath, transformer);
        }

        return fileSystem.readAllBytes(imgPath);
    }

    private FileInfo getVideoFile(Integer id) throws InvalidFileIdException, InvalidMediaTypeException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(id);
        }

        // Is this a video file?
        FileInfo loadedFile = (FileInfo)file.get();
        if(loadedFile.getClassification() == null || !loadedFile.getClassification().getIsVideo()) {
            throw new InvalidMediaTypeException("video");
        }

        return loadedFile;
    }

    @GetMapping(path="/files/video-thumbnail",produces=MediaType.IMAGE_JPEG_VALUE)
    public byte[] getFileVideoImage(@RequestParam("id") Integer id) throws InvalidFileIdException, InvalidMediaTypeException, IOException {
        File imgPath = fileSystemObjectManager.getImageFromVideoFile(getVideoFile(id));
        LOG.info("Get file (video): {}", imgPath);

        return fileSystem.readAllBytes(imgPath);
    }

    @GetMapping(path="/files/video",produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] getFileVideo(@RequestParam("id") Integer id) throws InvalidFileIdException, InvalidMediaTypeException, IOException {
        File imgPath = fileSystemObjectManager.getFile(getVideoFile(id));
        LOG.info("Get file (video image): {}", imgPath);

        return imgPath == null ? new byte[0] : fileSystem.readAllBytes(imgPath);
    }

    @Operation(summary = "Update the date/time of a file that has metadata — writes EXIF, sets OS mtime, and recalculates MD5")
    @PutMapping(path="/files/date")
    public FileInfoExtra setFileDate(@RequestBody FileDateUpdateDTO update) throws InvalidFileIdException, FileMetaDataMissingException {
        return fileSystemObjectManager.setFileDate(update.getId(), update.getDate());
    }

    @Operation(summary = "Update the GPS location of a file that has metadata — writes EXIF and recalculates MD5")
    @PutMapping(path="/files/location")
    public FileInfoExtra setFileLocation(@RequestBody FileLocationUpdateDTO update) throws InvalidFileIdException, FileMetaDataMissingException {
        return fileSystemObjectManager.setFileLocation(update.getId(), update.getLatitude(), update.getLongitude());
    }

    @Operation(summary = "Download a file by id")
    @GetMapping(path="/files/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("id") Integer id) throws InvalidFileIdException, IOException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id, FileSystemObjectType.FSO_FILE));

        if (file.isEmpty()) {
            throw new InvalidFileIdException(id);
        }

        FileInfo fileInfo = (FileInfo) file.get();
        File physicalFile = fileSystemObjectManager.getFile(fileInfo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileInfo.getName()).build());
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok().headers(headers).body(fileSystem.readAllBytes(physicalFile));
    }

    @DeleteMapping(path="/file")
    public ActionConfirmDTO deleteFile(@RequestParam("id") Integer id) throws InvalidFileIdException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(id);
        }

        // Create a delete request.
        FileInfo loadedFile = (FileInfo)file.get();
        return actionManager.createFileDeleteAction(loadedFile);
    }
}
