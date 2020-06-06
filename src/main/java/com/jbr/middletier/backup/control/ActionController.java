package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.exception.ActionNotFoundException;
import com.jbr.middletier.backup.manager.DriveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@RestController
@RequestMapping("/jbr/int/backup")
public class ActionController {
    final static private Logger LOG = LoggerFactory.getLogger(ActionController.class);

    final private DriveManager driveManager;
    final private FileRepository fileRepository;
    final private IgnoreFileRepository ignoreFileRepository;
    final private ActionConfirmRepository actionConfirmRepository;

    @Autowired
    public ActionController(DriveManager driverManager,
                            FileRepository fileRepository,
                            IgnoreFileRepository ignoreFileRepository,
                            ActionConfirmRepository actionConfirmRepository ) {
        this.driveManager = driverManager;
        this.fileRepository = fileRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.actionConfirmRepository = actionConfirmRepository;
    }

    @RequestMapping(path="/actions",method=RequestMethod.GET)
    public @ResponseBody Iterable<ActionConfirm> getActions() {
        LOG.info("Get actions");

        return actionConfirmRepository.findByConfirmed(false);
    }

    @RequestMapping(path="/confirmed-actions",method=RequestMethod.GET)
    public @ResponseBody Iterable<ActionConfirm> getConfirmedActions() {
        LOG.info("Get actions");

        return actionConfirmRepository.findByConfirmed(true);
    }

    @RequestMapping(path="/ignore",method=RequestMethod.GET)
    public @ResponseBody Iterable<IgnoreFile> getIgnoreFiles() {
        LOG.info("Get ignore files");

        return ignoreFileRepository.findAll();
    }

    @RequestMapping(path="action-image",produces=MediaType.IMAGE_JPEG_VALUE,method=RequestMethod.GET)
    public @ResponseBody byte[] getActionImage(@RequestParam Integer actionId) throws IOException {
        LOG.info("Get action image.");

        Optional<ActionConfirm> existingAction = actionConfirmRepository.findById(actionId);

        if(!existingAction.isPresent()) {
            throw new ActionNotFoundException(actionId);
        }

        File imgPath = new File(existingAction.get().getPath());

        return Files.readAllBytes(imgPath.toPath());
    }

    @RequestMapping(path="/actions",method=RequestMethod.POST)
    public @ResponseBody ActionConfirm confirm (@RequestBody ConfirmActionRequest action) {
        LOG.info("Confirm action");

        // Is this a valid action?
        Optional<ActionConfirm> existingAction = actionConfirmRepository.findById(action.getId());

        if(!existingAction.isPresent()) {
            throw new ActionNotFoundException(action.getId());
        }

        // Update the action.
        existingAction.get().setConfirmed(true);
        existingAction.get().setParameter(action.getParameter());

        actionConfirmRepository.save(existingAction.get());

        return existingAction.get();
    }
}
