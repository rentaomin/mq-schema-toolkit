package com.rtm.mq.spec.excel;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for Excel import header mapping.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExcelImportConfig {
    private Map<String, List<String>> columnAliases = new LinkedHashMap<>();
    private int headerScanLimit = 50;
    private int headerValueScanLimit = 30;
    private boolean captureExtraColumns = true;
    private List<String> groupIdFieldNames = List.of("groupid");
    private List<String> occurrenceFieldNames = List.of("occurenceCount", "occurrenceCount");

    public Map<String, List<String>> getColumnAliases() {
        return columnAliases;
    }

    public void setColumnAliases(Map<String, List<String>> columnAliases) {
        this.columnAliases = columnAliases;
    }

    public int getHeaderScanLimit() {
        return headerScanLimit;
    }

    public void setHeaderScanLimit(int headerScanLimit) {
        this.headerScanLimit = headerScanLimit;
    }

    public int getHeaderValueScanLimit() {
        return headerValueScanLimit;
    }

    public void setHeaderValueScanLimit(int headerValueScanLimit) {
        this.headerValueScanLimit = headerValueScanLimit;
    }

    public boolean isCaptureExtraColumns() {
        return captureExtraColumns;
    }

    public void setCaptureExtraColumns(boolean captureExtraColumns) {
        this.captureExtraColumns = captureExtraColumns;
    }

    public List<String> getGroupIdFieldNames() {
        return groupIdFieldNames;
    }

    public void setGroupIdFieldNames(List<String> groupIdFieldNames) {
        this.groupIdFieldNames = groupIdFieldNames;
    }

    public List<String> getOccurrenceFieldNames() {
        return occurrenceFieldNames;
    }

    public void setOccurrenceFieldNames(List<String> occurrenceFieldNames) {
        this.occurrenceFieldNames = occurrenceFieldNames;
    }
}
