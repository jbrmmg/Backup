package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.exception.InvalidMediaTypeException;
import com.jbr.middletier.backup.manager.*;
import org.hibernate.sql.Select;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;

@RestController
@RequestMapping("/jbr/int/backup")
public class FileController {
    private static final Logger LOG = LoggerFactory.getLogger(FileController.class);

    private final DriveManager driveManager;
    private final AssociatedFileDataManager associatedFileDataManager;
    private final ActionManager actionManager;
    private final DuplicateManager duplicateManager;
    private final SynchronizeManager synchronizeManager;
    private final FileSystemObjectManager fileSystemObjectManager;
    private final FileSystem fileSystem;
    private final LabelManager labelManager;

    @Contract(pure = true)
    @Autowired
    public FileController(DriveManager driverManager,
                          AssociatedFileDataManager associatedFileDataManager,
                          ActionManager actionManager,
                          DuplicateManager duplicateManager,
                          SynchronizeManager synchronizeManager,
                          FileSystemObjectManager fileSystemObjectManager,
                          FileSystem fileSystem,
                          LabelManager labelManager) {
        this.driveManager = driverManager;
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.associatedFileDataManager = associatedFileDataManager;
        this.actionManager = actionManager;
        this.duplicateManager = duplicateManager;
        this.synchronizeManager = synchronizeManager;
        this.fileSystem = fileSystem;
        this.labelManager = labelManager;
    }

    @GetMapping(path="/files")
    public @ResponseBody List<FileInfoDTO> getFiles() {
        List<FileInfoDTO> result = new ArrayList<>();
        for(FileSystemObject nextFile: fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_FILE)) {
            result.add(fileSystemObjectManager.convertToDTO((FileInfo)nextFile));
        }

        result.sort(comparing(FileInfoDTO::getFilename));

