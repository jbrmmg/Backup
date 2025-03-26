package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.manager.importing.ImportManager;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.List;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/jbr/int/backup")
public class ImportController {
    private static final Logger LOG = LoggerFactory.getLogger(ImportController.class);

    private final ImportManager importManager;
    private final Flux<ServerSentEvent<List<PreImportFileDTO>>> updateNotifier;

    @Contract(pure = true)
    @Autowired
    public ImportController(ImportManager importManager) {
        this.importManager = importManager;
        this.updateNotifier = Flux.interval(Duration.ofSeconds(2))
                .map(this::checkFileUpdates);
    }

    private ServerSentEvent<List<PreImportFileDTO>> checkFileUpdates(long unused) {
        // Return the update information.
        return ServerSentEvent.<List<PreImportFileDTO>> builder()
                .data(importManager.getUpdates())
                .build();
    }

    @GetMapping(path = "/process-imports")
    public ImportFileDTO getFile(@RequestParam Integer id) throws InvalidFileIdException {
        LOG.info("Process the import files that have a destination.");

        //TODO - different method name
        return importManager.externalFindImportFile(id);
    }

    @DeleteMapping(path = "/delete-ignored")
    public String deleteIgnoredFiles() {
        LOG.info("Delete any files that are ignored.");

        if(importManager.removeIgnored()) {
            return "OK";
        }

        return "FAILED";
    }

    @GetMapping(path = "/import-files")
    public List<PreImportFileDTO> getImportFiles(@RequestParam Integer limit) {
        LOG.info("Get the pre import files.");

        return importManager.getImportFiles(limit != null ? limit : 0);
    }

    @GetMapping(path = "/import-file")
    public PreImportFileDTO getImportFile(@RequestParam String name) {
        LOG.info("Get the specified file.");

        return importManager.getImportFile(name);
    }

    @DeleteMapping(path = "/delete-confirmed-imports")
    public String deleteConfrimedImports() {
        LOG.info("Remove any files that are already imported.");

        if(importManager.removeDuplicates()) {
            return "OK";
        }

        return "FAILED";
    }

    @PostMapping(path = "/un-ignore-file")
    public String unIgnoreFile(@RequestBody String filename) {
        LOG.info("remove file from ignore list.");

        if(importManager.unIgnoreSelectedFile(filename)) {
            return "OK";
        }

        return "FAILED";
    }

    @PostMapping(path = "/ignore-file")
    public String ignoreFile(@RequestBody String filename) {
        LOG.info("Ignore the file.");

        if(importManager.ignoreSelectedFile(filename)) {
            return "OK";
        }

        return "FAILED";
    }

    @PostMapping(path = "/recipe-file")
    public String recipeFile(@RequestBody String filename) {
        LOG.info("Import the file as a recipe file.");

        if(importManager.recipeFile(filename)) {
            return "OK";
        }

        return "FAILED";
    }

    @GetMapping(path="/file-updates",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<List<PreImportFileDTO>>> fileUpdate() {
        try {
            return this.updateNotifier;
        } catch (Exception e) {
            LOG.info("Exception");
        }

        return null;
    }

    @GetMapping(path="/import-image",produces= MediaType.IMAGE_JPEG_VALUE)
    public byte[] getFileImage(@RequestParam String name) {
        return importManager.getFileContent(name);
    }

    @GetMapping(path="/import-video",produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] getFileVideo(@RequestParam String name) {
        return importManager.getFileContent(name);
    }

    @DeleteMapping(path="/clear-import-data")
    public String clearImportData() {
        LOG.info("Clear the import data.");

        if(importManager.clearImportData()) {
            return "OK";
        }

        return "FAILED";
    }

    @DeleteMapping(path="/clear-cache")
    public String clearCache() {
        LOG.info("Clear the cached data.");

        if(importManager.clearCacheData()) {
            return "OK";
        }

        return "FAILED";
    }
}
