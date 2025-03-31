package com.jbr.middletier.backup.manager.importing.step.process;

import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.data.ImportSource;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.jbr.middletier.backup.manager.importing.ImportManager.RECIPE_FILE_DESTINATION;

@Component
public class ImportFile extends ProcessBase {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFile.class);
    private final Map<FileProcessingStepType, List<TrafficLightType>> requiredStepStatus;

    @Autowired
    protected ImportFile(AssociatedFileDataManager associatedFileDataManager) {
        super(ImportFileStatusType.IFS_IMPORT_FILE, associatedFileDataManager);

        this.requiredStepStatus = new HashMap<>();
        this.requiredStepStatus.put(FileProcessingStepType.FPS_READ_PREIMPORT_FILE,getMustBeGreen());
        this.requiredStepStatus.put(FileProcessingStepType.FPS_GATHER_META_DATA,getMustBeGreen());
        this.requiredStepStatus.put(FileProcessingStepType.FPS_CHECK_DUPLICATE_FILE,getMustBeGreen());
        this.requiredStepStatus.put(FileProcessingStepType.FPS_CHECK_FILE_CONFIRMED_IMPORTED,getMustBeGreen());
        this.requiredStepStatus.put(FileProcessingStepType.FPS_CHECK_FILE_IGNORED,getMustNotBeRed());
    }

    @Override
    public TrafficLightType process(PreImportFileDTO file) throws ImportProcessException {
        validateStepStatus(file, requiredStepStatus);

        // File must have a destination.
        if(file.getDestination() == null || file.getDestination().isEmpty()){
            throw new ImportProcessException("Destination of " + file.getFilename() + " is empty");
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
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy");
            SimpleDateFormat sdf2 = new SimpleDateFormat("MMMM");

            destinationFilename += "/" + sdf1.format(file.getImportDate());
            destinationFilename += "/" + sdf2.format(file.getImportDate());
            destinationFilename += "/" + file.getDestination();
        }

        LOG.info("Moving {} to {}", file.getFilename(), destinationFilename);
        /*
          fileSystem.createDirectory(new File(newFilename).toPath());

        newFilename += "/" + path.getFileName();

        result.increment(ImportDataDTO.ImportDataCountType.IMPORTED);
        fileSystem.moveFile(path.toFile(), new File(newFilename), result);
         */

        return TrafficLightType.TL_GREEN;
    }
}
