package com.jbr.middletier.backup.data;

import javax.persistence.*;
import java.time.LocalDateTime;

@SuppressWarnings("unused")
@Entity
@Table(name="log")
public class DbLog {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="type")
    private String type;

    @Column(name="date")
    private LocalDateTime date;

    @Column(name="message")
    private String message;

    @Column(name="fso")
    private Integer fso;

    @Column(name="backup")
    private String backup;

    public DbLog() {
        this(DbLogType.DLT_ERROR,"Unknown",null,null);
    }

    public DbLog(DbLogType type, String message, Integer fso, String backup) {
        this.type = type.getTypeName();
        this.message = message;
        this.date = LocalDateTime.now();
        this.fso = fso;
        this.backup = backup;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public DbLogType getType() {
        return DbLogType.getDbLogType(this.type);
    }

    public void setType(DbLogType type) {
        this.type = type.getTypeName();
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getFso() { return this.fso; }

    public void setFso(Integer fso) { this.fso = fso; }

    public String getBackup() { return this.backup; }

    public void setBackup(String backup) { this.backup = backup; }
}
