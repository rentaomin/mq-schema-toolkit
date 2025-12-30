package com.rtm.mq.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Diff report container.
 */
public class DiffReport {
    private final List<DiffEntry> entries = new ArrayList<>();

    public List<DiffEntry> getEntries() {
        return entries;
    }

    public void addEntry(DiffEntry entry) {
        entries.add(entry);
    }
}
