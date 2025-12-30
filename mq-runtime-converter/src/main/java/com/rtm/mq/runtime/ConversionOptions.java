package com.rtm.mq.runtime;

/**
 * Conversion behavior configuration.
 */
public class ConversionOptions {
    private GroupIdMode groupIdMode = GroupIdMode.STRICT;

    public GroupIdMode getGroupIdMode() {
        return groupIdMode;
    }

    public void setGroupIdMode(GroupIdMode groupIdMode) {
        this.groupIdMode = groupIdMode;
    }
}
