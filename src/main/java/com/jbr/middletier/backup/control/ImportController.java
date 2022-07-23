package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.dto.ImportDataDTO;
import com.jbr.middletier.backup.dto.ImportFileDTO;
import com.jbr.middletier.backup.dto.ImportProcessDTO;
import com.jbr.middletier.backup.exception.ImportRequestException;
import com.jbr.middletier.backup.manager.ImportManager;
import org.jetbrains.annotations.Contract;
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
    public ImportController(ImportManager importManager) {
        this.importManager = importManager;
    }

    @PostMapping(path="/convert")
    public @ResponseBody List<ImportProcessDTO> processImports() {
        LOG.info("Convert files from pre import to import");

        return importManager.convertImportFiles();
    }

    @PostMapping(path="/import")
    public @ResponseBody List<GatherDataDTO> importPhotoDirectory() throws ImportRequestException, IOException {
        LOG.info("Import the files");

        return importManager.importPhoto();
    }

    @PostMapping(path="/importprocess")
    public @ResponseBody List<ImportDataDTO> importPhotoProcess() throws ImportRequestException {
        LOG.info("Process the import files.");

        return importManager.processImportFiles();
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
}
