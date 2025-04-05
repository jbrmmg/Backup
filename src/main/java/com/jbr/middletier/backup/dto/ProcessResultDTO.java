package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jbr.middletier.backup.jsonserialization.ProcessResultSerializer;

import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = ProcessResultSerializer.class)
public class ProcessResultDTO {
    private final int underlyingId;
    private boolean problems;
    private final Map<String,Integer> counts;

    protected void increment(String name) {
        if(counts.containsKey(name)) {
            counts.put(name, counts.get(name) + 1);
        }
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
