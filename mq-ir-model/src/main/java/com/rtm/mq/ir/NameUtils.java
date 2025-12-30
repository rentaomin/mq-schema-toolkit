package com.rtm.mq.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * Naming helpers for schema identifiers.
 */
public final class NameUtils {
    private NameUtils() {
    }

    public static String toUpperCamel(String value) {
        if (value == null) {
            return "Unnamed";
        }
        String cleaned = value.replaceAll("[^A-Za-z0-9]", " ").trim();
        if (cleaned.isEmpty()) {
            return "Unnamed";
        }
        String[] parts = cleaned.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                tokens.add(normalizeToken(part, true));
            }
        }
        if (tokens.isEmpty()) {
            return "Unnamed";
        }
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            builder.append(token);
        }
        return builder.toString();
    }

    public static String toLowerCamel(String value) {
        String upper = toUpperCamel(value);
        if (upper.isEmpty()) {
            return upper;
        }
        return Character.toLowerCase(upper.charAt(0)) + upper.substring(1);
    }

    private static String normalizeToken(String token, boolean capitalize) {
        if (token.isEmpty()) {
            return token;
        }
        String normalized = token;
        if (token.equals(token.toUpperCase())) {
            normalized = token.toLowerCase();
        }
        if (!capitalize) {
            return normalized;
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
