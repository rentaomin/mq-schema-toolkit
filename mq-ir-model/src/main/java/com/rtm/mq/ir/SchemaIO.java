package com.rtm.mq.ir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes schema YAML files.
 */
public final class SchemaIO {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private SchemaIO() {
    }

    public static MessageSchema read(Path path) throws IOException {
        try (var input = Files.newInputStream(path)) {
            return YAML_MAPPER.readValue(input, MessageSchema.class);
        }
    }

    public static void write(Path path, MessageSchema schema) throws IOException {
        Files.createDirectories(path.getParent());
        try (var output = Files.newOutputStream(path)) {
            YAML_MAPPER.writeValue(output, schema);
        }
    }
}
