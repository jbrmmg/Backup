package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

@SuppressWarnings("unused")
public class ProcessResultSerializer extends StdSerializer<ProcessResultDTO> {
    public ProcessResultSerializer() {
        this(null);
    }

    public ProcessResultSerializer(Class<ProcessResultDTO> t) {
        super(t);
    }

    @Override
    public void serialize(ProcessResultDTO processResultDTO, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("underlyingId",processResultDTO.getUnderlyingId());
        jsonGenerator.writeBooleanField("failed", processResultDTO.hasProblems());
        for(String nextName: processResultDTO.getCounts().keySet()) {
            jsonGenerator.writeNumberField(nextName,processResultDTO.getCount(nextName));
        }
        jsonGenerator.writeEndObject();
    }
}
