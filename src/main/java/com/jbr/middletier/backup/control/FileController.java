package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.manager.DriveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/jbr/int/backup")
public class FileController {
    final static private Logger LOG = LoggerFactory.getLogger(FileController.class);

    final private DriveManager driveManager;
    final private FileRepository fileRepository;

    @Autowired
    public FileController(DriveManager driverManager,
                           FileRepository fileRepository ) {
        this.driveManager = driverManager;
        this.fileRepository = fileRepository;
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
        } catch (IOException e) {
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
}
