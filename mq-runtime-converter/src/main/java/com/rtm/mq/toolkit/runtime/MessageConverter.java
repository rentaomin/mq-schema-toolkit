package com.rtm.mq.toolkit.runtime;

import com.rtm.mq.toolkit.ir.FieldNode;
import com.rtm.mq.toolkit.ir.MessageSchema;
import com.rtm.mq.toolkit.ir.NameUtils;
import com.rtm.mq.toolkit.ir.Occurrence;
import com.rtm.mq.toolkit.ir.SchemaElement;
import com.rtm.mq.toolkit.ir.SegmentNode;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Schema-driven marshal/unmarshal for fixed-length messages.
 */
public final class MessageConverter implements MessageCodec {
    private final ConversionOptions options;

    public MessageConverter() {
        this(new ConversionOptions());
    }

    public MessageConverter(ConversionOptions options) {
        this.options = options;
    }

    /**
     * Marshals a POJO to bytes using schema definition.
     *
     * @param pojo   root object
     * @param schema schema definition
     * @return fixed-length message bytes
     */
    public byte[] marshal(Object pojo, MessageSchema schema) {
        Objects.requireNonNull(pojo, "pojo");
        Objects.requireNonNull(schema, "schema");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeSegmentElements(schema.getRoot(), pojo, output, "/" + schema.getRoot().getName());
        return output.toByteArray();
    }

    /**
     * Unmarshals bytes into a POJO using schema definition.
     *
     * @param bytes  message bytes
     * @param schema schema definition
     * @param type   root class
     * @param <T>    type of root object
     * @return conversion result with trace metadata
     */
    public <T> ConversionResult<T> unmarshal(byte[] bytes, MessageSchema schema, Class<T> type) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(type, "type");

