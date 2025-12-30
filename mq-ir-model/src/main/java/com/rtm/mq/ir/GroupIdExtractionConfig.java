package com.rtm.mq.ir;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration for extracting groupId from description text.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupIdExtractionConfig {
    private List<String> patterns = new ArrayList<>();
    private int maxLength = 10;

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * Extracts a groupId value using configured patterns or fallback token.
     *
     * @param description description text
     * @return extracted groupId value
     */
    public String extractGroupId(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (String pattern : patterns) {
            Pattern compiled = Pattern.compile(pattern);
            Matcher matcher = compiled.matcher(trimmed);
            if (matcher.find()) {
                if (matcher.groupCount() >= 1) {
                    return matcher.group(1);
                }
                return matcher.group();
            }
        }
        String[] tokens = trimmed.split("\\s+");
        if (tokens.length == 0) {
            return null;
        }
        String token = tokens[0];
        if (token.length() > maxLength) {
            return token.substring(0, maxLength);
        }
        return token;
    }
}
