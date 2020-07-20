package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.ImportRequest;
import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.manager.DriveManager;
import com.jbr.middletier.backup.manager.ImportManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jbr/int/backup")
public class ImportController {
    final static private Logger LOG = LoggerFactory.getLogger(ActionController.class);

    final private ImportManager importManager;
    final private ImportFileRepository importFileRepository;

    @Contract(pure = true)
    @Autowired
    public ImportController(ImportManager importManager,
                            ImportFileRepository importFileRepository ) {
        this.importManager = importManager;
        this.importFileRepository = importFileRepository;
    }

    @RequestMapping(path="/import", method= RequestMethod.POST)
    public @ResponseBody OkStatus importPhotoDirectory(@NotNull @RequestBody ImportRequest importRequest) throws Exception {
        LOG.info("Import - " + importRequest.getPath());

        importManager.importPhoto(importRequest);

        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/import", method= RequestMethod.DELETE)
    public @ResponseBody OkStatus removeEntries() {
        LOG.info("Remove entries from import table");

        importManager.removeEntries();

        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/importprocess", method= RequestMethod.POST)
    public @ResponseBody OkStatus importPhotoProcess() throws Exception {
        LOG.info("Import - process");

        importManager.importPhotoProcess();

        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/importfiles", method= RequestMethod.GET)
    public @ResponseBody Iterable<ImportFile> getImportFiles() {
        LOG.info("Get the import files.");

        return importFileRepository.findAll();
    }

    @RequestMapping(path="/importfiles", method= RequestMethod.PUT)
    public @ResponseBody Iterable<ImportFile> resetFiles() {
        LOG.info("Get the import files.");

        Iterable<ImportFile> result = importFileRepository.findAll();

        for(ImportFile nextImport: result) {
            nextImport.setStatus("READ");
            importFileRepository.save(nextImport);
        }

        return importFileRepository.findAll();
    }
}
