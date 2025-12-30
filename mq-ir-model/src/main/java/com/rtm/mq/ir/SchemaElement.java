package com.rtm.mq.ir;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base element for schema trees.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SegmentNode.class, name = "segment"),
        @JsonSubTypes.Type(value = FieldNode.class, name = "field")
})
public sealed interface SchemaElement permits SegmentNode, FieldNode {
    String getName();
}
