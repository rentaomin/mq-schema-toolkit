package com.rtm.mq.ir;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converter mapping configuration for generation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConverterMappingConfig {
    private Map<String, String> datatypeConverters = new LinkedHashMap<>();
    private Map<String, String> fieldOverrides = new LinkedHashMap<>();
    private String fallbackConverter = "stringFieldConverter";

    public Map<String, String> getDatatypeConverters() {
        return datatypeConverters;
    }

    public void setDatatypeConverters(Map<String, String> datatypeConverters) {
        this.datatypeConverters = datatypeConverters;
    }

    public Map<String, String> getFieldOverrides() {
        return fieldOverrides;
    }

    public void setFieldOverrides(Map<String, String> fieldOverrides) {
        this.fieldOverrides = fieldOverrides;
    }

    public String getFallbackConverter() {
        return fallbackConverter;
    }

    public void setFallbackConverter(String fallbackConverter) {
        this.fallbackConverter = fallbackConverter;
    }

    /**
     * Resolves converter by field path/name and datatype.
     *
     * @param datatype field datatype
     * @param fieldPath schema path
     * @param fieldName field name
     * @return converter resolution details
     */
    public ConverterResolution resolveConverter(String datatype, String fieldPath, String fieldName) {
        if (fieldPath != null && fieldOverrides.containsKey(fieldPath)) {
            return new ConverterResolution(fieldOverrides.get(fieldPath), false, "field override by path");
        }
        if (fieldName != null && fieldOverrides.containsKey(fieldName)) {
            return new ConverterResolution(fieldOverrides.get(fieldName), false, "field override by name");
        }
        String normalizedDatatype = normalize(datatype);
        if (normalizedDatatype != null) {
            for (Map.Entry<String, String> entry : datatypeConverters.entrySet()) {
                if (normalizedDatatype.equals(normalize(entry.getKey()))) {
                    return new ConverterResolution(entry.getValue(), false, "datatype mapping");
                }
            }
        }
        String fallback = fallbackConverter != null ? fallbackConverter : "stringFieldConverter";
        return new ConverterResolution(fallback, true, "fallback converter used");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
