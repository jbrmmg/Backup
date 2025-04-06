package com.jbr.middletier.backup.manager.importing.step;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@JsonSerialize(using = StepStatus.StepStatusSerializer.class)
public class StepStatus {
    private final Map<FileProcessingStepType, TrafficLightType> status;

    public static class StepStatusSerializer extends JsonSerializer<StepStatus> {
        @Override
        public void serialize(StepStatus stepStatus, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            for(FileProcessingStepType step :  FileProcessingStepType.getStepsInOrder()) {
                TrafficLightType status = stepStatus.getStepStatus(step);
                jsonGenerator.writeStringField(FileProcessingStepType.getJsonName(step),
                        TrafficLightType.getTextValue(status));
            }
            jsonGenerator.writeEndObject();
        }
    }

    public StepStatus() {
        this.status = new EnumMap<>(FileProcessingStepType.class);

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
