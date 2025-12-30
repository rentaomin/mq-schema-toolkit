package com.rtm.mq.runtime;

/**
 * Conversion issue captured during marshal/unmarshal.
 */
public record ConversionIssue(String level, String path, String message) {
}
