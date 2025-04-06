package com.jbr.middletier.backup.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

@JsonComponent
public class LatLongSerializer extends JsonSerializer<LatLong> {
    @Override
    public void serialize(LatLong latLong, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(latLong != null) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField("lat", latLong.getLatitude());
            jsonGenerator.writeNumberField("long", latLong.getLongitude());
            jsonGenerator.writeEndObject();
        }
    }
}
