package com.rtm.mq.cli;

import com.rtm.mq.ir.GenerationReportIO;
import com.rtm.mq.ir.GroupIdExtractionConfig;
import com.rtm.mq.ir.MessageSchema;
import com.rtm.mq.ir.SchemaIO;
import com.rtm.mq.ir.YamlConfigIO;
import com.rtm.mq.spec.excel.ExcelImportConfig;
import com.rtm.mq.spec.excel.ExcelSchemaImporter;
import com.rtm.mq.spec.excel.ImportResult;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Imports Excel message specs into IR YAML schemas.
 */
@CommandLine.Command(name = "import-excel", description = "Import Excel specification into IR YAML.")
public class ImportExcelCommand extends BaseCommand {
    @CommandLine.Option(names = "--excel", required = true, description = "Excel specification file.")
    private Path excelPath;

    @Override
    public void run() {
        try {
            ExcelSchemaImporter importer = new ExcelSchemaImporter();
            GroupIdExtractionConfig groupIdConfig = loadGroupIdConfig();
            ExcelImportConfig importConfig = loadImportConfig();
            ImportResult result = importer.importFile(excelPath, groupIdConfig, importConfig);
            Path schemaDirPath = resolveSchemaDir();

            if (result.request() != null) {
                writeSchema(schemaDirPath, result.request(), "request");
            }
            if (result.response() != null) {
                writeSchema(schemaDirPath, result.response(), "response");
            }

            Path reportPath = baseDir.resolve("generation-report.json");
            GenerationReportIO.write(reportPath, result.report());
            System.out.println("Imported schemas to " + schemaDirPath);
            System.out.println("Generation report: " + reportPath);
        } catch (Exception ex) {
            throw new CommandLine.ExecutionException(new CommandLine(this), ex.getMessage(), ex);
        }
    }

    private void writeSchema(Path schemaDirPath, MessageSchema schema, String direction) throws Exception {
        Files.createDirectories(schemaDirPath);
        String fileName = schema.getOperationId() + "-" + direction + "-v" + schema.getVersion() + ".yaml";
        SchemaIO.write(schemaDirPath.resolve(fileName), schema);
    }

    private GroupIdExtractionConfig loadGroupIdConfig() throws Exception {
        Path configPath = resolveConfigDir().resolve("groupid-extraction.yaml");
        if (Files.exists(configPath)) {
            return YamlConfigIO.read(configPath, GroupIdExtractionConfig.class);
        }
        return new GroupIdExtractionConfig();
    }

    private ExcelImportConfig loadImportConfig() throws Exception {
        Path configPath = resolveConfigDir().resolve("importer.yaml");
        if (Files.exists(configPath)) {
            return YamlConfigIO.read(configPath, ExcelImportConfig.class);
        }
        return new ExcelImportConfig();
    }
}
