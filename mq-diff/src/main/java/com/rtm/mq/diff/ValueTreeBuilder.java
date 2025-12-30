package com.rtm.mq.diff;

import com.rtm.mq.ir.FieldNode;
import com.rtm.mq.ir.NameUtils;
import com.rtm.mq.ir.SchemaElement;
import com.rtm.mq.ir.SegmentNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds neutral Map/List tree from POJO and schema.
 */
public final class ValueTreeBuilder {
    public Map<String, Object> build(SegmentNode root, Object pojo) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (pojo == null) {
            return result;
        }
        for (SchemaElement element : root.getElements()) {
            if (element instanceof FieldNode field) {
                if (field.isProtocol()) {
                    continue;
                }
                result.put(field.getName(), readProperty(pojo, field.getName()));
            } else if (element instanceof SegmentNode child) {
                String propertyName = NameUtils.toLowerCamel(child.getName());
                Object value = readProperty(pojo, propertyName);
                if (child.getOccurrence() != null && child.getOccurrence().isRepeating()) {
                    List<Object> items = new ArrayList<>();
                    if (value instanceof List<?> list) {
                        for (Object entry : list) {
                            items.add(build(child, entry));
                        }
                    }
                    result.put(propertyName, items);
                } else {
                    result.put(propertyName, build(child, value));
                }
            }
        }
        return result;
    }

    private Object readProperty(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return null;
        }
        Method getter = findGetter(target.getClass(), fieldName);
        if (getter != null) {
            try {
                return getter.invoke(target);
            } catch (Exception ignored) {
            }
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Method findGetter(Class<?> type, String fieldName) {
        String methodName = "get" + NameUtils.toUpperCamel(fieldName);
        try {
            return type.getMethod(methodName);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }
}
