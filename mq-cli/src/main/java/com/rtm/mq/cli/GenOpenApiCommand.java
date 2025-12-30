package com.rtm.mq.cli;

import com.rtm.mq.codegen.OpenApiGenerator;
import com.rtm.mq.ir.Direction;
import com.rtm.mq.ir.MessageSchema;
import com.rtm.mq.ir.SchemaIO;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates OpenAPI YAML from request/response schemas.
 */
@CommandLine.Command(name = "gen-openapi", description = "Generate OpenAPI YAML from IR.")
public class GenOpenApiCommand extends BaseCommand {
    @Override
    public void run() {
        try {
            Path schemaDirPath = resolveSchemaDir();
            List<Path> schemaFiles = listSchemaFiles(schemaDirPath);
            if (schemaFiles.isEmpty()) {
                System.out.println("No schemas found to generate.");
                return;
            }
            MessageSchema request = null;
            MessageSchema response = null;
            for (Path schemaPath : schemaFiles) {
                MessageSchema schema = SchemaIO.read(schemaPath);
                if (schema.getDirection() == Direction.REQUEST) {
                    request = schema;
                } else if (schema.getDirection() == Direction.RESPONSE) {
                    response = schema;
                }
            }
            if (request == null || response == null) {
                throw new IllegalStateException("Both request and response schemas are required.");
            }
            Path outputDir = baseDir.resolve("generated").resolve("openapi");
            Path outputPath = outputDir.resolve("openapi.yaml");
            OpenApiGenerator generator = new OpenApiGenerator();
            generator.write(request, response, outputPath);
            System.out.println("Generated OpenAPI to " + outputPath);
        } catch (Exception ex) {
            throw new CommandLine.ExecutionException(new CommandLine(this), ex.getMessage(), ex);
        }
    }

    private List<Path> listSchemaFiles(Path schemaDir) throws Exception {
        if (!Files.exists(schemaDir)) {
            return List.of();
        }
        try (var stream = Files.list(schemaDir)) {
            return stream.filter(path -> path.toString().endsWith(".yaml")).toList();
        }
    }
}
