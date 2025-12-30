package com.rtm.mq.ir;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Leaf field definition in a schema segment.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class FieldNode implements SchemaElement {
    private String name;
    private String originalName;
    private String description;
    private Integer lengthBytes;
    private String datatype;
    private boolean required;
    private Boolean nullable;
    private String example;
    private String format;
    private String converter;
    private String defaultValue;
    private boolean protocol;
    private Map<String, String> extensions = new LinkedHashMap<>();

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getLengthBytes() {
        return lengthBytes;
    }

    public void setLengthBytes(Integer lengthBytes) {
        this.lengthBytes = lengthBytes;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getConverter() {
        return converter;
    }

    public void setConverter(String converter) {
        this.converter = converter;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isProtocol() {
        return protocol;
    }

    public void setProtocol(boolean protocol) {
        this.protocol = protocol;
    }

    public Map<String, String> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, String> extensions) {
        this.extensions = extensions;
    }
}
