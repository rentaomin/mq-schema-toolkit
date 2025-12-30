package com.rtm.mq.toolkit.cli;

import com.rtm.mq.toolkit.codegen.JavaPojoGenerator;
import com.rtm.mq.toolkit.ir.MessageSchema;
import com.rtm.mq.toolkit.ir.SchemaIO;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates Java POJOs from schemas.
 */
@CommandLine.Command(name = "gen-java", description = "Generate Java POJOs from IR.")
public class GenJavaCommand extends BaseCommand {
    @CommandLine.Option(names = "--basePackage", description = "Base package for generated classes.",
            defaultValue = "com.rtm.mq.generated")
    private String basePackage;

    @Override
    public void run() {
        try {
            Path schemaDirPath = resolveSchemaDir();
            List<Path> schemaFiles = listSchemaFiles(schemaDirPath);
            if (schemaFiles.isEmpty()) {
                System.out.println("No schemas found to generate.");
                return;
            }
            Path outputDir = baseDir.resolve("generated").resolve("java");
            JavaPojoGenerator generator = new JavaPojoGenerator();
            for (Path schemaPath : schemaFiles) {
                MessageSchema schema = SchemaIO.read(schemaPath);
                generator.generate(schema.getRoot(), outputDir, basePackage);
            }
            System.out.println("Generated Java to " + outputDir);
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
