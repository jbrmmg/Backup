package com.jbr.middletier.backup.manager.importing.step;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;

import java.io.IOException;

public class StepStatusSerializer extends JsonSerializer<StepStatus> {
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
