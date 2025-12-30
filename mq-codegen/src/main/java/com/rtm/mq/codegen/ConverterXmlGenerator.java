package com.rtm.mq.codegen;

import com.rtm.mq.ir.ConverterMappingConfig;
import com.rtm.mq.ir.ConverterResolution;
import com.rtm.mq.ir.FieldNode;
import com.rtm.mq.ir.GenerationReport;
import com.rtm.mq.ir.NameUtils;
import com.rtm.mq.ir.SchemaElement;
import com.rtm.mq.ir.SegmentNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates XML converter DSL files.
 */
public final class ConverterXmlGenerator {
    public void write(MessageContext context, Path outputPath) throws IOException {
        String xml = generate(context);
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, xml, StandardCharsets.UTF_8);
    }

    public String generate(MessageContext context) {
        String converterTag = context.inbound ? "fix-length-inbound-converter" : "fix-length-outbound-converter";
        String converterId = context.inbound ? "resp_converter" : "req_converter";

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<beans:beans xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"xxx\">\n");
        builder.append("  <").append(converterTag)
                .append(" id=\"").append(converterId).append("\" codeGen=\"true\">\n");
        builder.append("    <message forType=\"")
                .append(context.basePackage).append('.').append(context.root.getName())
                .append("\">\n");

        for (SchemaElement element : context.root.getElements()) {
            if (element instanceof SegmentNode segment) {
                appendSegmentFields(builder, segment, context, 6, "/" + segment.getName());
            }
        }

        builder.append("    </message>\n");
        builder.append("  </").append(converterTag).append(">\n");
        builder.append("</beans:beans>\n");
        return builder.toString();
    }

    private void appendSegmentFields(StringBuilder builder,
                                     SegmentNode segment,
                                     MessageContext context,
                                     int indent,
                                     String path) {
        appendProtocolFields(builder, segment, indent);
        appendSegmentBody(builder, segment, context, indent, path);
    }

    private void appendSegmentBody(StringBuilder builder,
                                   SegmentNode segment,
                                   MessageContext context,
                                   int indent,
                                   String path) {
        String indentText = " ".repeat(indent);
        String fieldName = NameUtils.toLowerCamel(segment.getName());
        String fieldType = segment.getOccurrence() != null && segment.getOccurrence().isRepeating()
                ? "RepeatingField"
                : "CompositeField";

        builder.append(indentText)
                .append("<field name=\"").append(fieldName)
                .append("\" type=\"").append(fieldType)
                .append("\" forType=\"").append(context.basePackage).append('.').append(segment.getName())
                .append("\">\n");

        for (SchemaElement element : segment.getElements()) {
            if (element instanceof FieldNode field) {
                appendDataField(builder, field, context, indent + 2, path + "/" + field.getName());
            } else if (element instanceof SegmentNode child) {
                appendSegmentFields(builder, child, context, indent + 2, path + "/" + child.getName());
            }
        }

        builder.append(indentText).append("</field>\n");
    }

    private void appendProtocolFields(StringBuilder builder,
                                      SegmentNode segment,
                                      int indent) {
        String indentText = " ".repeat(indent);
        if (segment.getProtocol() != null && segment.getProtocol().getGroupId() != null) {
            String groupIdValue = segment.getProtocol().getGroupIdValue();
            builder.append(indentText)
                    .append("<field type=\"DataField\" length=\"10\" fixedLength=\"true\" transitory=\"true\"");
            if (groupIdValue != null && !groupIdValue.isBlank()) {
                builder.append(" defaultValue=\"").append(escape(groupIdValue)).append("\"");
            }
            builder.append(" converter=\"stringFieldConverter\" />\n");
        }
        if (segment.getProtocol() != null && segment.getProtocol().getOccurrenceCount() != null) {
            builder.append(indentText)
                    .append("<field type=\"DataField\" length=\"4\" fixedLength=\"true\" transitory=\"true\"")
                    .append(" converter=\"counterFieldConverter\" />\n");
        }
    }

    private void appendDataField(StringBuilder builder,
                                 FieldNode field,
                                 MessageContext context,
                                 int indent,
                                 String path) {
        if (field.isProtocol()) {
            return;
        }
        String indentText = " ".repeat(indent);
        ConverterResolution resolution = context.mapping.resolveConverter(field.getDatatype(), path, field.getName());
        if (isUnsignedField(field) && field.getLengthBytes() != null && field.getLengthBytes() != 4) {
            resolution = new ConverterResolution("stringFieldConverter", true, "unsigned length != 4");
        }
        if (resolution.usedFallback()) {
            context.report.addFallback(path, resolution.converter(), resolution.reason());
        }

        builder.append(indentText)
                .append("<field name=\"").append(field.getName()).append("\" type=\"DataField\"");
        if (field.getLengthBytes() != null) {
            builder.append(" length=\"").append(field.getLengthBytes()).append("\"");
        }
        builder.append(" fixedLength=\"true\"");
        if (field.getExample() != null && !field.getExample().isBlank()) {
            builder.append(" defaultValue=\"").append(escape(field.getExample())).append("\"");
        } else if (field.getDefaultValue() != null && !field.getDefaultValue().isBlank()) {
            builder.append(" defaultValue=\"").append(escape(field.getDefaultValue())).append("\"");
        }
        if (field.getDatatype() != null && field.getDatatype().toLowerCase().contains("string")) {
            builder.append(" nullPad=\" \"");
        }
        builder.append(" converter=\"").append(resolution.converter()).append("\" />\n");
    }

    private String escape(String value) {
        return value.replace("\"", "&quot;");
    }

    private boolean isUnsignedField(FieldNode field) {
        String datatype = field.getDatatype() != null ? field.getDatatype().toLowerCase() : "";
        return datatype.contains("unsigned") || datatype.contains("integer") || datatype.contains("long");
    }

    /**
     * Context for XML generation.
     */
    public record MessageContext(SegmentNode root,
                                 String basePackage,
                                 ConverterMappingConfig mapping,
                                 GenerationReport report,
                                 boolean inbound) {
    }
}
