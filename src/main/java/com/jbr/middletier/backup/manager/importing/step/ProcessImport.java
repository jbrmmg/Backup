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

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        LOG.info("Perform the import for {}", file.getImportName());
        return TrafficLightType.TL_RED;
    }
}
