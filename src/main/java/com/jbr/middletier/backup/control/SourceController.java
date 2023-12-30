package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.ImportSourceDTO;
import com.jbr.middletier.backup.dto.PreImportSourceDTO;
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

import java.util.ArrayList;
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

    private List<SourceDTO> getSources() {
        List<SourceDTO> result = new ArrayList<>();

        associatedFileDataManager.findAllSource().forEach(source -> result.add(associatedFileDataManager.convertToDTO(source)));
        LOG.info("Get the sources - {}", result.size());

        return result;
    }

    @GetMapping(path="/source")
    public List<SourceDTO> getSource() {
        return getSources();
    }

    @PostMapping(path="/source")
    public List<SourceDTO> createSource(@NotNull @RequestBody SourceDTO source) throws SourceAlreadyExistsException {
        associatedFileDataManager.createSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PutMapping(path="/source")
    public List<SourceDTO> updateSource(@NotNull @RequestBody SourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.updateSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @DeleteMapping(path="/source")
    public List<SourceDTO> deleteSource(@RequestBody SourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.deleteSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PostMapping(path="/importSource")
    public List<SourceDTO> createImportSource(@NotNull @RequestBody ImportSourceDTO source) throws SourceAlreadyExistsException {
        associatedFileDataManager.createImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PutMapping(path="/importSource")
    public List<SourceDTO> updateImportSource(@NotNull @RequestBody ImportSourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.updateImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @DeleteMapping(path="/importSource")
    public List<SourceDTO> deleteImportSource(@RequestBody ImportSourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.deleteImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PostMapping(path="/preImportSource")
    public List<SourceDTO> createPreImportSource(@NotNull @RequestBody PreImportSourceDTO source) throws SourceAlreadyExistsException {
        associatedFileDataManager.createPreImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PutMapping(path="/preImportSource")
    public List<SourceDTO> updatePreImportSource(@NotNull @RequestBody PreImportSourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.updatePreImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @DeleteMapping(path="/preImportSource")
    public List<SourceDTO> deletePreImportSource(@RequestBody PreImportSourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.deletePreImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }
}
