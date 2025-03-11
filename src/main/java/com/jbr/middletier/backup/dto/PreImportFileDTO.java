package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.TrafficLightType;

public class PreImportFileDTO extends ImportFileDTO {
    private TrafficLightType ignored;
    private TrafficLightType immediateImported;
    private TrafficLightType imported;
    private TrafficLightType duplicated;

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
}
