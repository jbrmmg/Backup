package com.jbr.middletier.backup.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Created by jason on 11/02/17.
 */

@SuppressWarnings("unused")
@Entity
@Table(name="backup")
public class Backup {
    @Id
    @Column(name="id")
    private String id;

    @Column(name="type")
    private String type;

    @Column(name="directory")
    private String directory;

    @Column(name="artifact")
    private String artifact;

    @Column(name="backupname")
    private String backupName;

    @Column(name="filename")
    private String fileName;

    @Column(name="time")
    private long time;

    protected Backup() {
        this.id = "";
        this.type = "";
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDirectory() {
        return directory;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getBackupName() {
        return backupName;
    }

    public String getFileName() { return this.fileName; }

    public long getTime() { return this.time; }

    public void setTime(long time) {
        this.time = time;
    }

    public void setFileName(String filename) {this.fileName = filename; }

    public void setArtifact(String artifact) {this.artifact = artifact; }

    public void setBackupName(String backupName) {this.backupName = backupName; }

    public void setId(@NotNull String id) { this.id = id; }

    public void setType(@NotNull String type) { this.type = type; }

    public void setDirectory(String directory) { this.directory = directory; }
}
