package com.jbr.middletier.backup.dto;

import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
public class BackupDTO {
    private String id;
    private String type;
    private String directory;
    private String artifact;
    private String backupName;
    private String fileName;
    private long time;

    public BackupDTO() {
        setId("");
        setType("");
    }

    public BackupDTO(String id, String type) {
        setId(id);
        setType(type);
    }

    public String getId() {
        return id;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(@NotNull String type) {
        this.type = type;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public String getBackupName() {
        return backupName;
    }

    public void setBackupName(String backupName) {
        this.backupName = backupName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
