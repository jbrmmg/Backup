package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.exception.ActionNotFoundException;
import com.jbr.middletier.backup.manager.ActionManager;
import com.jbr.middletier.backup.summary.Summary;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/jbr/int/backup")
public class ActionController {
    private static final Logger LOG = LoggerFactory.getLogger(ActionController.class);

    private final IgnoreFileRepository ignoreFileRepository;
    private final ActionConfirmRepository actionConfirmRepository;
    private final ActionManager emailManager;
    private final Summary summary;

    @Contract(pure = true)
    @Autowired
    public ActionController(IgnoreFileRepository ignoreFileRepository,
                            ActionConfirmRepository actionConfirmRepository,
                            ActionManager emailManager,
                            SourceRepository sourceRepository,
                            DirectoryRepository directoryRepository,
                            FileRepository fileRepository) {
        this.ignoreFileRepository = ignoreFileRepository;
        this.actionConfirmRepository = actionConfirmRepository;
        this.emailManager = emailManager;
        this.summary = Summary.getInstance(sourceRepository,directoryRepository,fileRepository);
    }

    @GetMapping(path="/actions")
    public @ResponseBody Iterable<ActionConfirm> getActions() {
        LOG.info("Get actions");

        return actionConfirmRepository.findByConfirmed(false);
    }

    @GetMapping(path="/confirmed-actions")
    public @ResponseBody Iterable<ActionConfirm> getConfirmedActions() {
        LOG.info("Get actions");

        return actionConfirmRepository.findByConfirmed(true);
    }

    @GetMapping(path="/ignore")
    public @ResponseBody Iterable<IgnoreFile> getIgnoreFiles() {
        LOG.info("Get ignore files");

        return ignoreFileRepository.findAllByOrderByIdAsc();
    }

    @PostMapping(path="/actions")
    public @ResponseBody ActionConfirm confirm (@NotNull @RequestBody ConfirmActionRequest action) {
        LOG.info("Confirm action");

        // Is this a valid action?
        Optional<ActionConfirm> existingAction = actionConfirmRepository.findById(action.getId());

        if(!existingAction.isPresent()) {
            throw new ActionNotFoundException(action.getId());
        }

        // What type is this?
        if(existingAction.get().getAction().equals("IMPORT") || Boolean.TRUE.equals(action.getConfirm())) {
            // For import, always confirm the action.
            existingAction.get().setConfirmed(true);
            existingAction.get().setParameter(action.getParameter());

            actionConfirmRepository.save(existingAction.get());
        } else {
            actionConfirmRepository.deleteById(action.getId());
        }

        return existingAction.get();
    }

    @PostMapping(path="/actionemail")
    public @ResponseBody  OkStatus emailActions() {
        emailManager.sendActionEmail();

        return OkStatus.getOkStatus();
    }

    @GetMapping(path="/summary")
    public @ResponseBody
    Summary summary() {
        return this.summary;
    }
}
