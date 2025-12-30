package com.rtm.mq.web;

import com.rtm.mq.codegen.ConverterXmlGenerator;
import com.rtm.mq.codegen.XmlTemplateConfig;
import com.rtm.mq.codegen.JavaPojoGenerator;
import com.rtm.mq.codegen.OpenApiGenerator;
import com.rtm.mq.diff.DiffReport;
import com.rtm.mq.diff.HtmlReportRenderer;
import com.rtm.mq.diff.MessageDiffEngine;
import com.rtm.mq.ir.ConverterMappingConfig;
import com.rtm.mq.ir.Direction;
import com.rtm.mq.ir.GenerationReport;
import com.rtm.mq.ir.GenerationReportIO;
import com.rtm.mq.ir.GroupIdExtractionConfig;
import com.rtm.mq.ir.MessageSchema;
import com.rtm.mq.ir.ProtocolConfig;
import com.rtm.mq.ir.SchemaIO;
import com.rtm.mq.ir.YamlConfigIO;
import com.rtm.mq.spec.excel.ExcelImportConfig;
import com.rtm.mq.spec.excel.ExcelSchemaImporter;
import com.rtm.mq.spec.excel.ImportResult;
import com.rtm.mq.runtime.MessageCodec;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Service layer for schema tooling operations.
 */
@Service
public class ToolkitService {
    private final MessageCodec codec;
    private final ToolkitProperties properties;

    public ToolkitService(MessageCodec codec, ToolkitProperties properties) {
        this.codec = codec;
        this.properties = properties;
    }
    public ImportResult importExcel(Path excelPath, Path baseDir, Path schemaDir) throws Exception {
        ExcelSchemaImporter importer = new ExcelSchemaImporter();
        GroupIdExtractionConfig groupIdConfig = loadGroupIdConfig(baseDir);
        ExcelImportConfig importConfig = loadImportConfig(baseDir);
        ImportResult result = importer.importFile(excelPath, groupIdConfig, importConfig);

        Path schemaDirPath = schemaDir != null ? schemaDir : baseDir.resolve("schemas");
        if (result.request() != null) {
            writeSchema(schemaDirPath, result.request(), "request");
        }
        if (result.response() != null) {
            writeSchema(schemaDirPath, result.response(), "response");
        }
        GenerationReportIO.write(baseDir.resolve("generation-report.json"), result.report());
        return result;
    }

    public void generateJava(Path baseDir, Path schemaDir, String basePackage) throws Exception {
        JavaPojoGenerator generator = new JavaPojoGenerator();
        for (Path schemaPath : listSchemaFiles(schemaDir)) {
            MessageSchema schema = SchemaIO.read(schemaPath);
            generator.generate(schema.getRoot(), baseDir.resolve("generated").resolve("java"), basePackage);
        }
    }

    public void generateXml(Path baseDir, Path schemaDir, String basePackage) throws Exception {
        ConverterMappingConfig mapping = loadConverterMapping(baseDir);
        ProtocolConfig protocolConfig = loadProtocolConfig(baseDir);
        XmlTemplateConfig templateConfig = loadTemplateConfig(baseDir);
        GenerationReport report = loadReport(baseDir);
        ConverterXmlGenerator generator = new ConverterXmlGenerator();
        for (Path schemaPath : listSchemaFiles(schemaDir)) {
            MessageSchema schema = SchemaIO.read(schemaPath);
            boolean inbound = schema.getDirection() == Direction.RESPONSE;
            String fileName = inbound ? "mq-response-converters.xml" : "mq-request-converters.xml";
            ConverterXmlGenerator.MessageContext context =
                    new ConverterXmlGenerator.MessageContext(schema.getRoot(), basePackage, mapping, report, inbound,
                            protocolConfig, templateConfig);
            generator.write(context, baseDir.resolve("generated").resolve("xml").resolve(fileName));
        }
        GenerationReportIO.write(baseDir.resolve("generation-report.json"), report);
    }