        ConversionResult<T> result = new ConversionResult<>(instantiate(type));
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(resolveByteOrder());
        readSegmentElements(schema.getRoot(), result.value(), buffer, result, "/" + schema.getRoot().getName());
        return result;
    }

    private void writeSegmentElements(SegmentNode segment, Object target, ByteArrayOutputStream output, String path) {
        for (SchemaElement element : segment.getElements()) {
            if (element instanceof FieldNode field) {
                writeField(field, readProperty(target, field.getName()), output);
            } else if (element instanceof SegmentNode child) {
                writeSegment(child, target, output, path + "/" + child.getName());
            }
        }
    }

    private void writeSegment(SegmentNode segment, Object parent, ByteArrayOutputStream output, String path) {
        Object value = readProperty(parent, NameUtils.toLowerCamel(segment.getName()));
        Occurrence occurrence = segment.getOccurrence() != null ? segment.getOccurrence() : new Occurrence(1, 1);
        int count = determineCount(value, occurrence, path);
        writeGroupId(segment, output);
        writeOccurrenceCount(count, output);

        if (occurrence.isRepeating()) {
            if (value instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    Object entry = list.get(i);
                    writeSegmentElements(segment, entry, output, path + "[" + i + "]");
                }
            }
        } else if (value != null) {
            writeSegmentElements(segment, value, output, path);
        }
    }

    private void readSegmentElements(SegmentNode segment,
                                     Object target,
                                     ByteBuffer buffer,
                                     ConversionResult<?> result,
                                     String path) {
        for (SchemaElement element : segment.getElements()) {
            if (element instanceof FieldNode field) {
                Object value = readField(field, buffer, result, path + "/" + field.getName());
                writeProperty(target, field.getName(), value);
            } else if (element instanceof SegmentNode child) {
                readSegment(child, target, buffer, result, path + "/" + child.getName());
            }
        }
    }

    private void readSegment(SegmentNode segment,
                             Object parent,
                             ByteBuffer buffer,
                             ConversionResult<?> result,
                             String path) {
        String groupIdField = options.getProtocolConfig().getGroupIdFieldName();
        String occurrenceField = options.getProtocolConfig().getOccurrenceFieldName();
        String groupId = readGroupId(segment, buffer, result, path + "/" + groupIdField);
        if (segment.getProtocol() != null && segment.getProtocol().getGroupIdValue() != null) {
            String expected = segment.getProtocol().getGroupIdValue();
            if (!expected.equals(groupId)) {
                String message = "Expected groupId " + expected + " but found " + groupId;
                if (options.getGroupIdMode() == GroupIdMode.STRICT) {
                    result.issues().add(new ConversionIssue("ERROR", path, message));
                } else {
                    result.issues().add(new ConversionIssue("WARN", path, message));
                }
            }
        }
        long count = readOccurrenceCount(segment, buffer, result, path + "/" + occurrenceField);
        Occurrence occurrence = segment.getOccurrence() != null ? segment.getOccurrence() : new Occurrence(1, 1);
        if (!occurrence.isRepeating() && count != 1) {
            result.issues().add(new ConversionIssue("WARN", path, "Expected occurenceCount 1 but found " + count));
        }

        if (occurrence.isRepeating()) {
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Object entry = instantiate(resolveSegmentClass(parent, segment));
                readSegmentElements(segment, entry, buffer, result, path + "[" + i + "]");
                list.add(entry);
            }
            writeProperty(parent, NameUtils.toLowerCamel(segment.getName()), list);
        } else {
            Object entry = instantiate(resolveSegmentClass(parent, segment));
            readSegmentElements(segment, entry, buffer, result, path);
            writeProperty(parent, NameUtils.toLowerCamel(segment.getName()), entry);
        }
    }

    private String readGroupId(SegmentNode segment, ByteBuffer buffer, ConversionResult<?> result, String path) {
        int start = buffer.position();
        int length = resolveGroupIdLength(segment);
        byte[] data = new byte[length];
        buffer.get(data);
        result.traces().add(new FieldTrace(path, start, data.length));
        return decodeString(data);
    }

    private void writeGroupId(SegmentNode segment, ByteArrayOutputStream output) {
        String value = segment.getProtocol() != null ? segment.getProtocol().getGroupIdValue() : null;
        writeString(value, resolveGroupIdLength(segment), output);
    }

    private long readOccurrenceCount(SegmentNode segment,
                                     ByteBuffer buffer,
                                     ConversionResult<?> result,
                                     String path) {
        int start = buffer.position();
        int length = resolveOccurrenceLength(segment);
        byte[] data = new byte[length];
        buffer.get(data);
        result.traces().add(new FieldTrace(path, start, length));
        if (length == 4) {
            return Integer.toUnsignedLong(ByteBuffer.wrap(data).order(resolveByteOrder()).getInt());
        }
        String text = decodeString(data);
        if (text == null || text.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            result.issues().add(new ConversionIssue("WARN", path, "Invalid occurrence value: " + text));
            return 0L;
        }
    }

    private void writeOccurrenceCount(long value, ByteArrayOutputStream output) {
        if (value < 0 || value > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("OccurrenceCount out of range: " + value);
        }
        int length = resolveOccurrenceLength(null);
        if (length == 4) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(resolveByteOrder());
            buffer.putInt((int) value);
            output.writeBytes(buffer.array());
            return;
        }
        writeString(String.valueOf(value), length, output);
    }

    private Object readField(FieldNode field,
                             ByteBuffer buffer,
                             ConversionResult<?> result,
                             String path) {
        if (field.getLengthBytes() == null) {
            return null;
        }
        int start = buffer.position();
        byte[] data = new byte[field.getLengthBytes()];
        buffer.get(data);
        result.traces().add(new FieldTrace(path, start, data.length));

        if (isBinaryUnsigned(field)) {
            if (data.length != 4) {
                result.issues().add(new ConversionIssue("WARN", path, "Expected 4 bytes for unsigned integer"));
                return decodeString(data);
            }
            return Integer.toUnsignedLong(ByteBuffer.wrap(data).order(resolveByteOrder()).getInt());
        }
        return decodeString(data);
    }

    private void writeField(FieldNode field, Object value, ByteArrayOutputStream output) {
        if (field.getLengthBytes() == null) {
            return;
        }
        int length = field.getLengthBytes();
        if (isBinaryUnsigned(field) && length == 4) {
            long number = value == null ? 0L : ((Number) value).longValue();
            if (number < 0 || number > 0xFFFF_FFFFL) {
                throw new IllegalArgumentException("Unsigned int out of range for " + field.getName());
            }
            ByteBuffer buffer = ByteBuffer.allocate(4).order(resolveByteOrder());
            buffer.putInt((int) number);
            output.writeBytes(buffer.array());
            return;
        }
        String text = value == null ? null : String.valueOf(value);
        writeString(text, length, output);
    }

    private boolean isBinaryUnsigned(FieldNode field) {
        String datatype = field.getDatatype() != null ? field.getDatatype().toLowerCase() : "";
        return datatype.contains("unsigned") || datatype.contains("integer") || datatype.contains("long");
    }

    private int resolveGroupIdLength(SegmentNode segment) {
        if (segment != null && segment.getProtocol() != null && segment.getProtocol().getGroupId() != null
                && segment.getProtocol().getGroupId().getLengthBytes() != null) {
            return segment.getProtocol().getGroupId().getLengthBytes();
        }
        return options.getProtocolConfig().getGroupIdLength();
    }

    private int resolveOccurrenceLength(SegmentNode segment) {
        if (segment != null && segment.getProtocol() != null && segment.getProtocol().getOccurrenceCount() != null
                && segment.getProtocol().getOccurrenceCount().getLengthBytes() != null) {
            return segment.getProtocol().getOccurrenceCount().getLengthBytes();
        }
        return options.getProtocolConfig().getOccurrenceLength();
    }

    private ByteOrder resolveByteOrder() {
        String order = options.getProtocolConfig().getByteOrder();
        if (order != null && order.equalsIgnoreCase("LITTLE_ENDIAN")) {
            return ByteOrder.LITTLE_ENDIAN;
        }
        return ByteOrder.BIG_ENDIAN;
    }

    private String decodeString(byte[] data) {
        String raw = new String(data, StandardCharsets.US_ASCII);
        if (raw.trim().isEmpty()) {
            return null;
        }
        int end = raw.length();
        while (end > 0 && raw.charAt(end - 1) == ' ') {
            end--;
        }
        return raw.substring(0, end);
    }

    private void writeString(String value, int length, ByteArrayOutputStream output) {
        String text = value == null ? "" : value;
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length > length) {
            throw new IllegalArgumentException("Value too long for field length " + length);
        }
        byte[] padded = Arrays.copyOf(bytes, length);
        Arrays.fill(padded, bytes.length, length, (byte) ' ');
        output.writeBytes(padded);
    }

    private int determineCount(Object value, Occurrence occurrence, String path) {
        if (occurrence.isRepeating()) {
            if (value == null) {
                if (occurrence.getMinOccurs() > 0) {
                    throw new IllegalArgumentException("Missing required list segment at " + path);
                }
                return 0;
            }
            if (!(value instanceof List<?> list)) {
                throw new IllegalArgumentException("Expected list for repeating segment at " + path);
            }
            return list.size();
        }
        if (value == null) {
            if (occurrence.getMinOccurs() > 0) {
                throw new IllegalArgumentException("Missing required segment at " + path);
            }
            return 0;
        }
        return 1;
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

    private void writeProperty(Object target, String fieldName, Object value) {
        if (target == null || fieldName == null) {
            return;
        }
        Method setter = findSetter(target.getClass(), fieldName, value);
        if (setter != null) {
            try {
                setter.invoke(target, value);
                return;
            } catch (Exception ignored) {
                return;
            }
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ignored) {
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

    private Method findSetter(Class<?> type, String fieldName, Object value) {
        String methodName = "set" + NameUtils.toUpperCamel(fieldName);
        Method[] methods = type.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                if (value == null || method.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                    return method;
                }
            }
        }
        return null;
    }

    private Class<?> resolveSegmentClass(Object parent, SegmentNode segment) {
        if (parent != null) {
            String basePackage = parent.getClass().getPackageName();
            try {
                return Class.forName(basePackage + "." + segment.getName());
            } catch (ClassNotFoundException ignored) {
            }
            try {
                Field field = parent.getClass().getDeclaredField(NameUtils.toLowerCamel(segment.getName()));
                Class<?> type = field.getType();
                if (!List.class.isAssignableFrom(type)) {
                    return type;
                }
            } catch (NoSuchFieldException ignored) {
            }
            Method getter = findGetter(parent.getClass(), NameUtils.toLowerCamel(segment.getName()));
            if (getter != null && !List.class.isAssignableFrom(getter.getReturnType())) {
                return getter.getReturnType();
            }
        }
        throw new IllegalStateException("Unable to resolve class for segment " + segment.getName());
    }

    private <T> T instantiate(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to instantiate " + type.getName(), ex);
        }
    }
}
