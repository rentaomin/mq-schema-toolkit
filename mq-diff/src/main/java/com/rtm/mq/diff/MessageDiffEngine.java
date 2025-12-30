package com.rtm.mq.diff;

import com.rtm.mq.ir.FieldNode;
import com.rtm.mq.ir.MessageSchema;
import com.rtm.mq.ir.NameUtils;
import com.rtm.mq.ir.SchemaElement;
import com.rtm.mq.ir.SegmentNode;
import com.rtm.mq.runtime.ConversionResult;
import com.rtm.mq.runtime.FieldTrace;
import com.rtm.mq.runtime.MessageConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces diffs between expected and actual messages.
 */
public final class MessageDiffEngine {
    private final MessageConverter converter;
    private final ValueTreeBuilder treeBuilder = new ValueTreeBuilder();

    public MessageDiffEngine() {
        this(new MessageConverter());
    }

    public MessageDiffEngine(MessageConverter converter) {
        this.converter = converter;
    }

    public <T> DiffReport diff(byte[] expected,
                               byte[] actual,
                               MessageSchema schema,
                               Class<T> type) {
        ConversionResult<T> expectedResult = converter.unmarshal(expected, schema, type);
        ConversionResult<T> actualResult = converter.unmarshal(actual, schema, type);

        Map<String, Object> expectedTree = treeBuilder.build(schema.getRoot(), expectedResult.value());
        Map<String, Object> actualTree = treeBuilder.build(schema.getRoot(), actualResult.value());

        DiffReport report = new DiffReport();
        Map<String, FieldTrace> expectedTrace = traceIndex(expectedResult.traces());
        Map<String, FieldTrace> actualTrace = traceIndex(actualResult.traces());

        compareSegment(schema.getRoot(), expectedTree, actualTree, "/" + schema.getRoot().getName(),
                expectedTrace, actualTrace, report);
        return report;
    }

    private void compareSegment(SegmentNode segment,
                                Map<String, Object> expected,
                                Map<String, Object> actual,
                                String path,
                                Map<String, FieldTrace> expectedTrace,
                                Map<String, FieldTrace> actualTrace,
                                DiffReport report) {
        for (SchemaElement element : segment.getElements()) {
            if (element instanceof FieldNode field) {
                if (field.isProtocol()) {
                    continue;
                }
                String fieldPath = path + "/" + field.getName();
                Object expectedValue = expected != null ? expected.get(field.getName()) : null;
                Object actualValue = actual != null ? actual.get(field.getName()) : null;
                compareValue(fieldPath, expectedValue, actualValue, field.isRequired(), expectedTrace, actualTrace, report);
            } else if (element instanceof SegmentNode child) {
                String propertyName = NameUtils.toLowerCamel(child.getName());
                String childPath = path + "/" + child.getName();
                if (child.getOccurrence() != null && child.getOccurrence().isRepeating()) {
                    List<?> expectedList = expected != null ? (List<?>) expected.get(propertyName) : null;
                    List<?> actualList = actual != null ? (List<?>) actual.get(propertyName) : null;
                    compareLists(child, expectedList, actualList, childPath, expectedTrace, actualTrace, report);
                } else {
                    Map<String, Object> expectedChild = expected != null ? (Map<String, Object>) expected.get(propertyName) : null;
                    Map<String, Object> actualChild = actual != null ? (Map<String, Object>) actual.get(propertyName) : null;
                    compareSegment(child, expectedChild, actualChild, childPath, expectedTrace, actualTrace, report);
                }
            }
        }
    }

    private void compareLists(SegmentNode segment,
                              List<?> expected,
                              List<?> actual,
                              String path,
                              Map<String, FieldTrace> expectedTrace,
                              Map<String, FieldTrace> actualTrace,
                              DiffReport report) {
        int expectedSize = expected != null ? expected.size() : 0;
        int actualSize = actual != null ? actual.size() : 0;
        int max = Math.max(expectedSize, actualSize);
        for (int i = 0; i < max; i++) {
            String itemPath = path + "[" + i + "]";
            Object expectedItem = i < expectedSize ? expected.get(i) : null;
            Object actualItem = i < actualSize ? actual.get(i) : null;
            if (expectedItem == null || actualItem == null) {
                compareValue(itemPath, expectedItem, actualItem,
                        segment.getOccurrence() != null && segment.getOccurrence().getMinOccurs() > 0,
                        expectedTrace, actualTrace, report);
                continue;
            }
            compareSegment(segment, (Map<String, Object>) expectedItem, (Map<String, Object>) actualItem,
                    itemPath, expectedTrace, actualTrace, report);
        }
    }

    private void compareValue(String path,
                              Object expectedValue,
                              Object actualValue,
                              boolean required,
                              Map<String, FieldTrace> expectedTrace,
                              Map<String, FieldTrace> actualTrace,
                              DiffReport report) {
        if (actualValue == null && required) {
            report.addEntry(new DiffEntry(path, DiffKind.MISSING_REQUIRED, expectedValue, actualValue,
                    toOffset(expectedTrace.get(path)), toOffset(actualTrace.get(path))));
            return;
        }
        if (expectedValue == null && actualValue == null) {
            return;
        }
        if (expectedValue != null && actualValue != null) {
            if (!expectedValue.getClass().equals(actualValue.getClass())) {
                report.addEntry(new DiffEntry(path, DiffKind.TYPE_ERROR, expectedValue, actualValue,
                        toOffset(expectedTrace.get(path)), toOffset(actualTrace.get(path))));
                return;
            }
            if (!expectedValue.equals(actualValue)) {
                report.addEntry(new DiffEntry(path, DiffKind.MISMATCH, expectedValue, actualValue,
                        toOffset(expectedTrace.get(path)), toOffset(actualTrace.get(path))));
            }
            return;
        }
        report.addEntry(new DiffEntry(path, DiffKind.MISMATCH, expectedValue, actualValue,
                toOffset(expectedTrace.get(path)), toOffset(actualTrace.get(path))));
    }

    private Map<String, FieldTrace> traceIndex(List<FieldTrace> traces) {
        Map<String, FieldTrace> index = new HashMap<>();
        for (FieldTrace trace : traces) {
            index.put(trace.path(), trace);
        }
        return index;
    }

    private DiffEntry.Offset toOffset(FieldTrace trace) {
        if (trace == null) {
            return null;
        }
        return new DiffEntry.Offset(trace.startOffset(), trace.length());
    }
}
