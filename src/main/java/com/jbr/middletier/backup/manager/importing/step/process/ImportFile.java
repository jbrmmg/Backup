package com.jbr.middletier.backup.manager.importing.step.process;

import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.data.ImportSource;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dto.ImportProcessDTO;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.dto.ProcessResultDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.jbr.middletier.backup.manager.importing.ImportManager.RECIPE_FILE_DESTINATION;

@Component
public class ImportFile extends ProcessBase {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFile.class);
    private final Map<FileProcessingStepType, List<TrafficLightType>> requiredStepStatus;

    private final FileSystem fileSystem;

    @Autowired
    protected ImportFile(ImportSourceManager importSourceManager,
                         AssociatedFileDataManager associatedFileDataManager,
                         FileSystem fileSystem) {
        super(ImportFileStatusType.IFS_IMPORT_FILE, associatedFileDataManager, importSourceManager);

        this.fileSystem = fileSystem;

        this.requiredStepStatus = new EnumMap<>(FileProcessingStepType.class);
        this.requiredStepStatus.put(FileProcessingStepType.FPS_READ_PREIMPORT_FILE,getMustBeGreen());
        this.requiredStepStatus.put(FileProcessingStepType.FPS_GATHER_META_DATA,getMustBeGreen());
        this.requiredStepStatus.put(FileProcessingStepType.FPS_CHECK_FILE_CONFIRMED_IMPORTED,getMustNotBeGreen());
        this.requiredStepStatus.put(FileProcessingStepType.FPS_CHECK_FILE_IGNORED,getMustNotBeRed());
    }

    @Override
    public TrafficLightType process(PreImportFileDTO file) throws ImportProcessException {
        validateStepStatus(file, requiredStepStatus);

        // File must have a destination.
        if(file.getDestination() == null || file.getDestination().isEmpty()){
            throw new ImportProcessException("Destination of " + file.getFilename() + " is empty");
        }

        // File must not be in the post import directory.
        if(file.isInPostImport()) {
            throw new ImportProcessException(file.getFilename() + " cannot be in the post import directory.");
        }

        // Perform the import.
        LOG.info("Importing the file {}", file.getFilename());

        File importFile = getImportFilename(file.getFilename());
        File postImportFile = getPostImportFilename(file);

        if(!importFile.exists()){
            throw new ImportProcessException("The file " + file.getFilename() + " does not exist.");
        }

        // Get details of the import source.
        Optional<ImportSource> importSource = Optional.empty();
        for(ImportSource nextSource: associatedFileDataManager.findAllImportSource()) {
            importSource = Optional.of(nextSource);
        }

        if(importSource.isEmpty()){
            throw new ImportProcessException("Import source is invalid.");
        }

        Optional<Source> destination = associatedFileDataManager.findSourceIfExists(importSource.get().getDestination().getIdAndType().getId());
        if (destination.isEmpty()) {
            throw new ImportProcessException("Import destination is invalid.");
        }

        String destinationFilename = destination.get().getPath();

        if(file.getDestination().equalsIgnoreCase(RECIPE_FILE_DESTINATION)) {
            destinationFilename += "/0000/recipe";
        } else {
            DateTimeFormatter dtf1 = DateTimeFormatter.ofPattern("yyyy");
            DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("MMMM");

            destinationFilename += "/" + dtf1.format(file.getImportDate());
            destinationFilename += "/" + dtf2.format(file.getImportDate());
            destinationFilename += "/" + file.getDestination();
        }

        try {
            LOG.info("Moving {} to {}", file.getFilename(), destinationFilename);
            fileSystem.createDirectory(new File(destinationFilename).toPath());

            destinationFilename += "/" + file.getFilename();

            ProcessResultDTO copyResult = new ImportProcessDTO();
            fileSystem.copyFile(importFile, new File(destinationFilename), copyResult);
            if(copyResult.hasProblems()) {
                throw new ImportProcessException(file.getFilename() + " cannot be copied to destination " + destinationFilename + ".");
            }
            if(file.getImportDate() != null) {
                fileSystem.setFileFromLocalDateTime(new File(destinationFilename), file.getImportDate(), 0);
            }

            // Copy the input file to the post import directory.
            fileSystem.copyFile(importFile, postImportFile, copyResult);
            if(copyResult.hasProblems()) {
                throw new ImportProcessException(file.getFilename() + " cannot be copied to the post import directory.");
            }

            file.setInPostImport(true);
            file.setDestination(null);
            file.setStepStatus(FileProcessingStepType.FPS_CHECK_FILE_CONFIRMED_IMPORTED,TrafficLightType.TL_AMBER);
        } catch (IOException e) {
            throw new ImportProcessException("Could not move the file " + file.getFilename(), e);
        }

        return TrafficLightType.TL_GREEN;
    }
}
