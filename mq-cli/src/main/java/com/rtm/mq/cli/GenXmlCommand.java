package com.rtm.mq.cli;

import com.rtm.mq.codegen.ConverterXmlGenerator;
import com.rtm.mq.codegen.XmlTemplateConfig;
import com.rtm.mq.ir.ConverterMappingConfig;
import com.rtm.mq.ir.GenerationReport;
import com.rtm.mq.ir.GenerationReportIO;
import com.rtm.mq.ir.MessageSchema;
import com.rtm.mq.ir.ProtocolConfig;
import com.rtm.mq.ir.SchemaIO;
import com.rtm.mq.ir.YamlConfigIO;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates converter XML files from schemas.
 */
@CommandLine.Command(name = "gen-xml", description = "Generate converter XML from IR.")
public class GenXmlCommand extends BaseCommand {
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
            ConverterMappingConfig mapping = loadConverterMapping();
            ProtocolConfig protocolConfig = loadProtocolConfig();
            XmlTemplateConfig templateConfig = loadTemplateConfig();
            GenerationReport report = loadReport();
            ConverterXmlGenerator generator = new ConverterXmlGenerator();
            Path outputDir = baseDir.resolve("generated").resolve("xml");
            for (Path schemaPath : schemaFiles) {
                MessageSchema schema = SchemaIO.read(schemaPath);
                boolean inbound = schema.getDirection() != null && schema.getDirection().name().equalsIgnoreCase("RESPONSE");
                String fileName = inbound ? "mq-response-converters.xml" : "mq-request-converters.xml";
                ConverterXmlGenerator.MessageContext context =
                        new ConverterXmlGenerator.MessageContext(schema.getRoot(), basePackage, mapping, report, inbound,
                                protocolConfig, templateConfig);
                generator.write(context, outputDir.resolve(fileName));
            }
            GenerationReportIO.write(baseDir.resolve("generation-report.json"), report);
            System.out.println("Generated XML to " + outputDir);
        } catch (Exception ex) {
            throw new CommandLine.ExecutionException(new CommandLine(this), ex.getMessage(), ex);
        }
    }

    private ConverterMappingConfig loadConverterMapping() throws Exception {
        Path configPath = resolveConfigDir().resolve("converter-mapping.yaml");
        if (Files.exists(configPath)) {
            return YamlConfigIO.read(configPath, ConverterMappingConfig.class);
        }
        return new ConverterMappingConfig();
    }

    private ProtocolConfig loadProtocolConfig() throws Exception {
        Path configPath = resolveConfigDir().resolve("protocol.yaml");
        if (Files.exists(configPath)) {
            return YamlConfigIO.read(configPath, ProtocolConfig.class);
        }
        return new ProtocolConfig();
    }

    private XmlTemplateConfig loadTemplateConfig() throws Exception {
        Path configPath = resolveConfigDir().resolve("xml-template.yaml");
        if (Files.exists(configPath)) {
            return YamlConfigIO.read(configPath, XmlTemplateConfig.class);
        }
        return new XmlTemplateConfig();
    }

    private GenerationReport loadReport() {
        Path reportPath = baseDir.resolve("generation-report.json");
        if (Files.exists(reportPath)) {
            try {
                return GenerationReportIO.read(reportPath);
            } catch (Exception ignored) {
            }
        }
        return new GenerationReport();
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
