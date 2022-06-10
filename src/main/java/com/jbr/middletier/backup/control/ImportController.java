package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.ImportRequest;
import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.exception.ImportRequestException;
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
    private final ImportFileRepository importFileRepository;

    @Contract(pure = true)
    @Autowired
    public ImportController(ImportManager importManager,
                            ImportFileRepository importFileRepository ) {
        this.importManager = importManager;
        this.importFileRepository = importFileRepository;
    }

    @PostMapping(path="/import")
    public @ResponseBody List<GatherDataDTO> importPhotoDirectory(@NotNull @RequestBody ImportRequest importRequest) throws ImportRequestException, IOException {
        LOG.info("Import - {}", importRequest.getPath());

        return importManager.importPhoto(importRequest);
    }

    @DeleteMapping(path="/import")
    public @ResponseBody OkStatus removeEntries() {
        LOG.info("Remove entries from import table");

        importManager.removeEntries();

        return OkStatus.getOkStatus();
    }

    @PostMapping(path="/importprocess")
    public @ResponseBody OkStatus importPhotoProcess() throws ImportRequestException {
        LOG.info("Import - process");

        importManager.importPhotoProcess();

        return OkStatus.getOkStatus();
    }

    @GetMapping(path="/importfiles")
    public @ResponseBody Iterable<ImportFile> getImportFiles() {
        LOG.info("Get the import files.");

        return importFileRepository.findAllByOrderByIdAsc();
    }

    @PutMapping(path="/importfiles")
    public @ResponseBody Iterable<ImportFile> resetFiles() {
        LOG.info("Get the import files.");

        Iterable<ImportFile> result = importFileRepository.findAll();

        for(ImportFile nextImport: result) {
            nextImport.setStatus("READ");
            importFileRepository.save(nextImport);
        }

        return importFileRepository.findAllByOrderByIdAsc();
    }
}
