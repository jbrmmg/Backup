package com.jbr.middletier.backup.manager.importing.step.process;

import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RemoveActivePhoto extends ProcessBase {
    private static final Logger LOG = LoggerFactory.getLogger(RemoveActivePhoto.class);
    private final Map<FileProcessingStepType, List<TrafficLightType>> requiredStepStatus;

    @Autowired
    protected RemoveActivePhoto(AssociatedFileDataManager associatedFileDataManager) {
        super(ImportFileStatusType.IFS_REMOVE_ACTIVE_PHOTO,associatedFileDataManager);

        this.requiredStepStatus = new HashMap<>();
        this.requiredStepStatus.put(FileProcessingStepType.FPS_READ_PREIMPORT_FILE,getMustBeGreen());
        this.requiredStepStatus.put(FileProcessingStepType.FPS_GATHER_META_DATA,getMustBeGreen());
        this.requiredStepStatus.put(FileProcessingStepType.FPS_CHECK_ACTIVE_PHOTO_FILE,getMustBeRed());
    }

    @Override
    public TrafficLightType process(PreImportFileDTO file) throws ImportProcessException {
        try {
            // Verify that the states of this file are correct - if they are remove the files from all imports.
            validateStepStatus(file, requiredStepStatus);

            // Perform the action
            LOG.info("About to remove active photo {} from all import directories.", file.getFilename());

            deleteFile(file);
            return TrafficLightType.TL_GREEN;
        } catch (IOException e) {
            throw new  ImportProcessException("IO Exception when removing active photo", e);
        }
    }
}
