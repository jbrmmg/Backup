package com.jbr.middletier.backup.manager.importing.step;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;

import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = StepStatusSerializer.class)
public class StepStatus {
    Map<FileProcessingStepType, TrafficLightType> status;

    public StepStatus() {
        this.status = new HashMap<>();

        for(FileProcessingStepType step :  FileProcessingStepType.getStepsInOrder()) {
            this.status.put(step, TrafficLightType.TL_UNKNOWN);
        }
    }

    public FileProcessingStepType getNextUnknownStep() {
        for(FileProcessingStepType step: FileProcessingStepType.getStepsInOrder()) {
            TrafficLightType trafficLightType = status.get(step);
            if(trafficLightType == TrafficLightType.TL_UNKNOWN) {
                return step;
            }
        }

        return FileProcessingStepType.FPS_FINAL_UPDATE;
    }

    public TrafficLightType getStepStatus(FileProcessingStepType step) {
        return status.get(step);
    }

    public void setStepStatus(FileProcessingStepType step, TrafficLightType trafficLightType) {
        // Update the status of the step.
        this.status.put(step,trafficLightType);
    }
}
