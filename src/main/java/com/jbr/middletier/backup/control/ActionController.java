package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.ActionConfirmDTO;
import com.jbr.middletier.backup.dto.FileInfoDTO;
import com.jbr.middletier.backup.manager.ActionManager;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import com.jbr.middletier.backup.summary.Summary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1")
@Tag(name = "Actions", description = "Pending file synchronisation actions and system summary")
public class ActionController {
    private static final Logger LOG = LoggerFactory.getLogger(ActionController.class);

    private final FileSystemObjectManager fileSystemObjectManager;
    private final ActionManager actionManager;
    private final Summary summary;

    @Contract(pure = true)
    @Autowired
    public ActionController(FileSystemObjectManager fileSystemObjectManager,
                            ActionManager actionManager,
                            ApplicationProperties applicationProperties,
                            AssociatedFileDataManager associatedFileDataManager) {
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.actionManager = actionManager;
        this.summary = Summary.getInstance(associatedFileDataManager, fileSystemObjectManager, applicationProperties);
    }

    @GetMapping(path="/actions")
    public List<ActionConfirmDTO> getActions() {
        LOG.info("Get actions");

        return actionManager.externalFindByConfirmed(false);
    }

    @GetMapping(path="/actions/confirmed")
    public List<ActionConfirmDTO> getConfirmedActions() {
        LOG.info("Get confirmed actions");

        return actionManager.externalFindByConfirmed(true);
    }

    @Operation(summary = "List files permanently excluded from synchronisation")
    @GetMapping(path="/ignored")
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

    @Operation(summary = "Send a summary email of all pending actions")
    @PostMapping(path="/actions/email")
    public  OkStatus emailActions() {
        actionManager.sendActionEmail();

        return OkStatus.getOkStatus();
    }

    @GetMapping(path="/summary")
    public Summary summary() {
        return this.summary;
    }
}
