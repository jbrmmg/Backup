package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Getter
@JsonSerialize(using = ImportFileSummaryDTO.ImportFileSummarySerializer.class)
public class ImportFileSummaryDTO {
    private int totalPreImportFiles;
    private int totalImportFiles;
    private int totalPostImportFiles;
    @Setter
    private int queued;
    private final Map<FileProcessingStepType, ImportFileSummaryStepDTO> counts;

    public static class ImportFileSummarySerializer extends JsonSerializer<ImportFileSummaryDTO> {
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

    public ImportFileSummaryDTO() {
        counts = new EnumMap<>(FileProcessingStepType.class);
        totalPreImportFiles = 0;
        totalImportFiles = 0;
        totalPostImportFiles = 0;
        queued = 0;

        for(FileProcessingStepType type : FileProcessingStepType.values()){
            counts.put(type,new ImportFileSummaryStepDTO(type));
        }
    }

    public void incrementTotalPreImportFiles(){
        totalPreImportFiles++;
    }

    public void incrementTotalImportFiles(){
        totalImportFiles++;
    }

    public void incrementTotalPostImportFiles(){
        totalPostImportFiles++;
    }

    public void incrementStepCount(FileProcessingStepType type, TrafficLightType status) {
        counts.get(type).increment(status);
    }
}
