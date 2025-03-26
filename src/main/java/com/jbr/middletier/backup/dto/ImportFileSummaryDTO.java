package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;

import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = ImportFileSummarySerializer.class)
public class ImportFileSummaryDTO {
    private int totalPreImportFiles;
    private int totalImportFiles;
    private int totalPostImportFiles;
    private final Map<FileProcessingStepType,ImportFIleSummaryStepDTO> counts;

    public ImportFileSummaryDTO() {
        counts = new HashMap<>();
        totalPreImportFiles = 0;
        totalImportFiles = 0;
        totalPostImportFiles = 0;

        for(FileProcessingStepType type : FileProcessingStepType.values()){
            counts.put(type,new ImportFIleSummaryStepDTO(type));
        }
    }

    public int getTotalPreImportFiles() {
        return totalPreImportFiles;
    }

    public int getTotalImportFiles() {
        return totalImportFiles;
    }

    public int getTotalPostImportFiles() {
        return totalPostImportFiles;
    }

    public Map<FileProcessingStepType,ImportFIleSummaryStepDTO> getCounts() {
        return counts;
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
