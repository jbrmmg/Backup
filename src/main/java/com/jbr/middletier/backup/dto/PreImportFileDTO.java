package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jbr.middletier.backup.data.MD5;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.step.StepStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Optional;

public class PreImportFileDTO extends ImportFileDTO {
    @Setter
    @Getter
    private volatile LocalDateTime updateTime;
    @Setter
    @Getter
    private volatile String destination;
    @Setter
    @Getter
    private volatile Double duration;
    @Setter
    @Getter
    private volatile String importName;
    @Setter
    @Getter
    private volatile LocalDateTime importDate;
    @Setter
    @Getter
    private volatile Long importSize;
    @Getter
    private volatile String importMd5;
    @Setter
    @Getter
    private volatile boolean errorInPostImport;
    @Setter
    @Getter
    private volatile boolean errorInImport;
    @Setter
    @Getter
    private volatile boolean inDatabase;
    @Setter
    @Getter
    private volatile boolean inImport;
    @Setter
    @Getter
    private volatile boolean inPostImport;
    @Getter
    private final StepStatus stepStatus;
    private final boolean stopMarker;

    public PreImportFileDTO() {
        this.inDatabase = false;
        this.inImport = false;
        this.inPostImport = false;
        this.stepStatus = new StepStatus();
        this.stopMarker = false;
    }

    public PreImportFileDTO(boolean stopMarker) {
        if(stopMarker){
            this.stepStatus = new StepStatus();
            this.stopMarker = true;
        } else {
            this.inDatabase = false;
            this.inImport = false;
            this.inPostImport = false;
            this.stepStatus = new StepStatus();
            this.stopMarker = false;
        }
    }

    @JsonIgnore
    public FileProcessingStepType getNextUnknownStep() {
        return this.stepStatus.getNextUnknownStep();
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

    @JsonIgnore
    public boolean isStopMarker() {
        return stopMarker;
    }

    public void setImportMd5(MD5 md5) { this.importMd5 = md5 != null ? md5.toString() : null; }

    public Optional<MD5> getImportMd5Optional() { return this.importMd5 == null ? Optional.empty() : Optional.of(new MD5(this.importMd5)); }
}
