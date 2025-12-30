package com.rtm.mq.toolkit.diff;

/**
 * Single diff entry with offset mappings.
 */
public record DiffEntry(String path,
                        DiffKind kind,
                        Object expected,
                        Object actual,
                        Offset expectedOffset,
                        Offset actualOffset) {
    public record Offset(int start, int length) {
    }
}
