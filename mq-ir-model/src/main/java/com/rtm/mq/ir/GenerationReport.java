package com.rtm.mq.ir;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Report of generation decisions and warnings.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerationReport {
    private List<GroupIdEntry> groupIds = new ArrayList<>();
    private List<FallbackConverterEntry> fallbackConverters = new ArrayList<>();
    private List<ReportIssue> issues = new ArrayList<>();

    public List<GroupIdEntry> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<GroupIdEntry> groupIds) {
        this.groupIds = groupIds;
    }

    public List<FallbackConverterEntry> getFallbackConverters() {
        return fallbackConverters;
    }

    public void setFallbackConverters(List<FallbackConverterEntry> fallbackConverters) {
        this.fallbackConverters = fallbackConverters;
    }

    public List<ReportIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<ReportIssue> issues) {
        this.issues = issues;
    }

    public void addGroupId(String segmentPath, String groupIdValue) {
        groupIds.add(new GroupIdEntry(segmentPath, groupIdValue));
    }

    public void addFallback(String fieldPath, String converter, String reason) {
        fallbackConverters.add(new FallbackConverterEntry(fieldPath, converter, reason));
    }

    public void addIssue(String level, String message, String context) {
        issues.add(new ReportIssue(level, message, context));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GroupIdEntry(String segmentPath, String groupIdValue) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FallbackConverterEntry(String fieldPath, String converter, String reason) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReportIssue(String level, String message, String context) {
    }
}
