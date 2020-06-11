package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.HierarchyResponse;
import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.data.Synchronize;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.SynchronizeRepository;
import com.jbr.middletier.backup.manager.DriveManager;
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
    final static private Logger LOG = LoggerFactory.getLogger(FileController.class);

    final private DriveManager driveManager;
    final private FileRepository fileRepository;
    final private SynchronizeRepository synchronizeRepository;
    final private DirectoryRepository directoryRepository;

    @Autowired
    public FileController(DriveManager driverManager,
                          FileRepository fileRepository,
                          SynchronizeRepository synchronizeRepository,
                          DirectoryRepository directoryRepository ) {
        this.driveManager = driverManager;
        this.fileRepository = fileRepository;
        this.synchronizeRepository = synchronizeRepository;
        this.directoryRepository = directoryRepository;
    }

    @RequestMapping(path="/files", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<FileInfo> getFiles() { return fileRepository.findAll(); }

    @RequestMapping(path="/gather", method= RequestMethod.POST)
    public @ResponseBody
    OkStatus gather(@RequestBody String temp) {
        LOG.info("Process drive - " + temp);

        try {
            driveManager.gather();
        } catch (Exception e) {
            OkStatus status = new OkStatus();
            status.setStatus("Failed - " + e.toString());
        }

        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/duplicate", method= RequestMethod.POST)
    public @ResponseBody OkStatus duplicate(@RequestBody String temp) {
        LOG.info("Process drive - " + temp);

        driveManager.duplicateCheck();

        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/sync", method= RequestMethod.POST)
    public @ResponseBody OkStatus synchronize(@RequestBody String temp) {
        LOG.info("Syncronize drives - " + temp);

        driveManager.synchronize();

        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/hierarchy",method= RequestMethod.POST)
    public @ResponseBody List<HierarchyResponse> hierarchy( @RequestBody HierarchyResponse lastResponse ) {
        List<HierarchyResponse> result = new ArrayList<>();

        // Get the options for directory and their ids.
        if(lastResponse.getId() == -1) {
            // Level 1 - get those sources that are the left hand side of synchronisation.
            for(Synchronize nextSynchronize: synchronizeRepository.findAll()) {
                boolean alreadyAdded = false;

                for(HierarchyResponse nextResponse: result) {
                    if(nextResponse.getId() == nextSynchronize.getSource().getId()) {
                        alreadyAdded = true;
                    }
                }

                if(!alreadyAdded) {
                    HierarchyResponse response = new HierarchyResponse(nextSynchronize.getSource().getId(),0,"/",-1);

                    String[] directories = nextSynchronize.getSource().getPath().split("/");

                    response.setDisplayName(directories[directories.length-1]);

                    result.add(response);
                }
            }
        } else {
            // Get the next level
            result = directoryRepository.findAtLevel(lastResponse.getId(),lastResponse.getLevel() + 1,lastResponse.getPath() + "%");

            // Update the display name.
            for(HierarchyResponse nextResponse: result) {
                String[] directories = nextResponse.getPath().split("/");

                nextResponse.setDisplayName(directories[directories.length-1]);
            }

            // Get any files that are in this directory.
            Iterable<FileInfo> files = fileRepository.findByDirectoryInfoId(lastResponse.getUnderlyingId());

            for(FileInfo nextFile: files) {
                if(nextFile.getName().equals(".")) {
                    continue;
                }

                HierarchyResponse response = new HierarchyResponse();
                response.setDirectory(false);
                response.setLevel(lastResponse.getLevel());
                response.setPath(nextFile.getDirectoryInfo().getPath());
                response.setDisplayName(nextFile.getName());
                response.setUnderlyingId(nextFile.getId());

                result.add(response);
            }
        }

        return result;
    }

    @RequestMapping(path="/file",method= RequestMethod.GET)
    public @ResponseBody FileInfo getFile( @RequestParam Integer id ) throws Exception {
        Optional<FileInfo> file = fileRepository.findById(id);

        if(!file.isPresent()) {
            throw new Exception(id + " does not exist");
        }

        return file.get();
    }

    @RequestMapping(path="/fileImage",produces= MediaType.IMAGE_JPEG_VALUE,method= RequestMethod.GET)
    public @ResponseBody byte[] getFileImage(@RequestParam Integer id) throws Exception {
        Optional<FileInfo> file = fileRepository.findById(id);

        if(!file.isPresent()) {
            throw new Exception(id + " does not exist");
        }

        if(!file.get().getName().toLowerCase().contains(".jpg")) {
            return null;
        }

        String filename = file.get().getDirectoryInfo().getSource().getPath() + file.get().getDirectoryInfo().getPath() + "/" + file.get().getName();
        LOG.info("Get file: " + filename);

        File imgPath = new File(filename);

        return Files.readAllBytes(imgPath.toPath());
    }

    @RequestMapping(path="/fileVideo",produces=MediaType.APPLICATION_OCTET_STREAM_VALUE,method=RequestMethod.GET)
    public @ResponseBody byte[] getFileVideo(@RequestParam Integer id) throws Exception {
        Optional<FileInfo> file = fileRepository.findById(id);

        if(!file.isPresent()) {
            throw new Exception(id + " does not exist");
        }

        if(!file.get().getName().toLowerCase().contains(".mp4") && !file.get().getName().toLowerCase().contains(".mov")) {
            return null;
        }

        String filename = file.get().getDirectoryInfo().getSource().getPath() + file.get().getDirectoryInfo().getPath() + "/" + file.get().getName();
        LOG.info("Get file: " + filename);

        File imgPath = new File(filename);

        return Files.readAllBytes(imgPath.toPath());
    }

}
