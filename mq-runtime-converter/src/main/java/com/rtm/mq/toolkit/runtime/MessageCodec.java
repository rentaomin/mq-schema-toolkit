package com.rtm.mq.toolkit.runtime;

import com.rtm.mq.toolkit.ir.MessageSchema;

/**
 * Pluggable message codec interface for marshal/unmarshal.
 */
public interface MessageCodec {
    byte[] marshal(Object pojo, MessageSchema schema);

    <T> ConversionResult<T> unmarshal(byte[] bytes, MessageSchema schema, Class<T> type);
}
