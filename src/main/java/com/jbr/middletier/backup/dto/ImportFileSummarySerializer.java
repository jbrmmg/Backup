package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;

import java.io.IOException;
import java.util.Map;

public class ImportFileSummarySerializer extends JsonSerializer<ImportFileSummaryDTO> {
    @Override
    public void serialize(ImportFileSummaryDTO summary, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("PreImport", summary.getTotalPreImportFiles());
        jsonGenerator.writeNumberField("Import", summary.getTotalImportFiles());
        jsonGenerator.writeNumberField("PostImport", summary.getTotalPostImportFiles());
        jsonGenerator.writeNumberField("Queued", summary.getQueued());

        for(Map.Entry<FileProcessingStepType, ImportFileSummaryStepDTO> next : summary.getCounts().entrySet()) {
            // Write the next count.
            jsonGenerator.writeObjectFieldStart(FileProcessingStepType.getJsonName(next.getKey()));

            ImportFileSummaryStepDTO step = next.getValue();

            for(TrafficLightType nextStatus : TrafficLightType.values()) {
                jsonGenerator.writeNumberField(TrafficLightType.getTextValue(nextStatus), step.getCount(nextStatus));
            }

            jsonGenerator.writeEndObject();
        }

        jsonGenerator.writeEndObject();
    }
}
