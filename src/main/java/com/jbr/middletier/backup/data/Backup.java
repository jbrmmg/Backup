package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Created by jason on 11/02/17.
 */

@Getter
@Entity
@Table(name="backup")
public class Backup {
    @Id
    @Column(name="id")
    private String id;

    @Column(name="type")
    private String type;

    @Setter
    @Column(name="directory")
    private String directory;

    @Setter
    @Column(name="artifact")
    private String artifact;

    @Setter
    @Column(name="backupname")
    private String backupName;

    @Setter
    @Column(name="filename")
    private String fileName;

    @Setter
    @Column(name="time")
    private long time;

    protected Backup() {
        this.id = "";
        this.type = "";
    }

    public void setId(@NotNull String id) { this.id = id; }

    public void setType(@NotNull String type) { this.type = type; }
}
