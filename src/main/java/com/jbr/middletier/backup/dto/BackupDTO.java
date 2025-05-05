package com.jbr.middletier.backup.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
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

    public void setId(@NotNull String id) {
        this.id = id;
    }

    public void setType(@NotNull String type) {
        this.type = type;
    }
}
