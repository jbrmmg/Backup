package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = ProcessResultSerializer.class)
public class ProcessResultDTO {
    private final int underlyingId;
    private boolean problems;
    private final Map<String,Count> counts;

    private static class Count{
        int countValue;

        public Count(int initial) {
            this.countValue = initial;
        }

        public void increment() {
            this.countValue++;
        }

        public int get() {
            return this.countValue;
        }
    }

    protected void increment(String name) {
        if(counts.containsKey(name)) {
            counts.get(name).increment();
            return;
        }

        counts.put(name, new Count(1));
    }

    protected int getCount(String name) {
        if(counts.containsKey(name)) {
            return counts.get(name).get();
        }

        counts.put(name, new Count(0));
        return 0;
    }

    protected ProcessResultDTO(int underlyingId) {
        this.underlyingId = underlyingId;
        this.problems = false;
        this.counts = new HashMap<>();
    }

    public Map<String,Count> getCounts() {
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
