package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;

import java.util.HashMap;
import java.util.Map;

public class ImportFileSummaryStepDTO {
    private final FileProcessingStepType type;
    private final Map<TrafficLightType,Integer> counts;

    public ImportFileSummaryStepDTO(FileProcessingStepType type) {
        this.type = type;
        this.counts = new HashMap<>();

        for(TrafficLightType trafficLightType : TrafficLightType.values()){
            counts.put(trafficLightType,0);
        }
    }

    public FileProcessingStepType getType() {
        return this.type;
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
