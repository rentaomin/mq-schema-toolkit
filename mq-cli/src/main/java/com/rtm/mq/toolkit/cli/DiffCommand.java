package com.rtm.mq.toolkit.cli;

import com.rtm.mq.toolkit.diff.DiffReport;
import com.rtm.mq.toolkit.diff.HtmlReportRenderer;
import com.rtm.mq.toolkit.diff.MessageDiffEngine;
import com.rtm.mq.toolkit.ir.MessageSchema;
import com.rtm.mq.toolkit.ir.SchemaIO;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Diffs two MQ messages using schema.
 */
@CommandLine.Command(name = "diff", description = "Diff expected vs actual messages.")
public class DiffCommand extends BaseCommand {
    @CommandLine.Option(names = "--schema", required = true, description = "Schema YAML file.")
    private Path schemaPath;

    @CommandLine.Option(names = "--expected", required = true, description = "Expected message file.")
    private Path expectedPath;

    @CommandLine.Option(names = "--actual", required = true, description = "Actual message file.")
    private Path actualPath;

    @CommandLine.Option(names = "--class", required = true, description = "Root class for conversion.")
    private String rootClass;

    @CommandLine.Option(names = "--output", description = "Output HTML report path.")
    private Path outputPath;

    @Override
    public void run() {
        try {
            MessageSchema schema = SchemaIO.read(schemaPath);
            byte[] expected = Files.readAllBytes(expectedPath);
            byte[] actual = Files.readAllBytes(actualPath);
            Class<?> type = Class.forName(rootClass);

            MessageDiffEngine engine = new MessageDiffEngine();
            DiffReport report = engine.diff(expected, actual, schema, type);

            Path output = outputPath != null
                    ? outputPath
                    : baseDir.resolve("reports").resolve("diff-report.html");
            Files.createDirectories(output.getParent());
            String html = new HtmlReportRenderer().render(report, expected, actual);
            Files.writeString(output, html);
            System.out.println("Diff report written to " + output);
        } catch (Exception ex) {
            throw new CommandLine.ExecutionException(new CommandLine(this), ex.getMessage(), ex);
        }
    }
}
