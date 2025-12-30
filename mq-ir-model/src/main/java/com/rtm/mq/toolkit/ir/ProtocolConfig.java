package com.rtm.mq.toolkit.ir;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Protocol configuration for groupId and occurrenceCount.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProtocolConfig {
    private String groupIdFieldName = "groupid";
    private String occurrenceFieldName = "occurenceCount";
    private int groupIdLength = 10;
    private int occurrenceLength = 4;
    private String byteOrder = "BIG_ENDIAN";
    private String groupIdConverter = "stringFieldConverter";
    private String occurrenceConverter = "counterFieldConverter";

    public String getGroupIdFieldName() {
        return groupIdFieldName;
    }

    public void setGroupIdFieldName(String groupIdFieldName) {
        this.groupIdFieldName = groupIdFieldName;
    }

    public String getOccurrenceFieldName() {
        return occurrenceFieldName;
    }

    public void setOccurrenceFieldName(String occurrenceFieldName) {
        this.occurrenceFieldName = occurrenceFieldName;
    }

    public int getGroupIdLength() {
        return groupIdLength;
    }

    public void setGroupIdLength(int groupIdLength) {
        this.groupIdLength = groupIdLength;
    }

    public int getOccurrenceLength() {
        return occurrenceLength;
    }

    public void setOccurrenceLength(int occurrenceLength) {
        this.occurrenceLength = occurrenceLength;
    }

    public String getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(String byteOrder) {
        this.byteOrder = byteOrder;
    }

    public String getGroupIdConverter() {
        return groupIdConverter;
    }

    public void setGroupIdConverter(String groupIdConverter) {
        this.groupIdConverter = groupIdConverter;
    }

    public String getOccurrenceConverter() {
        return occurrenceConverter;
    }

    public void setOccurrenceConverter(String occurrenceConverter) {
        this.occurrenceConverter = occurrenceConverter;
    }
}
