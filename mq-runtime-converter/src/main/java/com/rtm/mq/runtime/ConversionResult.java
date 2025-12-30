package com.rtm.mq.runtime;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversion result with tracing metadata.
 *
 * @param value parsed object
 * @param traces field traces
 * @param issues validation issues
 * @param <T> value type
 */
public record ConversionResult<T>(T value, List<FieldTrace> traces, List<ConversionIssue> issues) {
    public ConversionResult(T value) {
        this(value, new ArrayList<>(), new ArrayList<>());
    }
}
