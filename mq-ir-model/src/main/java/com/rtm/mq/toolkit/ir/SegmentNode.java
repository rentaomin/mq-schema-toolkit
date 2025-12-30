package com.rtm.mq.toolkit.ir;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Segment definition that contains child segments and fields.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SegmentNode implements SchemaElement {
    private String name;
    private String originalName;
    private String description;
    private Occurrence occurrence;
    private ProtocolFields protocol;
    private List<SchemaElement> elements = new ArrayList<>();
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

    public Occurrence getOccurrence() {
        return occurrence;
    }

    public void setOccurrence(Occurrence occurrence) {
        this.occurrence = occurrence;
    }

    public ProtocolFields getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtocolFields protocol) {
        this.protocol = protocol;
    }

    public List<SchemaElement> getElements() {
        return elements;
    }

    public void setElements(List<SchemaElement> elements) {
        this.elements = elements;
    }

    public Map<String, String> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, String> extensions) {
        this.extensions = extensions;
    }
}
