package com.rtm.mq.web;

import com.rtm.mq.ir.ProtocolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot configuration properties for toolkit.
 */
@ConfigurationProperties(prefix = "mqtool")
public class ToolkitProperties {
    private String baseDir = "schema-repo-example";
    private String schemaDir;
    private String basePackage = "com.rtm.mq.generated";
    private ProtocolConfig protocol = new ProtocolConfig();
    private String xmlTemplatePath;
    private String groupIdMode = "STRICT";

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getSchemaDir() {
        return schemaDir;
    }

    public void setSchemaDir(String schemaDir) {
        this.schemaDir = schemaDir;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public ProtocolConfig getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtocolConfig protocol) {
        this.protocol = protocol;
    }

    public String getXmlTemplatePath() {
        return xmlTemplatePath;
    }

    public void setXmlTemplatePath(String xmlTemplatePath) {
        this.xmlTemplatePath = xmlTemplatePath;
    }

    public String getGroupIdMode() {
        return groupIdMode;
    }

    public void setGroupIdMode(String groupIdMode) {
        this.groupIdMode = groupIdMode;
    }
}
