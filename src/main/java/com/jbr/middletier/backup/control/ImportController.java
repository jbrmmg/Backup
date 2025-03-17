package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.ImportRequestException;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.manager.importing.ImportManager;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
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

    @GetMapping(path = "/preimportfiles")
    public List<PreImportFileDTO> getPreImportFiles(@RequestParam Integer limit) {
        LOG.info("Get the pre import files.");

        return importManager.externalFindPreImportFiles(limit != null ? limit : 0);
    }

    @DeleteMapping(path = "/preimportfile")
    public String deletePreImportFile(@RequestBody String filename) {
        LOG.info("Delete pre import file - {}", filename);

        if(importManager.deletePreImportFile(filename)) {
            return "OK";
        }

        return "FAILED";
    }

    @PostMapping(path = "/removeignored")
    public String reimportFile() {
        LOG.info("Check the import directory and remove any that are ignored");

        if(importManager.removeIgnored()) {
            return "OK";
        }

        return "FAILED";
    }

    @PostMapping(path = "/importfiles")
    public String importFiles() {
        LOG.info("Import the files in the pre-import directory.");

        if(importManager.importFiles()) {
            return "OK";
        }

        return "FAILED";
    }

    @PostMapping(path = "/removeduplicates")
    public String removeduplicates() {
        LOG.info("Remove any files that are already imported.");

        if(importManager.removeDuplicates()) {
            return "OK";
        }

        return "FAILED";
    }

    @PostMapping(path = "/ignorefile")
    public String ignoreFile(@RequestBody String filename) {
        LOG.info("Ignore the file.");

        if(importManager.ignoreSelectedFile(filename)) {
            return "OK";
        }

        return "FAILED";
    }

    @PostMapping(path = "/recipefile")
    public String recipeFile(@RequestBody String filename) {
        LOG.info("Ignore the file.");

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
}
