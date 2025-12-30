package com.rtm.mq.toolkit.codegen;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Configuration for XML template rendering.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XmlTemplateConfig {
    private String templatePath;
    private String namespace = "xxx";
    private String converterTagInbound = "fix-length-inbound-converter";
    private String converterTagOutbound = "fix-length-outbound-converter";
    private String converterIdInbound = "resp_converter";
    private String converterIdOutbound = "req_converter";

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getConverterTagInbound() {
        return converterTagInbound;
    }

    public void setConverterTagInbound(String converterTagInbound) {
        this.converterTagInbound = converterTagInbound;
    }

    public String getConverterTagOutbound() {
        return converterTagOutbound;
    }

    public void setConverterTagOutbound(String converterTagOutbound) {
        this.converterTagOutbound = converterTagOutbound;
    }

    public String getConverterIdInbound() {
        return converterIdInbound;
    }

    public void setConverterIdInbound(String converterIdInbound) {
        this.converterIdInbound = converterIdInbound;
    }

    public String getConverterIdOutbound() {
        return converterIdOutbound;
    }

    public void setConverterIdOutbound(String converterIdOutbound) {
        this.converterIdOutbound = converterIdOutbound;
    }
}
