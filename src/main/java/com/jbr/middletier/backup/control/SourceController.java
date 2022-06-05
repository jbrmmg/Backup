package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.exception.InvalidSourceIdException;
import com.jbr.middletier.backup.exception.SourceAlreadyExistsException;
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
public class SourceController {
    private static final Logger LOG = LoggerFactory.getLogger(SourceController.class);

    private final AssociatedFileDataManager associatedFileDataManager;

    @Contract(pure = true)
    @Autowired
    public SourceController(AssociatedFileDataManager associatedFileDataManager) {
        this.associatedFileDataManager = associatedFileDataManager;
    }

    @GetMapping(path="/source")
    public @ResponseBody List<SourceDTO> getSource() {
        LOG.info("Get the source");
        return associatedFileDataManager.externalFindAllSource();
    }

    @PostMapping(path="/source")
    public @ResponseBody List<SourceDTO> createSource(@NotNull @RequestBody SourceDTO source) throws SourceAlreadyExistsException {
        associatedFileDataManager.createSource(source);
        return associatedFileDataManager.externalFindAllSource();
    }

    @PutMapping(path="/source")
    public @ResponseBody List<SourceDTO> updateSource(@NotNull @RequestBody SourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.updateSource(source);
        return associatedFileDataManager.externalFindAllSource();
    }

    @DeleteMapping(path="/source")
    public @ResponseBody List<SourceDTO> deleteSource(@RequestBody SourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.deleteSource(source);
        return associatedFileDataManager.externalFindAllSource();
    }
}
