package com.jbr.middletier.backup.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by jason on 11/02/17.
 */

@Entity
@Table(name="backup")
public class Backup implements Comparable<Backup>{
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

    @Column(name="time")
    private long time;

    protected Backup() { }

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

    public long getTime() {
        return time;
    }

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
