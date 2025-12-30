package com.rtm.mq.toolkit.runtime;

/**
 * Conversion issue captured during marshal/unmarshal.
 */
public record ConversionIssue(String level, String path, String message) {
}
