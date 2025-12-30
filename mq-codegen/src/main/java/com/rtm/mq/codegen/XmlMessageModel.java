package com.rtm.mq.codegen;

import java.util.ArrayList;
import java.util.List;

/**
 * XML message model for template rendering.
 */
public class XmlMessageModel {
    private String namespace;
    private String converterTag;
    private String converterId;
    private String messageType;
    private List<XmlFieldModel> fields = new ArrayList<>();

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getConverterTag() {
        return converterTag;
    }

    public void setConverterTag(String converterTag) {
        this.converterTag = converterTag;
    }

    public String getConverterId() {
        return converterId;
    }

    public void setConverterId(String converterId) {
        this.converterId = converterId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public List<XmlFieldModel> getFields() {
        return fields;
    }

    public void setFields(List<XmlFieldModel> fields) {
        this.fields = fields;
    }
}
