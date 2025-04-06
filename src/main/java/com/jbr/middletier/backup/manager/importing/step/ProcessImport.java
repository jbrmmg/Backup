package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import com.jbr.middletier.backup.manager.importing.step.process.ProcessBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ProcessImport extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessImport.class);

    private final List<ProcessBase> processors;

    @Autowired
    public ProcessImport(ImportFileRepository importFileRepository,
                            List<ProcessBase> processors,
                            ImportSourceManager importSourceManager) {
        super(importFileRepository, importSourceManager);
        this.processors = processors;
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile dbRecord) {
        return false;
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_PROCESS_IMPORT;
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        try {
            LOG.info("Perform the import for {}", file.getImportName());

            if (file.getStatus() == null) {
                return TrafficLightType.TL_GREEN;
            }

            // Translate the status.
            ImportFileStatusType status = ImportFileStatusType.getFileStatusType(file.getStatus());

            // Find the processor for this status.
            for(ProcessBase nextProcessor: this.processors) {
                if(nextProcessor.getType().equals(status)) {
                    return nextProcessor.process(file);
                }
            }

            LOG.info("Status {} - nothing to do for this.", file.getStatus());
        } catch (Exception e) {
            // Failed to perform the process step.
            LOG.warn("Failed to perform the process step for {}", file.getFilename());
        }

        return TrafficLightType.TL_RED;
    }
}
