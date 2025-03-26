package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    private void removeImported(PreImportFileDTO file) {
        // Verify that the states of this file are correct - if they are remove the files from all imports.
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        LOG.info("Perform the import for {}", file.getImportName());

        if(file.getStatus() == null) {
            return TrafficLightType.TL_RED;
        }

        // Check the status to see if there is a step to perform.
        switch(file.getStatus()) {
            case "REMOVE_IMPORTED":
                LOG.info("Processing a remove imported status");
                removeImported(file);
                return TrafficLightType.TL_GREEN;

            case "READ":
                LOG.info("Status is read - nothing to do at this time.");
                break;

            default:
                LOG.info("Status {} - nothing to do for this.", file.getStatus());
        }

        return TrafficLightType.TL_RED;
    }
}