    public void generateOpenApi(Path baseDir, Path schemaDir) throws Exception {
        MessageSchema request = null;
        MessageSchema response = null;
        for (Path schemaPath : listSchemaFiles(schemaDir)) {
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
        OpenApiGenerator generator = new OpenApiGenerator();
        generator.write(request, response, baseDir.resolve("generated").resolve("openapi").resolve("openapi.yaml"));
    }

    public void diffMessages(Path schemaPath,
                             Path expectedPath,
                             Path actualPath,
                             String rootClass,
                             Path outputPath) throws Exception {
        MessageSchema schema = SchemaIO.read(schemaPath);
        byte[] expected = Files.readAllBytes(expectedPath);
        byte[] actual = Files.readAllBytes(actualPath);
        Class<?> type = Class.forName(rootClass);
        DiffReport report = new MessageDiffEngine(codec).diff(expected, actual, schema, type);
        Path output = outputPath != null ? outputPath : expectedPath.getParent().resolve("diff-report.html");
        Files.createDirectories(output.getParent());
        String html = new HtmlReportRenderer().render(report, expected, actual);
        Files.writeString(output, html);
    }

    private void writeSchema(Path schemaDirPath, MessageSchema schema, String direction) throws Exception {
        Files.createDirectories(schemaDirPath);
        String fileName = schema.getOperationId() + "-" + direction + "-v" + schema.getVersion() + ".yaml";
        SchemaIO.write(schemaDirPath.resolve(fileName), schema);
    }

    private List<Path> listSchemaFiles(Path schemaDir) throws Exception {
        if (schemaDir == null || !Files.exists(schemaDir)) {
            return List.of();
        }
        try (var stream = Files.list(schemaDir)) {
            return stream.filter(path -> path.toString().endsWith(".yaml")).toList();
        }
    }

    private ConverterMappingConfig loadConverterMapping(Path baseDir) throws Exception {
        Path configPath = baseDir.resolve("config").resolve("converter-mapping.yaml");
        if (Files.exists(configPath)) {
            return YamlConfigIO.read(configPath, ConverterMappingConfig.class);
        }
        return new ConverterMappingConfig();
    }

    private ProtocolConfig loadProtocolConfig(Path baseDir) throws Exception {
        Path configPath = baseDir.resolve("config").resolve("protocol.yaml");
        if (Files.exists(configPath)) {
            return YamlConfigIO.read(configPath, ProtocolConfig.class);
        }
        return properties.getProtocol();
    }

    private XmlTemplateConfig loadTemplateConfig(Path baseDir) throws Exception {
        Path configPath = baseDir.resolve("config").resolve("xml-template.yaml");
        if (Files.exists(configPath)) {
            return YamlConfigIO.read(configPath, XmlTemplateConfig.class);
        }
        XmlTemplateConfig config = new XmlTemplateConfig();
        if (properties.getXmlTemplatePath() != null) {
            config.setTemplatePath(properties.getXmlTemplatePath());
        }
        return config;
    }

    private GroupIdExtractionConfig loadGroupIdConfig(Path baseDir) throws Exception {
        Path configPath = baseDir.resolve("config").resolve("groupid-extraction.yaml");
        if (Files.exists(configPath)) {
            return YamlConfigIO.read(configPath, GroupIdExtractionConfig.class);
        }
        return new GroupIdExtractionConfig();
    }

    private GenerationReport loadReport(Path baseDir) {
        Path reportPath = baseDir.resolve("generation-report.json");
        if (Files.exists(reportPath)) {
            try {
                return GenerationReportIO.read(reportPath);
            } catch (Exception ignored) {
            }
        }
        return new GenerationReport();
    }

    private ExcelImportConfig loadImportConfig(Path baseDir) throws Exception {
        Path configPath = baseDir.resolve("config").resolve("importer.yaml");
        if (Files.exists(configPath)) {
            return YamlConfigIO.read(configPath, ExcelImportConfig.class);
        }
        return new ExcelImportConfig();
    }
}
