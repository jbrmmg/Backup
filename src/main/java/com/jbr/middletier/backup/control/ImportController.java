package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.ImportRequestException;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.manager.ImportManager;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/jbr/int/backup")
public class ImportController {
    private static final Logger LOG = LoggerFactory.getLogger(ImportController.class);

    private final ImportManager importManager;

    @Contract(pure = true)
    @Autowired
    public ImportController(ImportManager importManager) {
        this.importManager = importManager;
    }

    @PostMapping(path = "/convert")
    public List<ImportProcessDTO> processImports() {
        LOG.info("Convert files from pre import to import");

        return importManager.convertImportFiles();
    }

    @PostMapping(path = "/import")
    public List<GatherDataDTO> importPhotoDirectory() throws ImportRequestException, IOException {
        LOG.info("Import the files");

        return importManager.importPhoto();
    }

    @PostMapping(path = "/importprocess")
    public List<ImportDataDTO> importPhotoProcess() throws ImportRequestException {
        LOG.info("Process the import files.");

        return importManager.processImportFiles();
    }

    @GetMapping(path = "/importfiles")
    public List<ImportFileDTO> getImportFiles() {
        LOG.info("Get the import files.");

        return importManager.externalFindImportFiles();
    }

    @GetMapping(path = "/importfile")
    public ImportFileDTO getFile(@RequestParam Integer id) throws InvalidFileIdException {
        LOG.info("Get the import files.");

        return importManager.externalFindImportFile(id);
    }
}
