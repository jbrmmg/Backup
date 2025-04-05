package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jbr.middletier.backup.data.DbLogType;

import java.io.IOException;
import java.time.LocalDateTime;

@SuppressWarnings("unused")
@JsonSerialize(using = DbLogDTO.DbLogSerializer.class)
public class DbLogDTO {
    private DbLogType type;
    private LocalDateTime date;
    private String message;
    private Integer fso;
    private String backup;

    public static class DbLogSerializer extends JsonSerializer<DbLogDTO> {
        @Override
        public void serialize(DbLogDTO dbLogDTO, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            serializerProvider.defaultSerializeField("date", dbLogDTO.getDate(), jsonGenerator);
            serializerProvider.defaultSerializeField("type", dbLogDTO.getType().getDisplayName(), jsonGenerator);
            serializerProvider.defaultSerializeField("message", dbLogDTO.getMessage(), jsonGenerator);
            if(dbLogDTO.getFso() != null){
                serializerProvider.defaultSerializeField("fso", dbLogDTO.getFso(), jsonGenerator);
            }
            if(dbLogDTO.getBackup() != null){
                serializerProvider.defaultSerializeField("backup", dbLogDTO.getBackup(), jsonGenerator);
            }
            jsonGenerator.writeEndObject();
        }
    }

    public DbLogDTO() {
        this.type = DbLogType.DLT_DEBUG;
        this.message = "";
        this.date = LocalDateTime.now();
        this.fso = null;
        this.backup = null;
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

    public Integer getFso() { return this.fso; }

    public void setFso(Integer fso) { this.fso = fso; }

    public String getBackup() { return this.backup; }

    public void setBackup(String backup) { this.backup = backup; }
}
