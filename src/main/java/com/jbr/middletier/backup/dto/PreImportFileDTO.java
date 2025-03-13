package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.TrafficLightType;

import java.time.LocalDateTime;

public class PreImportFileDTO extends ImportFileDTO {
    private TrafficLightType ignored;
    private TrafficLightType immediateImported;
    private TrafficLightType imported;
    private TrafficLightType duplicated;
    private LocalDateTime updateTime;

    public TrafficLightType getIgnored() {
        return ignored;
    }

    public void setIgnored(TrafficLightType ignored) {
        this.ignored = ignored;
    }

    public TrafficLightType getImmediateImported() {
        return immediateImported;
    }

    public void setImmediateImported(TrafficLightType immediateImported) {
        this.immediateImported = immediateImported;
    }

    public TrafficLightType getImported() {
        return imported;
    }

    public void setImported(TrafficLightType imported) {
        this.imported = imported;
    }

    public TrafficLightType getDuplicated() {
        return duplicated;
    }

    public void setDuplicated(TrafficLightType duplicated) {
        this.duplicated = duplicated;
    }

    public void update() {
        this.updateTime = LocalDateTime.now();
    }

    public boolean updatedSince(LocalDateTime time) {
        if(this.updateTime == null){
            return false;
        }

        return this.updateTime.isAfter(time);
    }
}
