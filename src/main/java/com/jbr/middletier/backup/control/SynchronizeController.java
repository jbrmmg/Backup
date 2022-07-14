package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.SynchronizeDTO;
import com.jbr.middletier.backup.exception.InvalidSourceIdException;
import com.jbr.middletier.backup.exception.InvalidSynchronizeIdException;
import com.jbr.middletier.backup.exception.SynchronizeAlreadyExistsException;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jbr/ext/backup")
public class SynchronizeController {
    private static final Logger LOG = LoggerFactory.getLogger(SynchronizeController.class);

    private final AssociatedFileDataManager associatedFileDataManager;

    @Contract(pure = true)
    @Autowired
    public SynchronizeController(AssociatedFileDataManager associatedFileDataManager) {
        this.associatedFileDataManager = associatedFileDataManager;
    }

    @GetMapping(path="/synchronize")
    public @ResponseBody List<SynchronizeDTO> getSynchronize() {
        LOG.info("Get the synchronize");
        return associatedFileDataManager.externalFindAllSynchronize();
    }

    @PostMapping(path="/synchronize")
    public @ResponseBody List<SynchronizeDTO> createSynchronize(@NotNull @RequestBody SynchronizeDTO synchronize) throws SynchronizeAlreadyExistsException, InvalidSourceIdException {
        associatedFileDataManager.createSynchronize(synchronize);
        return associatedFileDataManager.externalFindAllSynchronize();
    }

    @PutMapping(path="/synchronize")
    public @ResponseBody List<SynchronizeDTO> updateSynchronize(@NotNull @RequestBody SynchronizeDTO synchronize) throws InvalidSynchronizeIdException, InvalidSourceIdException {
        associatedFileDataManager.updateSynchronize(synchronize);
        return associatedFileDataManager.externalFindAllSynchronize();
    }

    @DeleteMapping(path="/synchronize")
    public @ResponseBody List<SynchronizeDTO> deleteSynchronize(@RequestBody SynchronizeDTO synchronize) throws InvalidSynchronizeIdException {
        associatedFileDataManager.deleteSynchronize(synchronize);
        return associatedFileDataManager.externalFindAllSynchronize();
    }
}
