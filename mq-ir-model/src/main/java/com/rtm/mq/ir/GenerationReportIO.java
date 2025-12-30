package com.rtm.mq.ir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes generation report JSON files.
 */
public final class GenerationReportIO {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private GenerationReportIO() {
    }

    public static GenerationReport read(Path path) throws IOException {
        try (var input = Files.newInputStream(path)) {
            return JSON_MAPPER.readValue(input, GenerationReport.class);
        }
    }

    public static void write(Path path, GenerationReport report) throws IOException {
        Files.createDirectories(path.getParent());
        try (var output = Files.newOutputStream(path)) {
            JSON_MAPPER.writeValue(output, report);
        }
    }
}
