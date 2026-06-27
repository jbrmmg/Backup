package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.manager.importing.ImportManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/import")
@Tag(name = "Import", description = "Photo and media import pipeline")
public class ImportController {
    private static final Logger LOG = LoggerFactory.getLogger(ImportController.class);

    private static final String OK =  "OK";
    private static final String FAILED = "FAILED";
    
    private final ImportManager importManager;
    private final Flux<ServerSentEvent<List<PreImportFileDTO>>> updateNotifier;
    private final Flux<ServerSentEvent<ImportFileSummaryDTO>> updateSummaryNotifier;

    @Contract(pure = true)
    @Autowired
    public ImportController(ImportManager importManager) {
        this.importManager = importManager;
        this.updateNotifier = Flux.interval(Duration.ofSeconds(2))
                .map(this::checkFileUpdates);
        this.updateSummaryNotifier = Flux.interval(Duration.ofSeconds(5))
                .map(this::checkSummaryUpdates);
    }

    private ServerSentEvent<List<PreImportFileDTO>> checkFileUpdates(long unused) {
        // Return the update information.
        return ServerSentEvent.<List<PreImportFileDTO>> builder()
                .data(importManager.getUpdates())
                .build();
    }

    private ServerSentEvent<ImportFileSummaryDTO> checkSummaryUpdates(long unused) {
        // Get the summary information.
        return ServerSentEvent.<ImportFileSummaryDTO>builder()
                .data(importManager.getImportSummary())
                .build();
    }

    @Operation(summary = "Delete a file from the import directory")
    @DeleteMapping(path = "/file")
    public String deleteImportFile(@RequestBody @Pattern(regexp="^[\\w\\-. ]+$",message="Filename cannot contain special characters") String filename) {
        LOG.info("Delete import file {}.", filename);

        if(importManager.deleteImportFile(filename)) {
            return OK;
        }

        return FAILED;
    }

    @Operation(summary = "Delete all files marked as ignored")
    @DeleteMapping(path = "/ignored")
    public String deleteIgnoredFiles() {
        LOG.info("Delete any files that are ignored.");

        if(importManager.deleteIgnored()) {
            return OK;
        }

        return FAILED;
    }

    @Operation(summary = "Delete Apple Live Photo companion files from the import directory")
    @DeleteMapping(path = "/active-photos")
    public String deleteActivePhotos() {
        LOG.info("Delete any files that are Apple active photos.");

        if(importManager.deleteActivePhotos()) {
            return OK;
        }

        return FAILED;
    }

    @GetMapping(path = "/files")
    public List<PreImportFileDTO> getImportFiles(@RequestParam("limit") Integer limit,
                                                 @RequestParam(name="page", required = false) Integer page,
                                                 @RequestParam(name="stepType", required = false) String stepType,
                                                 @RequestParam(name="status", required = false) String status) {
        LOG.info("Get the pre import files.");

        return importManager.getImportFiles(limit,page,stepType,status);
    }

    @Operation(summary = "Trigger the import pipeline to process queued photos")
    @PostMapping(path = "/photos")
    public String importPhotos() {
        LOG.info("Import the photos.");

        if(importManager.importPhotos()) {
            return OK;
        }

        return FAILED;
    }

    @GetMapping(path = "/summary")
    public ImportFileSummaryDTO getImportFileSummary() {
        LOG.info("Get import file summary");

        return importManager.getImportSummary();
    }

    @GetMapping(path = "/file")
    public PreImportFileDTO getImportFile(@RequestParam(name="name") String name) {
        LOG.info("Get the specified file.");

        return importManager.getImportFile(name);
    }

    @Operation(summary = "Remove files that have already been successfully imported")
    @DeleteMapping(path = "/confirmed")
    public String deleteConfirmedImports() {
        LOG.info("Remove any files that are already imported.");

        if(importManager.deleteConfirmedImports()) {
            return OK;
        }

        return FAILED;
    }

    @Operation(summary = "Remove the ignore flag from a specific file")
    @PostMapping(path = "/file/un-ignore")
    public String unIgnoreFile(@RequestBody String filename) {
        LOG.info("remove file from ignore list.");

        if(importManager.unIgnoreSelectedFile(filename)) {
            return OK;
        }

        return FAILED;
    }

    @PostMapping(path = "/file/ignore")
    public String ignoreFile(@RequestBody String filename) {
        LOG.info("Ignore the file.");

        if(importManager.ignoreSelectedFile(filename)) {
            return OK;
        }

        return FAILED;
    }

    @Operation(summary = "Clear ignore flags for all files currently in the import directory")
    @PostMapping(path = "/un-ignore")
    public String unIgnoreImport() {
        LOG.info("Remove the current files in the import directory from the ignore files.");

        if(importManager.unIgnoreAllImport()) {
            return OK;
        }

        return FAILED;
    }

    @Operation(summary = "Route a file to the recipe special destination instead of the standard import path")
    @PostMapping(path = "/file/recipe")
    public String recipeFile(@RequestBody String filename) {
        LOG.info("Import the file as a recipe file.");

        if(importManager.specialDestinationFile(filename, ImportManager.SpecialDestinationType.RECIPE)) {
            return OK;
        }

        return FAILED;
    }

    @Operation(summary = "Route a file to the backup special destination instead of the standard import path")
    @PostMapping(path = "/file/backup")
    public String justBackupFile(@RequestBody String filename) {
        LOG.info("Import the file as a backup file.");

        if(importManager.specialDestinationFile(filename, ImportManager.SpecialDestinationType.BACKUP)) {
            return OK;
        }

        return FAILED;
    }

    @PostMapping(path = "/file/destination")
    public String updateDestination(@RequestBody @Valid DestinationUpdateDTO destinationUpdate) {
        LOG.info("Update the destination.");

        if(importManager.updateDestination(destinationUpdate)) {
            return OK;
        }

        return FAILED;
    }

    @Operation(summary = "SSE stream — pushes updated import file list every 2 seconds")
    @GetMapping(path="/events/files",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<List<PreImportFileDTO>>> fileUpdate() {
        try {
            return this.updateNotifier;
        } catch (Exception e) {
            LOG.info("Exception in file update");
        }

        return null;
    }

    @Operation(summary = "SSE stream — pushes updated import summary every 5 seconds")
    @GetMapping(path="/events/summary",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ImportFileSummaryDTO>> summaryUpdate() {
        try {
            return this.updateSummaryNotifier;
        } catch (Exception e) {
            LOG.info("Exception in summary update");
        }

        return null;
    }

    @GetMapping(path="/file/image",produces= MediaType.IMAGE_JPEG_VALUE)
    public byte[] getFileImage(@RequestParam(name="name") String name) {
        return importManager.getFileContent(name);
    }

    @GetMapping(path="/file/video",produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] getFileVideo(@RequestParam(name="name") String name) {
        return importManager.getFileContent(name);
    }

    @Operation(summary = "Clear all import tracking records from the database")
    @DeleteMapping(path="/data")
    public String clearImportData() {
        LOG.info("Clear the import data.");

        if(importManager.clearImportData()) {
            return OK;
        }

        return FAILED;
    }

    @Operation(summary = "Clear the cached import file state (forces a re-scan on next poll)")
    @DeleteMapping(path="/cache")
    public String clearCache() {
        LOG.info("Clear the cached data.");

        if(importManager.clearCacheData()) {
            return OK;
        }

        return FAILED;
    }
}
