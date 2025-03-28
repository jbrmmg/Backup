package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
public class ProcessImport extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessImport.class);

    protected ProcessImport(ImportFileRepository importFileRepository, AssociatedFileDataManager associatedFileDataManager) {
        super(importFileRepository, associatedFileDataManager);
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile record) {
        return false;
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_PROCESS_IMPORT;
    }

    private void deleteFile(PreImportFileDTO file) throws IOException {
        // Remove from all import directories PostImport, Import then PreImport.
        File postImportFile = getPostImportFilename(file);
        if(postImportFile.exists()) {
            Files.delete(postImportFile.toPath());
        }

        File importFile = getImportFilename(file.getImportName());
        if(importFile.exists()) {
            Files.delete(importFile.toPath());
        }

        File preImportFile = getPreImportFilename(file);
        if(preImportFile.exists()) {
            Files.delete(preImportFile.toPath());
        }

        // Mark this file as deleted.
        file.setStatus(ImportFileStatusType.IFS_REMOVED);
    }

    private void removeActivePhoto(PreImportFileDTO file) throws IOException {
        // Verify that the states of this file are correct - if they are then remove.
        for(FileProcessingStepType step: FileProcessingStepType.getStepsInOrder()) {
            TrafficLightType status = file.getStepStatus(step);

            switch (step) {
                case FPS_READ_PREIMPORT_FILE:
                case FPS_GATHER_META_DATA:
                    // Must be green.
                    if(status != TrafficLightType.TL_GREEN) {
                        LOG.warn("Remove Active Photo file process cannot be used if step {} is not green. {}", step, file.getFilename());
                        throw new IllegalStateException("Invalid state for remove imported process.");
                    }
                    break;

                case FPS_CHECK_ACTIVE_PHOTO_FILE:
                    // Must be red.
                    if(status != TrafficLightType.TL_RED) {
                        LOG.warn("Remove Active Photo file process Cannot be used on a file not marked as ignored. {}", file.getFilename());
                        throw new IllegalStateException("Invalid state for remove imported process.");
                    }
            }
        }

        LOG.info("About to remove active photo {} from all import directories.", file.getFilename());

        deleteFile(file);
    }

    private void removeIgnored(PreImportFileDTO file) throws IOException {
        // Verify that the states of this file are correct - if they are then remove.
        // Status of the file must be as follows:
        for(FileProcessingStepType step: FileProcessingStepType.getStepsInOrder()) {
            TrafficLightType status = file.getStepStatus(step);

            switch (step) {
                case FPS_READ_PREIMPORT_FILE:
                case FPS_GATHER_META_DATA:
                    // Must be green.
                    if(status != TrafficLightType.TL_GREEN) {
                        LOG.warn("Remove Ignored file process cannot be used if step {} is not green. {}", step, file.getFilename());
                        throw new IllegalStateException("Invalid state for remove imported process.");
                    }
                    break;

                case FPS_CHECK_FILE_IGNORED:
                    // Must be red.
                    if(status != TrafficLightType.TL_RED) {
                        LOG.warn("Remove Ignored file process Cannot be used on a file not marked as ignored. {}", file.getFilename());
                        throw new IllegalStateException("Invalid state for remove imported process.");
                    }
            }
        }

        LOG.info("About to remove ignored {} from all import directories.", file.getFilename());

        deleteFile(file);
    }

    private void removeImported(PreImportFileDTO file) throws IOException {
        // Verify that the states of this file are correct - if they are remove the files from all imports.

        // Status of the file must be as follows:
        for(FileProcessingStepType step: FileProcessingStepType.getStepsInOrder()) {
            TrafficLightType status = file.getStepStatus(step);

            switch (step) {
                case FPS_READ_PREIMPORT_FILE:
                case FPS_GATHER_META_DATA:
                case FPS_CHECK_DUPLICATE_FILE:
                case FPS_CHECK_FILE_CONFIRMED_IMPORTED:
                    // Must be green.
                    if(status != TrafficLightType.TL_GREEN) {
                        LOG.warn("Remove Imported file process cannot be used if step {} is not green. {}", step, file.getFilename());
                        throw new IllegalStateException("Invalid state for remove imported process.");
                    }
                    break;

                case FPS_CHECK_FILE_IGNORED:
                    // Must not be red.
                    if(status == TrafficLightType.TL_RED) {
                        LOG.warn("Remove Imported file process Cannot be used on a file marked as ignored. {}", file.getFilename());
                        throw new IllegalStateException("Invalid state for remove imported process.");
                    }
            }
        }

        LOG.info("About to remove {} from all import directories.", file.getFilename());

        deleteFile(file);
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        try {
            LOG.info("Perform the import for {}", file.getImportName());

            if (file.getStatus() == null) {
                return TrafficLightType.TL_GREEN;
            }

            // Check the status to see if there is a step to perform.
            switch (file.getStatus()) {
                case "REMOVE_IMPORTED":
                    LOG.info("Processing a remove imported status");
                    removeImported(file);
                    return TrafficLightType.TL_GREEN;

                case "REMOVE_IGNORED":
                    LOG.info("Processing a remove ignored status");
                    removeIgnored(file);
                    return TrafficLightType.TL_GREEN;

                case "REMOVE_ACTIVE_PHOTO":
                    LOG.info("Processing a remove active photo status");
                    removeActivePhoto(file);
                    return TrafficLightType.TL_GREEN;

                case "READ":
                    LOG.info("Status is read - nothing to do at this time.");
                    return TrafficLightType.TL_GREEN;

                default:
                    LOG.info("Status {} - nothing to do for this.", file.getStatus());
            }
        } catch (Exception e) {
            // Failed to perform the process step.
            LOG.warn("Failed to perform the process step for {}", file.getFilename());
        }

        return TrafficLightType.TL_RED;
    }
}
