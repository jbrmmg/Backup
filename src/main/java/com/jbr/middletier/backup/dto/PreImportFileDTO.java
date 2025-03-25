package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.step.StepStatus;
import java.time.LocalDateTime;

public class PreImportFileDTO extends ImportFileDTO {
    private LocalDateTime updateTime;
    private String destination;
    private Double duration;
    private String importName;
    private LocalDateTime importDate;
    private Long importSize;
    private String importMd5;
    private boolean errorInPostImport;
    private boolean errorInImport;
    private boolean processed;
    private boolean inDatabase;
    private boolean inImport;
    private boolean inPostImport;
    private final StepStatus stepStatus;

    public PreImportFileDTO() {
        this.inDatabase = false;
        this.inImport = false;
        this.inPostImport = false;
        this.stepStatus = new StepStatus();
    }

    @JsonIgnore
    public FileProcessingStepType getNextUnknownStep() {
        return this.stepStatus.getNextUnknownStep();
    }

    public StepStatus getStepStatus() {
        return this.stepStatus;
    }

    @JsonIgnore
    public TrafficLightType getStepStatus(FileProcessingStepType step) {
        return this.stepStatus.getStepStatus(step);
    }

    public void setStepStatus(FileProcessingStepType step, TrafficLightType status) {
        // Update the status of the step.
        this.stepStatus.setStepStatus(step,status);
        this.updateTime = LocalDateTime.now();
    }

    public boolean updatedSince(LocalDateTime time) {
        if(this.updateTime == null){
            return false;
        }

        return this.updateTime.isAfter(time);
    }

    public boolean isInDatabase() {
        return inDatabase;
    }

    public void setInDatabase(boolean inDatabase) {
        this.inDatabase = inDatabase;
    }

    public boolean isInImport() {
        return inImport;
    }

    public void setInImport(boolean inImport) {
        this.inImport = inImport;
    }

    public boolean isInPostImport() {
        return inPostImport;
    }

    public void setInPostImport(boolean inPostImport) {
        this.inPostImport = inPostImport;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public String getImportName() {
        return importName;
    }

    public void setImportName(String importFilename) {
        this.importName = importFilename;
    }

    public LocalDateTime getImportDate() {
        return importDate;
    }

    public void setImportDate(LocalDateTime importDate) {
        this.importDate = importDate;
    }

    public Long getImportSize() {
        return importSize;
    }

    public void setImportSize(Long importSize) {
        this.importSize = importSize;
    }

    public String getImportMd5() {
        return importMd5;
    }

    public void setImportMd5(String importMd5) {
        this.importMd5 = importMd5;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        if(processed == null){
            this.processed = false;
            return;
        }

        this.processed = processed;
    }

    public boolean isErrorInPostImport() {
        return errorInPostImport;
    }

    public void setErrorInPostImport(boolean errorInPostImport) {
        this.errorInPostImport = errorInPostImport;
    }

    public boolean isErrorInImport() {
        return errorInImport;
    }

    public void setErrorInImport(boolean errorInImport) {
        this.errorInImport = errorInImport;
    }
}
