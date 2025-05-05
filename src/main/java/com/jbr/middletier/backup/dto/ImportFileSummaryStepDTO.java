package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;

public class ImportFileSummaryStepDTO {
    @Getter
    private final FileProcessingStepType type;
    private final Map<TrafficLightType,Integer> counts;

    public ImportFileSummaryStepDTO(FileProcessingStepType type) {
        this.type = type;
        this.counts = new EnumMap<>(TrafficLightType.class);

        for(TrafficLightType trafficLightType : TrafficLightType.values()){
            counts.put(trafficLightType,0);
        }
    }

    public void increment(TrafficLightType trafficLightType) {
        Integer count = this.counts.get(trafficLightType);
        count += 1;
        this.counts.put(trafficLightType,count);
    }

    public int getCount(TrafficLightType type) {
        if(this.counts.containsKey(type)) {
            return this.counts.get(type);
        }

        return 0;
    }
}
