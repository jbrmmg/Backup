package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.data.ImportRequest;
import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.exception.ImportRequestException;
import com.jbr.middletier.backup.exception.MissingFileSystemObject;
import com.jbr.middletier.backup.manager.ImportManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
    public ImportController(ImportManager importManager ) {
        this.importManager = importManager;
    }

    @PostMapping(path="/import")
    public @ResponseBody List<GatherDataDTO> importPhotoDirectory(@NotNull @RequestBody ImportRequest importRequest) throws ImportRequestException, IOException {
        LOG.info("Import - {}", importRequest.getPath());

        return importManager.importPhoto(importRequest);
    }

    @DeleteMapping(path="/import")
    public @ResponseBody List<GatherDataDTO> removeEntries() throws MissingFileSystemObject {
        LOG.info("Remove entries from import table");

        return importManager.removeEntries();
    }

    @PostMapping(path="/importprocess")
    public @ResponseBody List<GatherDataDTO> importPhotoProcess() throws ImportRequestException, MissingFileSystemObject {
        LOG.info("Import - process");

        return importManager.importPhotoProcess();
    }

    @GetMapping(path="/importfiles")
    public @ResponseBody Iterable<ImportFile> getImportFiles() {
        LOG.info("Get the import files.");

        return importManager.findImportFiles();
    }

    @PutMapping(path="/importfiles")
    public @ResponseBody Iterable<ImportFile> resetFiles() {
        LOG.info("Get the import files.");

        return importManager.resetFiles();
    }
}
