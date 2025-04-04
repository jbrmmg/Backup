package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FinalStep extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(FinalStep.class);

    @Autowired
    protected FinalStep(ImportFileRepository importFileRepository, AssociatedFileDataManager associatedFileDataManager) {
        super(importFileRepository, associatedFileDataManager);
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile record) {
        return false;
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_FINAL_UPDATE;
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        try {
            // Just wait for 10 seconds.
            LOG.info("Performing Final Step {}", file.getFilename());
            Thread.sleep(1000);
            LOG.info("Performed Final Step {}", file.getFilename());
        } catch (InterruptedException e) {
            LOG.info("Final step aborted.");
            Thread.currentThread().interrupt();
        }

        return TrafficLightType.TL_GREEN;
    }
}
