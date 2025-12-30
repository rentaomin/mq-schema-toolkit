package com.rtm.mq.toolkit.cli;

import com.rtm.mq.toolkit.ir.MessageSchema;
import com.rtm.mq.toolkit.ir.SchemaElement;
import com.rtm.mq.toolkit.ir.SchemaIO;
import com.rtm.mq.toolkit.ir.SegmentNode;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates IR schemas.
 */
@CommandLine.Command(name = "validate", description = "Validate IR schemas.")
public class ValidateCommand extends BaseCommand {
    @Override
    public void run() {
        try {
            List<Path> schemaFiles = listSchemaFiles(resolveSchemaDir());
            if (schemaFiles.isEmpty()) {
                System.out.println("No schemas found to validate.");
                return;
            }
            for (Path path : schemaFiles) {
                MessageSchema schema = SchemaIO.read(path);
                List<String> issues = new ArrayList<>();
                validateSegment(schema.getRoot(), "/" + schema.getRoot().getName(), issues);
                if (issues.isEmpty()) {
                    System.out.println("OK: " + path);
                } else {
                    System.out.println("Issues in " + path + ":");
                    for (String issue : issues) {
                        System.out.println(" - " + issue);
                    }
                }
            }
        } catch (Exception ex) {
            throw new CommandLine.ExecutionException(new CommandLine(this), ex.getMessage(), ex);
        }
    }

    private void validateSegment(SegmentNode segment, String path, List<String> issues) {
        if (segment.getProtocol() == null || segment.getProtocol().getGroupId() == null) {
            issues.add(path + " missing groupid protocol field");
        }
        if (segment.getProtocol() == null || segment.getProtocol().getOccurrenceCount() == null) {
            issues.add(path + " missing occurenceCount protocol field");
        }
        for (SchemaElement element : segment.getElements()) {
            if (element instanceof SegmentNode child) {
                validateSegment(child, path + "/" + child.getName(), issues);
            }
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
