package com.jbr.middletier.backup.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

@JsonComponent
public class ImageSizeSerializer extends JsonSerializer<ImageSize> {
    @Override
    public void serialize(ImageSize imageSize, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(imageSize != null) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField("width", imageSize.width());
            jsonGenerator.writeNumberField("height", imageSize.height());
            jsonGenerator.writeEndObject();
        }
    }
}
