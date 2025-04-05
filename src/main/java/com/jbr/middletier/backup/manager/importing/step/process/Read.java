package com.jbr.middletier.backup.manager.importing.step.process;

import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Read extends ProcessBase {
    private static final Logger LOG = LoggerFactory.getLogger(Read.class);
    private final Map<FileProcessingStepType, List<TrafficLightType>> requiredStepStatus;

    @Autowired
    protected Read(ImportSourceManager importSourceManager,
                    AssociatedFileDataManager associatedFileDataManager) {
        super(ImportFileStatusType.IFS_READ, associatedFileDataManager, importSourceManager);

        this.requiredStepStatus = new HashMap<>();
        this.requiredStepStatus.put(FileProcessingStepType.FPS_READ_PREIMPORT_FILE,getMustBeGreen());
    }

    @Override
    public TrafficLightType process(PreImportFileDTO file) throws ImportProcessException {
        validateStepStatus(file, requiredStepStatus);

        LOG.info("Read status check successful.");
        return TrafficLightType.TL_GREEN;
    }
}
