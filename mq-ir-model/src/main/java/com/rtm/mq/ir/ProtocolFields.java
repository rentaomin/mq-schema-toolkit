package com.rtm.mq.ir;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Protocol fields required for each segment.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProtocolFields {
    private FieldNode groupId;
    private FieldNode occurrenceCount;
    private String groupIdValue;

    public FieldNode getGroupId() {
        return groupId;
    }

    public void setGroupId(FieldNode groupId) {
        this.groupId = groupId;
    }

    public FieldNode getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(FieldNode occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public String getGroupIdValue() {
        return groupIdValue;
    }

    public void setGroupIdValue(String groupIdValue) {
        this.groupIdValue = groupIdValue;
    }
}
