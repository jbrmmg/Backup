package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.ImportSourceDTO;
import com.jbr.middletier.backup.dto.PostImportSourceDTO;
import com.jbr.middletier.backup.dto.PreImportSourceDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.exception.InvalidSourceIdException;
import com.jbr.middletier.backup.exception.SourceAlreadyExistsException;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Sources", description = "Source directory configuration for backup and import pipelines")
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

    @GetMapping(path="/sources")
    public List<SourceDTO> getSource() {
        return getSources();
    }

    @PostMapping(path="/sources")
    public List<SourceDTO> createSource(@NotNull @RequestBody SourceDTO source) throws SourceAlreadyExistsException {
        associatedFileDataManager.createSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PutMapping(path="/sources")
    public List<SourceDTO> updateSource(@NotNull @RequestBody SourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.updateSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @DeleteMapping(path="/sources")
    public List<SourceDTO> deleteSource(@RequestBody SourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.deleteSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PostMapping(path="/sources/import")
    public List<SourceDTO> createImportSource(@NotNull @RequestBody ImportSourceDTO source) throws SourceAlreadyExistsException {
        associatedFileDataManager.createImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PutMapping(path="/sources/import")
    public List<SourceDTO> updateImportSource(@NotNull @RequestBody ImportSourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.updateImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @DeleteMapping(path="/sources/import")
    public List<SourceDTO> deleteImportSource(@RequestBody ImportSourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.deleteImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PostMapping(path="/sources/pre-import")
    public List<SourceDTO> createPreImportSource(@NotNull @RequestBody PreImportSourceDTO source) throws SourceAlreadyExistsException {
        associatedFileDataManager.createPreImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PutMapping(path="/sources/pre-import")
    public List<SourceDTO> updatePreImportSource(@NotNull @RequestBody PreImportSourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.updatePreImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @DeleteMapping(path="/sources/pre-import")
    public List<SourceDTO> deletePreImportSource(@RequestBody PreImportSourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.deletePreImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PostMapping(path="/sources/post-import")
    public List<SourceDTO> createPosImportSource(@NotNull @RequestBody PostImportSourceDTO source) throws SourceAlreadyExistsException {
        associatedFileDataManager.createPostImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @PutMapping(path="/sources/post-import")
    public List<SourceDTO> updatePostImportSource(@NotNull @RequestBody PostImportSourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.updatePostImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }

    @DeleteMapping(path="/sources/post-import")
    public List<SourceDTO> deletePostImportSource(@RequestBody PostImportSourceDTO source) throws InvalidSourceIdException {
        associatedFileDataManager.deletePostImportSource(associatedFileDataManager.convertToEntity(source));
        return getSources();
    }
}
