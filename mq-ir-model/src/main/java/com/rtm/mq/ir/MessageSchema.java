package com.rtm.mq.ir;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Top-level schema metadata plus a root segment definition.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageSchema {
    private String schemaId;
    private String operationId;
    private String version;
    private Direction direction;
    private SegmentNode root;
    private Map<String, String> sharedHeader = new LinkedHashMap<>();

    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public SegmentNode getRoot() {
        return root;
    }

    public void setRoot(SegmentNode root) {
        this.root = root;
    }

    public Map<String, String> getSharedHeader() {
        return sharedHeader;
    }

    public void setSharedHeader(Map<String, String> sharedHeader) {
        this.sharedHeader = sharedHeader;
    }
}
