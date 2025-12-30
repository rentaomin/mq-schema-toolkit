package com.rtm.mq.toolkit.codegen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * XML field model for template rendering.
 */
public class XmlFieldModel {
    private String name;
    private String type;
    private String forType;
    private Map<String, String> attributes = new LinkedHashMap<>();
    private List<XmlFieldModel> children = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getForType() {
        return forType;
    }

    public void setForType(String forType) {
        this.forType = forType;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public List<XmlFieldModel> getChildren() {
        return children;
    }

    public void setChildren(List<XmlFieldModel> children) {
        this.children = children;
    }
}
