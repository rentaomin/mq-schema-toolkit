package com.rtm.mq.codegen;

import com.rtm.mq.ir.ConverterMappingConfig;
import com.rtm.mq.ir.ConverterResolution;
import com.rtm.mq.ir.FieldNode;
import com.rtm.mq.ir.GenerationReport;
import com.rtm.mq.ir.NameUtils;
import com.rtm.mq.ir.SchemaElement;
import com.rtm.mq.ir.SegmentNode;

import com.rtm.mq.ir.ProtocolConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

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
        XmlMessageModel model = buildMessageModel(context);
        XmlTemplateRenderer renderer = new XmlTemplateRenderer();
        try {
            return renderer.render(model, context.templateConfig);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render XML template: " + ex.getMessage(), ex);
        }
    }

    private XmlMessageModel buildMessageModel(MessageContext context) {
        XmlTemplateConfig templateConfig = context.templateConfig;
        XmlMessageModel model = new XmlMessageModel();
        model.setNamespace(templateConfig.getNamespace());
        model.setConverterTag(context.inbound ? templateConfig.getConverterTagInbound() : templateConfig.getConverterTagOutbound());
        model.setConverterId(context.inbound ? templateConfig.getConverterIdInbound() : templateConfig.getConverterIdOutbound());
        model.setMessageType(context.basePackage + "." + context.root.getName());

        for (SchemaElement element : context.root.getElements()) {
            if (element instanceof SegmentNode segment) {
                model.getFields().addAll(buildSegmentFields(segment, context, "/" + segment.getName()));
            }
        }
        return model;
    }

    private java.util.List<XmlFieldModel> buildSegmentFields(SegmentNode segment,
                                                             MessageContext context,
                                                             String path) {
        java.util.List<XmlFieldModel> fields = new java.util.ArrayList<>();
        fields.addAll(buildProtocolFields(segment, context));
        fields.add(buildSegmentBody(segment, context, path));
        return fields;
    }

    private XmlFieldModel buildSegmentBody(SegmentNode segment, MessageContext context, String path) {
        XmlFieldModel segmentField = new XmlFieldModel();
        segmentField.setName(NameUtils.toLowerCamel(segment.getName()));
        segmentField.setType(segment.getOccurrence() != null && segment.getOccurrence().isRepeating()
                ? "RepeatingField"
                : "CompositeField");
        segmentField.setForType(context.basePackage + "." + segment.getName());

        for (SchemaElement element : segment.getElements()) {
            if (element instanceof FieldNode field) {
                XmlFieldModel dataField = buildDataField(field, context, path + "/" + field.getName());
                if (dataField != null) {
                    segmentField.getChildren().add(dataField);
                }
            } else if (element instanceof SegmentNode child) {
                segmentField.getChildren().addAll(buildSegmentFields(child, context, path + "/" + child.getName()));
            }
        }
        return segmentField;
    }

    private java.util.List<XmlFieldModel> buildProtocolFields(SegmentNode segment, MessageContext context) {
        java.util.List<XmlFieldModel> fields = new java.util.ArrayList<>();
        ProtocolConfig protocol = context.protocolConfig;
        if (segment.getProtocol() != null && segment.getProtocol().getGroupId() != null) {
            XmlFieldModel groupId = new XmlFieldModel();
            groupId.setType("DataField");
            groupId.getAttributes().put("length", String.valueOf(resolveGroupIdLength(segment, protocol)));
            groupId.getAttributes().put("fixedLength", "true");
            groupId.getAttributes().put("transitory", "true");
            String groupIdValue = segment.getProtocol().getGroupIdValue();
            if (groupIdValue != null && !groupIdValue.isBlank()) {
                groupId.getAttributes().put("defaultValue", groupIdValue);
            }
            groupId.getAttributes().put("converter", protocol.getGroupIdConverter());
            fields.add(groupId);
        }
        if (segment.getProtocol() != null && segment.getProtocol().getOccurrenceCount() != null) {
            XmlFieldModel occurrence = new XmlFieldModel();
            occurrence.setType("DataField");
            occurrence.getAttributes().put("length", String.valueOf(resolveOccurrenceLength(segment, protocol)));
            occurrence.getAttributes().put("fixedLength", "true");
            occurrence.getAttributes().put("transitory", "true");
            occurrence.getAttributes().put("converter", protocol.getOccurrenceConverter());
            fields.add(occurrence);
        }
        return fields;
    }

    private XmlFieldModel buildDataField(FieldNode field,
                                         MessageContext context,
                                         String path) {
        if (field.isProtocol()) {
            return null;
        }
        ConverterResolution resolution = context.mapping.resolveConverter(field.getDatatype(), path, field.getName());
        if (isUnsignedField(field) && field.getLengthBytes() != null && field.getLengthBytes() != 4) {
            resolution = new ConverterResolution("stringFieldConverter", true, "unsigned length != 4");
        }
        if (resolution.usedFallback()) {
            context.report.addFallback(path, resolution.converter(), resolution.reason());
        }

        XmlFieldModel dataField = new XmlFieldModel();
        dataField.setName(field.getName());
        dataField.setType("DataField");
        if (field.getLengthBytes() != null) {
            dataField.getAttributes().put("length", field.getLengthBytes().toString());
        }
        dataField.getAttributes().put("fixedLength", "true");
        if (field.getExample() != null && !field.getExample().isBlank()) {
            dataField.getAttributes().put("defaultValue", field.getExample());
        } else if (field.getDefaultValue() != null && !field.getDefaultValue().isBlank()) {
            dataField.getAttributes().put("defaultValue", field.getDefaultValue());
        }
        if (field.getDatatype() != null && field.getDatatype().toLowerCase().contains("string")) {
            dataField.getAttributes().put("nullPad", " ");
        }
        dataField.getAttributes().put("converter", resolution.converter());
        if (!field.getExtensions().isEmpty()) {
            for (var entry : field.getExtensions().entrySet()) {
                String attrName = toXmlAttributeName(entry.getKey());
                if (!attrName.isBlank()) {
                    dataField.getAttributes().put(attrName, entry.getValue());
                }
            }
        }
        return dataField;
    }

    private int resolveGroupIdLength(SegmentNode segment, ProtocolConfig protocol) {
        if (segment.getProtocol() != null && segment.getProtocol().getGroupId() != null
                && segment.getProtocol().getGroupId().getLengthBytes() != null) {
            return segment.getProtocol().getGroupId().getLengthBytes();
        }
        return protocol.getGroupIdLength();
    }

    private int resolveOccurrenceLength(SegmentNode segment, ProtocolConfig protocol) {
        if (segment.getProtocol() != null && segment.getProtocol().getOccurrenceCount() != null
                && segment.getProtocol().getOccurrenceCount().getLengthBytes() != null) {
            return segment.getProtocol().getOccurrenceCount().getLengthBytes();
        }
        return protocol.getOccurrenceLength();
    }

    private String toXmlAttributeName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("[^A-Za-z0-9_\\-]", "");
        if (normalized.isEmpty()) {
            return "";
        }
        if (Character.isDigit(normalized.charAt(0))) {
            return "_" + normalized;
        }
        return normalized;
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
                                 boolean inbound,
                                 ProtocolConfig protocolConfig,
                                 XmlTemplateConfig templateConfig) {
    }
}
