package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class DbLogSerializer  extends StdSerializer<DbLogDTO> {
    public DbLogSerializer(Class<DbLogDTO> t) {
        super(t);
    }

    @Override
    public void serialize(DbLogDTO dbLogDTO, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializerProvider.defaultSerializeField("date", dbLogDTO.getDate(), jsonGenerator);
        serializerProvider.defaultSerializeField("type", dbLogDTO.getType().getDisplayName(), jsonGenerator);
        serializerProvider.defaultSerializeField("message", dbLogDTO.getMessage(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }
}
