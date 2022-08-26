package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jbr.middletier.backup.data.DbLogType;

import java.time.LocalDateTime;

@SuppressWarnings("unused")
@JsonSerialize(using = DbLogSerializer.class)
public class DbLogDTO {
    private DbLogType type;
    private LocalDateTime date;
    private String message;

    public DbLogDTO() {
        this.type = DbLogType.DLT_DEBUG;
        this.message = "";
        this.date = LocalDateTime.now();
    }

    public DbLogType getType() {
        return type;
    }

    public void setType(DbLogType type) {
        this.type = type;
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
}
