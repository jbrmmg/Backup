package com.jbr.middletier.backup.jsonserialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jbr.middletier.backup.dto.ProcessResultDTO;

import java.io.IOException;

public class ProcessResultSerializer extends JsonSerializer<ProcessResultDTO> {
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
