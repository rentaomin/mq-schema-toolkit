package com.rtm.mq.runtime;

/**
 * Trace of a field with byte offsets.
 */
public record FieldTrace(String path, int startOffset, int length) {
}
