package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.dto.BackupDTO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Created by jason on 11/02/17.
 */

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

    public Backup(BackupDTO source) {
        setId(source.getId());
        setType(source.getType());
        update(source);
    }

    public void update(BackupDTO source) {
        setType(source.getType());
        setDirectory(source.getDirectory());

        this.artifact = source.getArtifact();
        this.backupName = source.getBackupName();
        this.fileName = source.getFileName();
        this.time = source.getTime();
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

    public void setId(@NotNull String id) { this.id = id; }

    public void setType(@NotNull String type) { this.type = type; }

    public void setDirectory(String directory) { this.directory = directory; }
}
