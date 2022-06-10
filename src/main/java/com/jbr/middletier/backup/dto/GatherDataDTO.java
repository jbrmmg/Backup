package com.jbr.middletier.backup.dto;

public class GatherDataDTO {
    private int sourceId;
    private int filesInserted;
    private int directoriesInserted;
    private int filesRemoved;
    private int directoriesRemoved;
    private int deletes;
    private boolean problems;

    public GatherDataDTO(int sourceId) {
        this.sourceId = sourceId;
        this.filesInserted = 0;
        this.directoriesInserted = 0;
        this.filesRemoved = 0;
        this.directoriesRemoved = 0;
        this.deletes = 0;
        this.problems = false;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public int getFilesInserted() {
        return filesInserted;
    }

    public void incrementFilesInserted() {
        this.filesInserted++;
    }

    public int getDirectoriesInserted() {
        return directoriesInserted;
    }

    public void incrementDirectoriesInserted() {
        this.directoriesInserted++;
    }

    public int getFilesRemoved() {
        return filesRemoved;
    }

    public void incrementFilesRemoved() {
        this.filesRemoved++;
    }

    public int getDirectoriesRemoved() {
        return directoriesRemoved;
    }

    public void incrementDirectoriesRemoved() {
        this.directoriesRemoved++;
    }

    public int getDeletes() {
        return this.deletes;
    }

    public void incrementDeletes() {
        this.deletes++;
    }

    public void setProblems() {
        this.problems = true;
    }

    public boolean getProblems() {
        return this.problems;
    }
}
