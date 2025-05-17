package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="log")
public class DbLog {
    @Setter
    @Getter
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="type")
    private String type;

    @Setter
    @Getter
    @Column(name="date")
    private LocalDateTime date;

    @Setter
    @Getter
    @Column(name="message")
    private String message;

    @Setter
    @Getter
    @Column(name="fso")
    private Integer fso;

    @Setter
    @Getter
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

    public DbLogType getType() {
        return DbLogType.getDbLogType(this.type);
    }

    public void setType(DbLogType type) {
        this.type = type.getTypeName();
    }
}
