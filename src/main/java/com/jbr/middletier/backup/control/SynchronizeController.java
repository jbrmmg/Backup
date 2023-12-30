package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.SynchronizeDTO;
import com.jbr.middletier.backup.exception.InvalidSynchronizeIdException;
import com.jbr.middletier.backup.exception.SynchronizeAlreadyExistsException;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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

    private List<SynchronizeDTO> getSynchronizations() {
        List<SynchronizeDTO> result = new ArrayList<>();

        associatedFileDataManager.findAllSynchronize().forEach(synchronize -> result.add(associatedFileDataManager.convertToDTO(synchronize)));
        LOG.info("Get the synchronizations - {}", result.size());

        return result;
    }

    @GetMapping(path="/synchronize")
    public List<SynchronizeDTO> getSynchronize() {
        return getSynchronizations();
    }

    @PostMapping(path="/synchronize")
    public List<SynchronizeDTO> createSynchronize(@NotNull @RequestBody SynchronizeDTO synchronize) throws SynchronizeAlreadyExistsException {
        associatedFileDataManager.createSynchronize(associatedFileDataManager.convertToEntity(synchronize));
        return getSynchronizations();
    }

    @PutMapping(path="/synchronize")
    public List<SynchronizeDTO> updateSynchronize(@NotNull @RequestBody SynchronizeDTO synchronize) throws InvalidSynchronizeIdException {
        associatedFileDataManager.updateSynchronize(associatedFileDataManager.convertToEntity(synchronize));
        return getSynchronizations();
    }

    @DeleteMapping(path="/synchronize")
    public List<SynchronizeDTO> deleteSynchronize(@RequestBody SynchronizeDTO synchronize) throws InvalidSynchronizeIdException {
        associatedFileDataManager.deleteSynchronize(associatedFileDataManager.convertToEntity(synchronize));
        return getSynchronizations();
    }
}
