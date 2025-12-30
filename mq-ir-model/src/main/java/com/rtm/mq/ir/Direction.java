package com.rtm.mq.ir;

/**
 * Message direction in the schema repository.
 */
public enum Direction {
    REQUEST,
    RESPONSE;

    /**
     * Parses a direction string in a tolerant way.
     *
     * @param value input value
     * @return parsed direction
     */
    public static Direction fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "request", "req" -> REQUEST;
            case "response", "resp" -> RESPONSE;
            default -> throw new IllegalArgumentException("Unsupported direction: " + value);
        };
    }
}
