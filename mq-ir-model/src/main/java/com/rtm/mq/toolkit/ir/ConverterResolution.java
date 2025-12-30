package com.rtm.mq.toolkit.ir;

/**
 * Converter lookup result with fallback metadata.
 */
public record ConverterResolution(String converter, boolean usedFallback, String reason) {
}
