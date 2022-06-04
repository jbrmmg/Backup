package com.jbr.middletier.backup.dto;

public class SyncDataDTO {
    private int syncId;
    private int filesCopied;
    private int filesForDelete;
    private boolean failed;

    public SyncDataDTO() {
        this.syncId = -1;
        this.filesCopied = 0;
        this.filesForDelete = 0;
        this.failed = false;
    }

    public void setSyncId(int syncId) {
        this.syncId = syncId;
    }

    public int getSyncId() {
        return this.syncId;
    }

    public void incrementFilesCopied() {
        this.filesCopied++;
    }

    public int getFilesCopied() {
        return this.filesCopied;
    }

    public void incrementFilesForDelete() {
        this.filesForDelete++;
    }

    public int getFilesForDelete() {
        return this.filesForDelete;
    }

    public void setFailed() {
        this.failed = true;
    }

    public boolean getFailed() {
        return this.failed;
    }
}
