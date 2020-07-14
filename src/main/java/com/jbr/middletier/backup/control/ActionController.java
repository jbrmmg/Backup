package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.exception.ActionNotFoundException;
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
    final static private Logger LOG = LoggerFactory.getLogger(ActionController.class);

    final private IgnoreFileRepository ignoreFileRepository;
    final private ActionConfirmRepository actionConfirmRepository;

    @Contract(pure = true)
    @Autowired
    public ActionController(IgnoreFileRepository ignoreFileRepository,
                            ActionConfirmRepository actionConfirmRepository ) {
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

    @RequestMapping(path="/actions",method=RequestMethod.POST)
    public @ResponseBody ActionConfirm confirm (@NotNull @RequestBody ConfirmActionRequest action) {
        LOG.info("Confirm action");

        // Is this a valid action?
        Optional<ActionConfirm> existingAction = actionConfirmRepository.findById(action.getId());

        if(!existingAction.isPresent()) {
            throw new ActionNotFoundException(action.getId());
        }

        // What type is this?
        if(existingAction.get().getAction().equals("IMPORT") || action.getConfirm()) {
            // For import, always confirm the action.
            existingAction.get().setConfirmed(true);
            existingAction.get().setParameter(action.getParameter());

            actionConfirmRepository.save(existingAction.get());
        } else {
            actionConfirmRepository.deleteById(action.getId());
        }

        return existingAction.get();
    }
}
