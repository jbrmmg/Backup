package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.ActionConfirmRepository;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.SynchronizeRepository;
import com.jbr.middletier.backup.dto.SyncDataDTO;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.exception.InvalidMediaTypeException;
import com.jbr.middletier.backup.manager.DriveManager;
import com.jbr.middletier.backup.manager.DuplicateManager;
import com.jbr.middletier.backup.manager.SynchronizeManager;
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
    private final FileRepository fileRepository;
    private final SynchronizeRepository synchronizeRepository;
    private final DirectoryRepository directoryRepository;
    private final ActionConfirmRepository actionConfirmRepository;
    private final DuplicateManager duplicateManager;
    private final SynchronizeManager synchronizeManager;

    @Contract(pure = true)
    @Autowired
    public FileController(DriveManager driverManager,
                          FileRepository fileRepository,
                          SynchronizeRepository synchronizeRepository,
                          DirectoryRepository directoryRepository,
                          ActionConfirmRepository actionConfirmRepository,
                          DuplicateManager duplicateManager,
                          SynchronizeManager synchronizeManager ) {
        this.driveManager = driverManager;
        this.fileRepository = fileRepository;
        this.synchronizeRepository = synchronizeRepository;
        this.directoryRepository = directoryRepository;
        this.actionConfirmRepository = actionConfirmRepository;
        this.duplicateManager = duplicateManager;
        this.synchronizeManager = synchronizeManager;
    }

    @GetMapping(path="/files")
    public @ResponseBody
    Iterable<FileInfo> getFiles() { return fileRepository.findAllByOrderByIdAsc(); }

    @PostMapping(path="/gather")
    public @ResponseBody OkStatus gather(@RequestBody String reason) throws IOException {
        LOG.info("Process drive - {}", reason);

        driveManager.gather();

        return OkStatus.getOkStatus();
    }

    @PostMapping(path="/duplicate")
    public @ResponseBody OkStatus duplicate(@RequestBody String temp) {
        LOG.info("Process drive - {}", temp);

        duplicateManager.duplicateCheck();

        return OkStatus.getOkStatus();
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
            for(Synchronize nextSynchronize: synchronizeRepository.findAll()) {
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
    public @ResponseBody FileInfoExtra getFile(@RequestParam Integer id ) throws InvalidFileIdException {
        Optional<FileInfo> file = fileRepository.findById(id);

        if(!file.isPresent()) {
            throw new InvalidFileIdException(id);
        }

        FileInfoExtra result = new FileInfoExtra(file.get());

        // Are there backups of this file?
        List<FileInfo> sameName = fileRepository.findByName(file.get().getName());

        // Must be the same size and md5 if present.
        List<FileInfo> backups = new ArrayList<>();

        String fileMD5 = file.get().getMD5() != null ? file.get().getMD5() : "";

        for(FileInfo nextSameName: sameName) {
            if(nextSameName.getIdAndType().equals(file.get().getIdAndType())) {
                continue;
            }

            if(nextSameName.getSize().equals(file.get().getSize())) {
                String nextMD6 = nextSameName.getMD5() != null ? nextSameName.getMD5() : "";

                if(fileMD5.equals(nextMD6) || fileMD5.equals("") || nextMD6.equals("") ) {
                    backups.add(nextSameName);
                }
            }
        }

        result.setBackups(backups);

        return result;
    }

    @GetMapping(path="/fileImage",produces= MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getFileImage(@RequestParam Integer id) throws InvalidFileIdException, InvalidMediaTypeException, IOException {
        Optional<FileInfo> file = fileRepository.findById(id);

        if(!file.isPresent()) {
            throw new InvalidFileIdException(id);
        }

        // Is this an image file
        if(file.get().getClassification() == null || !file.get().getClassification().getIsImage()) {
            throw new InvalidMediaTypeException("image");
        }

        LOG.info("Get file: {}", file.get().getFullFilename());

        File imgPath = new File(file.get().getFullFilename());

        return Files.readAllBytes(imgPath.toPath());
    }

    @GetMapping(path="/fileVideo",produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] getFileVideo(@RequestParam Integer id) throws InvalidFileIdException, InvalidMediaTypeException, IOException {
        Optional<FileInfo> file = fileRepository.findById(id);

        if(!file.isPresent()) {
            throw new InvalidFileIdException(id);
        }

        // Is this a video file?
        if(file.get().getClassification() == null || !file.get().getClassification().getIsVideo()) {
            throw new InvalidMediaTypeException("video");
        }

        LOG.info("Get file: {}", file.get().getFullFilename());

        File imgPath = new File(file.get().getFullFilename());

        return Files.readAllBytes(imgPath.toPath());
    }

    @DeleteMapping(path="/file")
    public @ResponseBody OkStatus deleteFile(@RequestParam Integer id) throws InvalidFileIdException {
        Optional<FileInfo> file = fileRepository.findById(id);

        if(!file.isPresent()) {
            throw new InvalidFileIdException(id);
        }

        // Create a delete request.
        ActionConfirm actionConfirm = new ActionConfirm();
        actionConfirm.setFileInfo(file.get());
        actionConfirm.setAction("DELETE");
        actionConfirm.setConfirmed(false);
        actionConfirm.setParameterRequired(false);

        actionConfirmRepository.save(actionConfirm);

        return OkStatus.getOkStatus();
    }
}
