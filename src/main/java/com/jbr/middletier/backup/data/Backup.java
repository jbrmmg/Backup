package com.jbr.middletier.backup.data;

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
public class Backup implements Comparable<Backup>{
    @Id
    @Column(name="id")
    @NotNull
    private String id;

    @Column(name="type")
    @NotNull
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

    protected Backup() { }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) { this.type = type; }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) { this.directory = directory; }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) { this.artifact = artifact; }

    public String getBackupName() {
        return backupName;
    }

    public void setBackupName(String backupName) { this.backupName = backupName; }

    public String getFileName() { return fileName; }

    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getTime() {
        return time;
    }

    public void setTime(long time) { this.time = time; }

    @SuppressWarnings("NullableProblems")
    public int compareTo(Backup compareBackup) {
        if(compareBackup == null){
            throw new IllegalArgumentException("RHS cannot be null.");
        }

        //ascending order
        long compare = this.time - compareBackup.time;
        return (int)compare;
    }
}
