package com.rtm.mq.ir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper for reading YAML configuration files.
 */
public final class YamlConfigIO {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private YamlConfigIO() {
    }

    public static <T> T read(Path path, Class<T> type) throws IOException {
        try (var input = Files.newInputStream(path)) {
            return YAML_MAPPER.readValue(input, type);
        }
    }
}
