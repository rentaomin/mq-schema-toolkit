package com.rtm.mq.runtime;

/**
 * Conversion behavior configuration.
 */
public class ConversionOptions {
    private GroupIdMode groupIdMode = GroupIdMode.STRICT;
    private com.rtm.mq.ir.ProtocolConfig protocolConfig = new com.rtm.mq.ir.ProtocolConfig();

    public GroupIdMode getGroupIdMode() {
        return groupIdMode;
    }

    public void setGroupIdMode(GroupIdMode groupIdMode) {
        this.groupIdMode = groupIdMode;
    }

    public com.rtm.mq.ir.ProtocolConfig getProtocolConfig() {
        return protocolConfig;
    }

    public void setProtocolConfig(com.rtm.mq.ir.ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
    }
}
