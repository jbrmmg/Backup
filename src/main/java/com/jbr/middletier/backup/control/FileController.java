package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.ActionConfirmDTO;
import com.jbr.middletier.backup.dto.DuplicateDataDTO;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.dto.SyncDataDTO;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.exception.InvalidMediaTypeException;
import com.jbr.middletier.backup.exception.MissingFileSystemObject;
import com.jbr.middletier.backup.manager.*;
import liquibase.repackaged.org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/jbr/int/backup")
public class FileController {
    private static final Logger LOG = LoggerFactory.getLogger(FileController.class);

    private final DriveManager driveManager;
//    private final FileRepository fileRepository;
    private final AssociatedFileDataManager associatedFileDataManager;
//    private final DirectoryRepository directoryRepository;
    private final ActionManager actionManager;
    private final DuplicateManager duplicateManager;
    private final SynchronizeManager synchronizeManager;
    private final FileSystemObjectManager fileSystemObjectManager;

    @Contract(pure = true)
    @Autowired
    public FileController(DriveManager driverManager,
//                          FileRepository fileRepository,
                          AssociatedFileDataManager associatedFileDataManager,
//                          DirectoryRepository directoryRepository,
                          ActionManager actionManager,
                          DuplicateManager duplicateManager,
                          SynchronizeManager synchronizeManager,
                          FileSystemObjectManager fileSystemObjectManager) {
        this.driveManager = driverManager;
        this.fileSystemObjectManager = fileSystemObjectManager;
//        this.fileRepository = fileRepository;
        this.associatedFileDataManager = associatedFileDataManager;
//        this.directoryRepository = directoryRepository;
        this.actionManager = actionManager;
        this.duplicateManager = duplicateManager;
        this.synchronizeManager = synchronizeManager;
    }

    @GetMapping(path="/files")
    public @ResponseBody Iterable<FileSystemObject> getFiles() {
        return fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_FILE);
    }

    @PostMapping(path="/gather")
    public @ResponseBody List<GatherDataDTO> gather(@RequestBody String reason) throws IOException {
        LOG.info("Process drive - {}", reason);

        return driveManager.gather();
    }

    @PostMapping(path="/duplicate")
    public @ResponseBody List<DuplicateDataDTO> duplicate(@RequestBody String temp) {
        LOG.info("Process drive - {}", temp);

        return duplicateManager.duplicateCheck();
    }

    @PostMapping(path="/sync")
    public @ResponseBody List<SyncDataDTO> synchronize(@RequestBody String temp) {
        LOG.info("Syncronize drives - {}", temp);

        return synchronizeManager.synchronize();
    }

    @PostMapping(path="/hierarchy")
    public @ResponseBody List<HierarchyResponse> hierarchy( @RequestBody HierarchyResponse lastResponse ) {
        List<HierarchyResponse> result = new ArrayList<>();

        // Get the options for directory and their ids.
        if(lastResponse.getId() == -1) {
            List<Integer> sourceIds = new ArrayList<>();

            // Level 1 - get those sources that are the left hand side of synchronisation.
            for(Synchronize nextSynchronize: associatedFileDataManager.internalFindAllSynchronize()) {
                if(sourceIds.contains(nextSynchronize.getSource().getIdAndType().getId())) {
                    continue;
                }

                // Generate the response.
                sourceIds.add(nextSynchronize.getSource().getIdAndType().getId());
                HierarchyResponse response = new HierarchyResponse(nextSynchronize.getSource().getIdAndType().getId(),0,"/",-1);

                String[] directories = nextSynchronize.getSource().getPath().split("/");

                response.setDisplayName(directories[directories.length-1]);

                result.add(response);
            }

            return result;
        }

        // Get the next level
        if(true)
            throw new IllegalStateException("fix this");
//        result = directoryRepository.findAtLevel(lastResponse.getId(),lastResponse.getLevel() + 1,lastResponse.getPath() + "%");

        // Update the display name.
        for(HierarchyResponse nextResponse: result) {
            String[] directories = nextResponse.getPath().split("/");

            nextResponse.setDisplayName(directories[directories.length-1]);
        }

        // Get any files that are in this directory.
        throw new NotImplementedException("Need to change this query!");
        /* TODO - fix this
        Iterable<FileInfo> files = fileRepository.findByDirectoryInfo(null);

        for(FileInfo nextFile: files) {
            if(nextFile.getName().equals(".")) {
                continue;
            }

            HierarchyResponse response = new HierarchyResponse();
            response.setDirectory(false);
            response.setLevel(lastResponse.getLevel());
            response.setPath(nextFile.getDirectoryInfo().getName());
            response.setDisplayName(nextFile.getName());
            response.setUnderlyingId(nextFile.getId());

            result.add(response);
        }

        return result;
         */
    }

    @GetMapping(path="/file")
    public @ResponseBody FileInfoExtra getFile(@RequestParam Integer id ) throws InvalidFileIdException, MissingFileSystemObject {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE), false);

        if(!file.isPresent()) {
            throw new InvalidFileIdException(id);
        }

        FileInfo originalFile = (FileInfo)file.get();
        FileInfoExtra result = new FileInfoExtra(originalFile);

        // Are there backups of this file?
        Iterable<FileSystemObject> sameName = fileSystemObjectManager.findFileSystemObjectByName(file.get().getName(), FileSystemObjectType.FSO_FILE);

        // Must be the same size and md5 if present.
        List<FileInfo> backups = new ArrayList<>();

        for(FileSystemObject nextSameName: sameName) {
            if(nextSameName.getIdAndType().equals(file.get().getIdAndType())) {
                continue;
            }

            if( !(nextSameName instanceof FileInfo)) {
                continue;
            }

            FileInfo nextFile = (FileInfo)nextSameName;

            if(nextFile.getSize().equals(originalFile.getSize()) && nextFile.getMD5().compare(originalFile.getMD5(),true)) {
                backups.add(nextFile);
            }
        }

        result.setBackups(backups);

        return result;
    }

    @GetMapping(path="/fileImage",produces= MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getFileImage(@RequestParam Integer id) throws InvalidFileIdException, InvalidMediaTypeException, IOException, MissingFileSystemObject {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE), false);

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

        return Files.readAllBytes(imgPath.toPath());
    }

    @GetMapping(path="/fileVideo",produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] getFileVideo(@RequestParam Integer id) throws InvalidFileIdException, InvalidMediaTypeException, IOException, MissingFileSystemObject {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE), false);

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

        return Files.readAllBytes(imgPath.toPath());
    }

    @DeleteMapping(path="/file")
    public @ResponseBody ActionConfirmDTO deleteFile(@RequestParam Integer id) throws InvalidFileIdException, MissingFileSystemObject {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE), false);

        if(!file.isPresent()) {
            throw new InvalidFileIdException(id);
        }

        // Create a delete request.
        FileInfo loadedFile = (FileInfo)file.get();
        return actionManager.createFileDeleteAction(loadedFile);
    }
}