        return result;
    }

    @PostMapping(path="/gather")
    public @ResponseBody List<GatherDataDTO> gather(@RequestBody String reason) {
        LOG.info("Process drive - {}", reason);

        return driveManager.gather();
    }

    @PostMapping(path="/duplicate")
    public @ResponseBody List<DuplicateDataDTO> duplicate(@RequestBody String temp) {
        LOG.info("Duplicate process drive - {}", temp);

        return duplicateManager.duplicateCheck();
    }

    @PostMapping(path="/sync")
    public @ResponseBody List<SyncDataDTO> synchronize(@RequestBody String temp) {
        LOG.info("Synchronize drives - {}", temp);

        return synchronizeManager.synchronize();
    }

    @PostMapping(path="/print")
    public @ResponseBody Integer print(@RequestBody Integer id) {
        return fileSystemObjectManager.select(id);
    }

    @PutMapping(path="/print")
    public @ResponseBody Integer updatePrint(@RequestBody SelectedPrintDTO selected) {
        return fileSystemObjectManager.updatePrint(selected);
    }

    @PostMapping(path="/unprint")
    public @ResponseBody Integer unprint(@RequestBody Integer id) {
        return fileSystemObjectManager.unselect(id);
    }

    @GetMapping(path="/prints")
    public @ResponseBody List<SelectedPrintDTO> prints() {
        return fileSystemObjectManager.getPrints();
    }

    @DeleteMapping(path="/prints")
    public @ResponseBody List<Integer> deletePrints() {
        return fileSystemObjectManager.deletePrints();
    }

    private int getParentId(Optional<FileSystemObject> optParent) {
        if(!optParent.isPresent()) {
            return -1;
        }

        FileSystemObject parent = optParent.get();
        return parent.getParentId().map(FileSystemObjectId::getId).orElse(-1);
    }

    @PostMapping(path="/hierarchy")
    public @ResponseBody List<HierarchyResponse> hierarchy( @RequestBody HierarchyResponse lastResponse ) {
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

        // First item is the backup.
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

            result.add(response);
        }

        result.sort(Comparator.comparingInt(HierarchyResponse::getOrderingIndex)
                .thenComparingInt(HierarchyResponse::getNumericValue)
                .thenComparing(HierarchyResponse::getCompareName));

        return result;
    }

    @GetMapping(path="/file")
    public @ResponseBody FileInfoExtra getFile(@RequestParam Integer id) throws InvalidFileIdException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        if(!file.isPresent()) {
            throw new InvalidFileIdException(id);
        }

        FileInfo originalFile = (FileInfo)file.get();
        File associatedFile = fileSystemObjectManager.getFile(originalFile);
        FileInfoExtra result = new FileInfoExtra(originalFile,associatedFile.getPath(),associatedFile.getPath(),associatedFile.getParent());

        // Are there backups of this file?
        Iterable<FileSystemObject> sameName = fileSystemObjectManager.findFileSystemObjectByName(file.get().getName(), FileSystemObjectType.FSO_FILE);

        for(FileSystemObject nextSameName: sameName) {
            if(nextSameName.getIdAndType().equals(file.get().getIdAndType()) || !(nextSameName instanceof FileInfo) ) {
                continue;
            }

            FileInfo nextFile = (FileInfo)nextSameName;

            if(nextFile.getSize().equals(originalFile.getSize()) && nextFile.getMD5().compare(originalFile.getMD5(),true)) {
                associatedFile = fileSystemObjectManager.getFile(nextFile);
                result.addFile(nextFile,associatedFile.getPath(),associatedFile.getPath(),associatedFile.getParent());
            }
        }

        // Get any labels.
        for(String nextLabel : labelManager.getLabelsForFile(file.get().getIdAndType())) {
            result.addLabel(nextLabel);
        }

        return result;
    }

    @PutMapping(path="/expire")
    public @ResponseBody FileInfoExtra expireFile(@RequestBody FileExpiryDTO expiry) throws InvalidFileIdException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(expiry.getId(),FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(expiry.getId());
        }
        fileSystemObjectManager.setFileExpiry(file.get().getIdAndType(), expiry.getExpiry());

        return getFile(expiry.getId());
    }

    @PostMapping(path="label")
    public @ResponseBody FileInfoExtra addLabel(@RequestBody FileLabelDTO fileLabelDTO) throws InvalidFileIdException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(fileLabelDTO.getFileId(),FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(fileLabelDTO.getFileId());
        }

        for(Integer label : fileLabelDTO.getLabels()) {
            labelManager.addLabelToFile(file.get().getIdAndType(), label);
        }

        return getFile(fileLabelDTO.getFileId());
    }

    @DeleteMapping(path="label")
    public @ResponseBody FileInfoExtra removeLabel(@RequestBody FileLabelDTO fileLabelDTO) throws InvalidFileIdException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(fileLabelDTO.getFileId(),FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(fileLabelDTO.getFileId());
        }

        for(Integer label : fileLabelDTO.getLabels()) {
            labelManager.removeLabelFromFile(file.get().getIdAndType(), label);
        }

        return getFile(fileLabelDTO.getFileId());
    }

    @GetMapping(path="labels")
    public @ResponseBody List<LabelDTO> labels() {
        return labelManager.getLabels();
    }

    @GetMapping(path="/fileImage",produces= MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getFileImage(@RequestParam Integer id) throws InvalidFileIdException, InvalidMediaTypeException, IOException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        if(!file.isPresent()) {
            throw new InvalidFileIdException(id);
        }

        // Is this an image file
        FileInfo loadedFile = (FileInfo)file.get();
        if(loadedFile.getClassification() == null || !loadedFile.getClassification().getIsImage()) {
            throw new InvalidMediaTypeException("image");
        }

        File imgPath = fileSystemObjectManager.getFile(loadedFile);
        LOG.info("Get file: {}", imgPath);

        return fileSystem.readAllBytes(imgPath);
    }

    @GetMapping(path="/fileVideo",produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] getFileVideo(@RequestParam Integer id) throws InvalidFileIdException, InvalidMediaTypeException, IOException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        if(!file.isPresent()) {
            throw new InvalidFileIdException(id);
        }

        // Is this a video file?
        FileInfo loadedFile = (FileInfo)file.get();
        if(loadedFile.getClassification() == null || !loadedFile.getClassification().getIsVideo()) {
            throw new InvalidMediaTypeException("video");
        }

        File imgPath = fileSystemObjectManager.getFile(loadedFile);
        LOG.info("Get file: {}", imgPath);

        return fileSystem.readAllBytes(imgPath);
    }

    @DeleteMapping(path="/file")
    public @ResponseBody ActionConfirmDTO deleteFile(@RequestParam Integer id) throws InvalidFileIdException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        if(!file.isPresent()) {
            throw new InvalidFileIdException(id);
        }

        // Create a delete request.
        FileInfo loadedFile = (FileInfo)file.get();
        return actionManager.createFileDeleteAction(loadedFile);
    }

    @PostMapping(path="/generate")
    public @ResponseBody OkStatus doSomething() {
        LOG.info("Get a list of the P files");

        fileSystemObjectManager.gatherList();

        return OkStatus.getOkStatus();
    }
}
