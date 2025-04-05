package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = ProcessResultDTO.ProcessResultSerializer.class)
public class ProcessResultDTO {
    private final int underlyingId;
    private boolean problems;
    private final Map<String,Integer> counts;

    public static class ProcessResultSerializer extends JsonSerializer<ProcessResultDTO> {
        @Override
        public void serialize(ProcessResultDTO processResultDTO, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField("underlyingId",processResultDTO.getUnderlyingId());
            jsonGenerator.writeBooleanField("failed", processResultDTO.hasProblems());
            for(String nextName: processResultDTO.getCounts().keySet()) {
                jsonGenerator.writeNumberField(nextName,processResultDTO.getCount(nextName));
            }
            jsonGenerator.writeEndObject();
        }
    }

    protected void increment(String name) {
        counts.computeIfPresent(name, (k, v) -> v + 1);
    }

    public int getCount(String name) {
        if(counts.containsKey(name)) {
            return counts.get(name);
        }

        counts.put(name, 0);
        return 0;
    }

    protected ProcessResultDTO(int underlyingId) {
        this.underlyingId = underlyingId;
        this.problems = false;
        this.counts = new HashMap<>();
    }

    public Map<String,Integer> getCounts() {
        return this.counts;
    }

    public void setProblems() {
        this.problems = true;
    }

    public boolean hasProblems() {
        return this.problems;
    }

    public int getUnderlyingId() {
        return this.underlyingId;
    }
}
