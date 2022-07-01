package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.ImportRequest;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.dto.ImportDataDTO;
import com.jbr.middletier.backup.dto.ImportFileDTO;
import com.jbr.middletier.backup.exception.ImportRequestException;
import com.jbr.middletier.backup.manager.ImportManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparing;

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
    public @ResponseBody List<GatherDataDTO> removeEntries() {
        LOG.info("Remove entries from import table");

        return importManager.removeEntries();
    }

    @PostMapping(path="/importprocess")
    public @ResponseBody List<ImportDataDTO> importPhotoProcess() throws ImportRequestException {
        LOG.info("Import - process");

        return importManager.importPhotoProcess();
    }

    private List<ImportFileDTO> getExternalList(Iterable<ImportFile> list) {
        List<ImportFileDTO> result = new ArrayList<>();
        for(ImportFile nextFile: list) {
            result.add(new ImportFileDTO(nextFile));
        }

        result.sort(comparing(ImportFileDTO::getFilename));

        return result;
    }

    @GetMapping(path="/importfiles")
    public @ResponseBody List<ImportFileDTO> getImportFiles() {
        LOG.info("Get the import files.");

        return getExternalList(importManager.findImportFiles());
    }

    @PutMapping(path="/importfiles")
    public @ResponseBody List<ImportFileDTO> resetFiles() {
        LOG.info("Get the import files.");

        return getExternalList(importManager.resetFiles());
    }
}
