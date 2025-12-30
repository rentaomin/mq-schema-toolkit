package com.rtm.mq.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.rtm.mq.ir.FieldNode;
import com.rtm.mq.ir.MessageSchema;
import com.rtm.mq.ir.NameUtils;
import com.rtm.mq.ir.SchemaElement;
import com.rtm.mq.ir.SegmentNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates OpenAPI YAML documents from schema IR.
 */
public final class OpenApiGenerator {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public void write(MessageSchema request, MessageSchema response, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        YAML_MAPPER.writeValue(outputPath.toFile(), buildDocument(request, response));
    }

    public Map<String, Object> buildDocument(MessageSchema request, MessageSchema response) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("openapi", "3.0.3");
        root.put("info", Map.of(
                "title", request.getOperationId(),
                "version", request.getVersion()
        ));

        Map<String, Object> components = new LinkedHashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();
        components.put("schemas", schemas);
        root.put("components", components);

        SegmentNode requestRoot = request.getRoot();
        SegmentNode responseRoot = response.getRoot();
        collectSchemas(requestRoot, schemas);
        collectSchemas(responseRoot, schemas);

        String pathKey = "/" + request.getOperationId();
        Map<String, Object> paths = new LinkedHashMap<>();
        Map<String, Object> pathItem = new LinkedHashMap<>();
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("operationId", request.getOperationId());
        post.put("requestBody", Map.of(
                "required", true,
                "content", Map.of(
                        "application/json", Map.of(
                                "schema", Map.of("$ref", "#/components/schemas/" + requestRoot.getName())
                        )
                )
        ));
        post.put("responses", Map.of(
                "200", Map.of(
                        "description", "Success",
                        "content", Map.of(
                                "application/json", Map.of(
                                        "schema", Map.of("$ref", "#/components/schemas/" + responseRoot.getName())
                                )
                        )
                )
        ));
        pathItem.put("post", post);
        paths.put(pathKey, pathItem);
        root.put("paths", paths);

        return root;
    }

    private void collectSchemas(SegmentNode segment, Map<String, Object> schemas) {
        if (!schemas.containsKey(segment.getName())) {
            schemas.put(segment.getName(), buildSegmentSchema(segment));
        }
        for (SchemaElement element : segment.getElements()) {
            if (element instanceof SegmentNode child) {
                collectSchemas(child, schemas);
            }
        }
    }

    private Map<String, Object> buildSegmentSchema(SegmentNode segment) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        if (segment.getDescription() != null) {
            schema.put("description", segment.getDescription());
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (SchemaElement element : segment.getElements()) {
            if (element instanceof FieldNode field) {
                if (field.isProtocol()) {
                    continue;
                }
                properties.put(field.getName(), buildFieldSchema(field));
                if (field.isRequired()) {
                    required.add(field.getName());
                }
            } else if (element instanceof SegmentNode child) {
                properties.put(NameUtils.toLowerCamel(child.getName()), buildChildSchema(child));
                if (child.getOccurrence() != null && child.getOccurrence().getMinOccurs() > 0) {
                    required.add(NameUtils.toLowerCamel(child.getName()));
                }
            }
        }
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private Map<String, Object> buildFieldSchema(FieldNode field) {
        Map<String, Object> schema = new LinkedHashMap<>();
        String datatype = field.getDatatype() != null ? field.getDatatype().toLowerCase() : "";
        if (datatype.contains("unsigned") || datatype.contains("integer") || datatype.contains("int")) {
            schema.put("type", "integer");
            schema.put("format", "int64");
        } else if (datatype.contains("decimal") || datatype.contains("currency")) {
            schema.put("type", "number");
            schema.put("format", "decimal");
        } else {
            schema.put("type", "string");
            if (field.getLengthBytes() != null) {
                schema.put("maxLength", field.getLengthBytes());
            }
        }
        if (field.getDescription() != null) {
            schema.put("description", field.getDescription());
        }
        if (field.getExample() != null && !field.getExample().isBlank()) {
            schema.put("example", field.getExample());
        }
        return schema;
    }

    private Map<String, Object> buildChildSchema(SegmentNode child) {
        if (child.getOccurrence() != null && child.getOccurrence().isRepeating()) {
            return Map.of(
                    "type", "array",
                    "items", Map.of("$ref", "#/components/schemas/" + child.getName())
            );
        }
        return Map.of("$ref", "#/components/schemas/" + child.getName());
    }
}
