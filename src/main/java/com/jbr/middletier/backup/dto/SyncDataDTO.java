package com.jbr.middletier.backup.dto;

public class SyncDataDTO {
    private int syncId;
    private int filesCopied;
    private int directoriesCopied;
    private int filesDeleted;
    private int directoriesDeleted;
    private boolean failed;

    public SyncDataDTO() {
        this.syncId = -1;
        this.filesCopied = 0;
        this.directoriesCopied = 0;
        this.filesDeleted = 0;
        this.directoriesDeleted = 0;
        this.failed = false;
    }

    public void setSyncId(int syncId) {
        this.syncId = syncId;
    }

    public int getFilesCopied() {
        return filesCopied;
    }

    public int getDirectoriesCopied() {
        return directoriesCopied;
    }

    public int getFilesDeleted() {
        return filesDeleted;
    }

    public int getDirectoriesDeleted() {
        return directoriesDeleted;
    }

    public void incrementFilesCopied() {
        this.filesCopied++;
    }

    public void incrementDirectoriesCopied() {
        this.directoriesCopied++;
    }

    public void incrementFilesDeleted() {
        this.filesDeleted++;
    }

    public void incrementDirectoriesDeleted() {
        this.directoriesDeleted++;
    }

    public void setFailed() {
        this.failed = true;
    }

    public boolean getFailed() {
        return this.failed;
    }
}
