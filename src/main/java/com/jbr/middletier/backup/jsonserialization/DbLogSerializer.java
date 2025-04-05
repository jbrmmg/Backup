package com.jbr.middletier.backup.jsonserialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jbr.middletier.backup.dto.DbLogDTO;

import java.io.IOException;

@SuppressWarnings("unused")
public class DbLogSerializer  extends JsonSerializer<DbLogDTO> {
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
