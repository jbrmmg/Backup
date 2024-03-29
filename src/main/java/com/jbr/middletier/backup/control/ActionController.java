package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.ActionConfirmDTO;
import com.jbr.middletier.backup.dto.FileInfoDTO;
import com.jbr.middletier.backup.manager.ActionManager;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import com.jbr.middletier.backup.summary.Summary;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparing;

@RestController
@RequestMapping("/jbr/int/backup")
public class ActionController {
    private static final Logger LOG = LoggerFactory.getLogger(ActionController.class);

    private final FileSystemObjectManager fileSystemObjectManager;
    private final ActionManager actionManager;
    private final Summary summary;

    @Contract(pure = true)
    @Autowired
    public ActionController(FileSystemObjectManager fileSystemObjectManager,
                            ActionManager actionManager,
                            AssociatedFileDataManager associatedFileDataManager) {
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.actionManager = actionManager;
        this.summary = Summary.getInstance(associatedFileDataManager, fileSystemObjectManager);
    }

    @GetMapping(path="/actions")
    public List<ActionConfirmDTO> getActions() {
        LOG.info("Get actions");

        return actionManager.externalFindByConfirmed(false);
    }

    @GetMapping(path="/confirmed-actions")
    public List<ActionConfirmDTO> getConfirmedActions() {
        LOG.info("Get actions");

        return actionManager.externalFindByConfirmed(true);
    }

    @GetMapping(path="/ignore")
    public List<FileInfoDTO> getIgnoreFiles() {
        LOG.info("Get ignore files");

        List<FileInfoDTO> result = new ArrayList<>();
        for(FileSystemObject nextFile: fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_IGNORE_FILE)) {
            result.add(fileSystemObjectManager.convertToDTO((IgnoreFile)nextFile));
        }

        result.sort(comparing(FileInfoDTO::getFilename));
        return result;
    }

    @PostMapping(path="/actions")
    public ActionConfirmDTO confirm (@NotNull @RequestBody ConfirmActionRequest action) {
        LOG.info("Confirm action");

        return actionManager.confirmAction(action);
    }

    @PostMapping(path="/actionemail")
    public  OkStatus emailActions() {
        actionManager.sendActionEmail();

        return OkStatus.getOkStatus();
    }

    @GetMapping(path="/summary")
    public Summary summary() {
        return this.summary;
    }
}
