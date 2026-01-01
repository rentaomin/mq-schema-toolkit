package com.rtm.mq.toolkit.ingest.excel;

import com.rtm.mq.toolkit.ir.Direction;
import com.rtm.mq.toolkit.ir.FieldNode;
import com.rtm.mq.toolkit.ir.GenerationReport;
import com.rtm.mq.toolkit.ir.GroupIdExtractionConfig;
import com.rtm.mq.toolkit.ir.MessageSchema;
import com.rtm.mq.toolkit.ir.NameUtils;
import com.rtm.mq.toolkit.ir.Occurrence;
import com.rtm.mq.toolkit.ir.ProtocolFields;
import com.rtm.mq.toolkit.ir.SchemaElement;
import com.rtm.mq.toolkit.ir.SegmentNode;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.openxml4j.util.ZipSecureFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Imports Excel message specifications into IR schemas.
 */
public final class ExcelSchemaImporter {
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    public ImportResult importFile(Path excelPath, GroupIdExtractionConfig groupIdConfig) throws IOException {
        return importFile(excelPath, groupIdConfig, null);
    }

    public ImportResult importFile(Path excelPath,
                                   GroupIdExtractionConfig groupIdConfig,
                                   ExcelImportConfig importConfig) throws IOException {
        Objects.requireNonNull(excelPath, "excelPath");
        GenerationReport report = new GenerationReport();
        ExcelImportConfig config = importConfig != null ? importConfig : defaultConfig();
        configurePoiSecurity();

        try (InputStream input = Files.newInputStream(excelPath);
             Workbook workbook = WorkbookFactory.create(input)) {
            HeaderInfo headerInfo = readHeaderInfo(workbook, report, config);
            MessageSchema request = null;
            MessageSchema response = null;

            Sheet requestSheet = workbook.getSheet("Request");
            if (requestSheet != null) {
                request = parseSheet(requestSheet, headerInfo, Direction.REQUEST, groupIdConfig, report, config);
            }
            Sheet responseSheet = workbook.getSheet("Response");
            if (responseSheet != null) {
                response = parseSheet(responseSheet, headerInfo, Direction.RESPONSE, groupIdConfig, report, config);
            }

            return new ImportResult(request, response, report);
        } catch (Exception ex) {
            report.addIssue("ERROR", "Failed to import Excel file", ex.getMessage());
            if (ex instanceof IOException ioEx) {
                throw ioEx;
            }
            throw new IOException("Failed to import Excel file: " + ex.getMessage(), ex);
        }
    }

    private HeaderInfo readHeaderInfo(Workbook workbook, GenerationReport report, ExcelImportConfig config) {
        HeaderInfo info = null;
        Sheet shared = workbook.getSheet("Shared Header");
        if (shared != null) {
            info = readHeaderInfo(shared, config);
        }
        if (info == null || info.operationId == null || info.version == null) {
            Sheet request = workbook.getSheet("Request");
            if (request != null) {
                HeaderInfo fallback = readHeaderInfo(request, config);
                info = merge(info, fallback);
            }
        }
        if (info == null || info.operationId == null) {
            report.addIssue("WARN", "Operation ID missing; using placeholder", "OperationId=UnknownOperation");
            info = merge(info, new HeaderInfo("UnknownOperation", info != null ? info.version : "1.0", Map.of()));
        }
        if (info.version == null) {
            report.addIssue("WARN", "Version missing; using placeholder", "Version=1.0");
            info = new HeaderInfo(info.operationId, "1.0", info.sharedHeader);
        }
        return info;
    }

    private HeaderInfo merge(HeaderInfo primary, HeaderInfo fallback) {
        if (primary == null) {
            return fallback;
        }
        if (fallback == null) {
            return primary;
        }
        String operationId = primary.operationId != null ? primary.operationId : fallback.operationId;
        String version = primary.version != null ? primary.version : fallback.version;
        Map<String, String> header = new LinkedHashMap<>();
        if (fallback.sharedHeader != null) {
            header.putAll(fallback.sharedHeader);
        }
        if (primary.sharedHeader != null) {
            header.putAll(primary.sharedHeader);
        }
        return new HeaderInfo(operationId, version, header);
    }

    private HeaderInfo readHeaderInfo(Sheet sheet, ExcelImportConfig config) {
        Map<String, String> headerPairs = new LinkedHashMap<>();
        int scanLimit = Math.min(sheet.getLastRowNum(), config.getHeaderValueScanLimit());
        for (int i = 0; i <= scanLimit; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                String cellValue = normalizeHeader(cellText(cell));
                if ("operationid".equals(cellValue)) {
                    String value = cellText(row.getCell(cell.getColumnIndex() + 1));
                    headerPairs.put("operationId", value);
                }
            }
            Cell first = row.getCell(0);
            if (first != null) {
                String key = cellText(first);
                if (key != null && !key.isBlank()) {
                    String value = cellText(row.getCell(1));
                    if (value != null && !value.isBlank()) {
                        headerPairs.put(key.trim(), value.trim());
                    }
                }
            }
        }
        String version = findVersion(sheet, config);
        if (headerPairs.containsKey("operationId")) {
            return new HeaderInfo(headerPairs.get("operationId"), version, headerPairs);
        }
        return new HeaderInfo(null, version, headerPairs);
    }

    private String findVersion(Sheet sheet, ExcelImportConfig config) {
        int scanLimit = Math.min(sheet.getLastRowNum(), config.getHeaderValueScanLimit());
        for (int i = 0; i <= scanLimit; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                String cellValue = normalizeHeader(cellText(cell));
                if ("version".equals(cellValue)) {
                    return cellText(row.getCell(cell.getColumnIndex() + 1));
                }
            }
        }
        return null;
    }

    private MessageSchema parseSheet(Sheet sheet,
                                     HeaderInfo headerInfo,
                                     Direction direction,
                                     GroupIdExtractionConfig groupIdConfig,
                                     GenerationReport report,
                                     ExcelImportConfig config) {
        HeaderMapping mapping = resolveHeaderMapping(sheet, config);
        String rootName = NameUtils.toUpperCamel(headerInfo.operationId + " " + direction.name().toLowerCase());
        SegmentNode root = new SegmentNode();
        root.setName(rootName);
        root.setOriginalName(headerInfo.operationId);
        root.setDescription(direction.name().toLowerCase() + " root");

        MessageSchema schema = new MessageSchema();
        schema.setOperationId(headerInfo.operationId);
        schema.setVersion(headerInfo.version);
        schema.setDirection(direction);
        schema.setSchemaId(headerInfo.operationId + ":" + direction.name().toLowerCase() + ":v" + headerInfo.version);
        schema.setRoot(root);
        if (headerInfo.sharedHeader != null) {
            schema.setSharedHeader(headerInfo.sharedHeader);
        }

        List<SegmentNode> stack = new ArrayList<>();
        stack.add(root);

        boolean started = false;
        int emptyStreak = 0;
        int lastDataRow = findLastDataRow(sheet, mapping);
        for (int rowIndex = mapping.headerRow + 1; rowIndex <= lastDataRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String segLevelRaw = cellText(row.getCell(mapping.columnIndex.get(ColumnKey.SEG_LVL)));
            String fieldName = cellText(row.getCell(mapping.columnIndex.get(ColumnKey.FIELD_NAME)));
            String description = cellText(row.getCell(mapping.columnIndex.get(ColumnKey.DESCRIPTION)));
            String lengthRaw = cellText(row.getCell(mapping.columnIndex.get(ColumnKey.LENGTH)));
            String datatype = cellText(row.getCell(mapping.columnIndex.get(ColumnKey.DATATYPE)));
            String opt = cellText(row.getCell(mapping.columnIndex.get(ColumnKey.OPT)));
            String nullableRaw = cellText(row.getCell(mapping.columnIndex.get(ColumnKey.NULLABLE)));
            String sample = cellText(row.getCell(mapping.columnIndex.get(ColumnKey.SAMPLE)));
            Map<String, String> extras = mapping.extractExtras(row);

            if (isEmptyRow(segLevelRaw, fieldName, description, lengthRaw, datatype, opt, nullableRaw, sample)
                    && extras.isEmpty()) {
                if (started) {
                    emptyStreak++;
                    if (emptyStreak >= config.getMaxConsecutiveEmptyRows()) {
                        break;
                    }
                }
                continue;
            }
            started = true;
            emptyStreak = 0;

            if (segLevelRaw == null || segLevelRaw.isBlank()) {
                continue;
            }
            Integer segLevel = parseInt(segLevelRaw);
            if (segLevel == null || segLevel < 1) {
                report.addIssue("WARN", "Invalid segment level", "Row=" + (rowIndex + 1) + ", value=" + segLevelRaw);
                continue;
            }
            String trimmedFieldName = fieldName != null ? fieldName.trim() : "";
            boolean isSegmentRow = (lengthRaw == null || lengthRaw.isBlank()) && (datatype == null || datatype.isBlank());

            if (isSegmentRow) {
                SegmentNode segment = new SegmentNode();
                String segmentToken = trimmedFieldName;
                if (segmentToken.contains(":")) {
                    segmentToken = segmentToken.split(":", 2)[1];
                }
                segment.setName(NameUtils.toUpperCamel(segmentToken));
                segment.setOriginalName(trimmedFieldName);
                segment.setDescription(description);
                segment.setProtocol(new ProtocolFields());
                segment.setOccurrence(new Occurrence(1, 1));
                if (!extras.isEmpty()) {
                    segment.getExtensions().putAll(extras);
                }

                adjustStack(stack, segLevel, report);
                SegmentNode parent = stack.get(segLevel - 1);
                parent.getElements().add(segment);
                stack.add(segment);
            } else {
                SegmentNode current = stack.get(stack.size() - 1);
                if (matchesConfig(trimmedFieldName, config.getGroupIdFieldNames())) {
                    FieldNode groupId = buildFieldNode(trimmedFieldName, description, lengthRaw, datatype, opt, nullableRaw, sample);
                    groupId.setProtocol(true);
                    if (!extras.isEmpty()) {
                        groupId.getExtensions().putAll(extras);
                    }
                    String groupIdValue = groupIdConfig != null ? groupIdConfig.extractGroupId(description) : description;
                    if (groupIdValue != null) {
                        groupIdValue = groupIdValue.trim();
                    }
                    groupId.setDefaultValue(groupIdValue);
                    current.getProtocol().setGroupId(groupId);
                    current.getProtocol().setGroupIdValue(groupIdValue);
                    report.addGroupId(buildSegmentPath(stack), groupIdValue);
                } else if (matchesConfig(trimmedFieldName, config.getOccurrenceFieldNames())) {
                    FieldNode occurrenceCount = buildFieldNode(trimmedFieldName, description, lengthRaw, datatype, opt, nullableRaw, sample);
                    occurrenceCount.setProtocol(true);
                    if (!extras.isEmpty()) {
                        occurrenceCount.getExtensions().putAll(extras);
                    }
                    current.getProtocol().setOccurrenceCount(occurrenceCount);
                    Occurrence occurrence = parseOccurrence(description);
                    if (occurrence != null) {
                        current.setOccurrence(occurrence);
                    }
                } else {
                    FieldNode field = buildFieldNode(trimmedFieldName, description, lengthRaw, datatype, opt, nullableRaw, sample);
                    applyExtrasToField(field, extras);
                    current.getElements().add(field);
                }
            }
        }

        return schema;
    }

    private void adjustStack(List<SegmentNode> stack, int segLevel, GenerationReport report) {
        while (stack.size() > segLevel) {
            stack.remove(stack.size() - 1);
        }
        if (stack.size() < segLevel) {
            report.addIssue("WARN", "Segment level jump; attaching to last known parent", "Level=" + segLevel);
            while (stack.size() < segLevel) {
                stack.add(stack.get(stack.size() - 1));
            }
        }
    }

    private FieldNode buildFieldNode(String fieldName,
                                     String description,
                                     String lengthRaw,
                                     String datatype,
                                     String opt,
                                     String nullableRaw,
                                     String sample) {
        FieldNode field = new FieldNode();
        field.setName(NameUtils.toLowerCamel(fieldName));
        field.setOriginalName(fieldName);
        field.setDescription(description);
        field.setLengthBytes(parseInt(lengthRaw));
        field.setDatatype(datatype != null && !datatype.isBlank() ? datatype.trim() : null);
        field.setRequired("M".equalsIgnoreCase(opt != null ? opt.trim() : ""));
        if (nullableRaw != null && !nullableRaw.isBlank()) {
            field.setNullable("Y".equalsIgnoreCase(nullableRaw.trim()));
        }
        field.setExample(sample);
        return field;
    }

    private void applyExtrasToField(FieldNode field, Map<String, String> extras) {
        if (extras.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : extras.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String normalized = normalizeHeader(key);
            if (normalized.contains("hardcode") || normalized.contains("hardcodevalue")) {
                if (value != null && value.equalsIgnoreCase("BLANK") && field.getLengthBytes() != null) {
                    field.setDefaultValue(" ".repeat(field.getLengthBytes()));
                } else {
                    field.setDefaultValue(value);
                }
            } else if (normalized.contains("testvalue") || normalized.contains("sample")) {
                if (field.getExample() == null) {
                    field.setExample(value);
                }
            } else if (normalized.contains("nls")) {
                field.getExtensions().put(key, value);
            } else if (normalized.contains("refid") || normalized.contains("comments") || normalized.contains("remarks")) {
                field.getExtensions().put(key, value);
            } else if (normalized.contains("smpfield")) {
                field.getExtensions().put(key, value);
            } else {
                field.getExtensions().put(key, value);
            }
        }
    }

    private Occurrence parseOccurrence(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        if (!trimmed.contains("..")) {
            return null;
        }
        String[] parts = trimmed.split("\\.\\.");
        if (parts.length != 2) {
            return null;
        }
        Integer min = parseInt(parts[0]);
        Integer max = "N".equalsIgnoreCase(parts[1].trim()) ? null : parseInt(parts[1]);
        if (min == null) {
            return null;
        }
        return new Occurrence(min, max);
    }

    private boolean matchesConfig(String value, List<String> candidates) {
        if (value == null || candidates == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate != null && value.equalsIgnoreCase(candidate.trim())) {
                return true;
            }
        }
        return false;
    }

    private HeaderMapping resolveHeaderMapping(Sheet sheet, ExcelImportConfig config) {
        Map<String, ColumnKey> synonyms = defaultSynonyms();
        if (config.getColumnAliases() != null) {
            for (Map.Entry<String, List<String>> entry : config.getColumnAliases().entrySet()) {
                ColumnKey key = ColumnKey.fromKey(entry.getKey());
                if (key != null) {
                    for (String alias : entry.getValue()) {
                        synonyms.put(normalizeHeader(alias), key);
                    }
                }
            }
        }

        int scanLimit = Math.min(sheet.getLastRowNum(), config.getHeaderScanLimit());
        for (int rowIndex = 0; rowIndex <= scanLimit; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            EnumMap<ColumnKey, Integer> columns = new EnumMap<>(ColumnKey.class);
            Map<Integer, String> headers = new LinkedHashMap<>();
            for (Cell cell : row) {
                String rawHeader = cellText(cell);
                String normalized = normalizeHeader(rawHeader);
                ColumnKey key = synonyms.get(normalized);
                if (key != null) {
                    columns.putIfAbsent(key, cell.getColumnIndex());
                }
                if (rawHeader != null && !rawHeader.isBlank()) {
                    headers.put(cell.getColumnIndex(), rawHeader.trim());
                }
            }
            if (columns.keySet().containsAll(requiredColumns(config))) {
                return new HeaderMapping(rowIndex, columns, headers, config.isCaptureExtraColumns());
            }
        }
        throw new IllegalStateException("Missing required columns: " + requiredColumns(config));
    }

    private String buildSegmentPath(List<SegmentNode> stack) {
        StringBuilder builder = new StringBuilder();
        for (SegmentNode segment : stack) {
            builder.append('/').append(segment.getName());
        }
        return builder.toString();
    }

    private boolean isEmptyRow(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private int findLastDataRow(Sheet sheet, HeaderMapping mapping) {
        int last = mapping.headerRow + 1;
        for (int i = sheet.getLastRowNum(); i >= mapping.headerRow + 1; i--) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            boolean hasValue = false;
            for (Cell cell : row) {
                String v = cellText(cell);
                if (v != null && !v.isBlank()) {
                    hasValue = true;
                    break;
                }
            }
            if (hasValue) {
                last = i;
                break;
            }
        }
        return last;
    }

    private static String cellText(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            String value = DATA_FORMATTER.formatCellValue(cell);
            return sanitize(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (value.contains(".")) {
                double parsed = Double.parseDouble(value);
                return (int) parsed;
            }
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private record HeaderInfo(String operationId, String version, Map<String, String> sharedHeader) {
    }

    private record HeaderMapping(int headerRow,
                                 EnumMap<ColumnKey, Integer> columnIndex,
                                 Map<Integer, String> headers,
                                 boolean captureExtras) {
        Map<String, String> extractExtras(Row row) {
            if (!captureExtras) {
                return Map.of();
            }
            Map<String, String> extras = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> entry : headers.entrySet()) {
                if (columnIndex.containsValue(entry.getKey())) {
                    continue;
                }
                String value = cellText(row.getCell(entry.getKey()));
                if (value != null && !value.isBlank()) {
                    extras.put(entry.getValue(), value.trim());
                }
            }
            return extras;
        }
    }

    private enum ColumnKey {
        SEG_LVL,
        FIELD_NAME,
        DESCRIPTION,
        LENGTH,
        DATATYPE,
        OPT,
        NULLABLE,
        NLS,
        SAMPLE;

        private static ColumnKey fromKey(String key) {
            if (key == null) {
                return null;
            }
            return switch (normalizeHeader(key)) {
                case "seglvl", "seglevel", "segmentlevel" -> SEG_LVL;
                case "fieldname", "field" -> FIELD_NAME;
                case "description", "desc" -> DESCRIPTION;
                case "length", "len" -> LENGTH;
                case "messagingdatatype", "datatype" -> DATATYPE;
                case "optom", "opt" -> OPT;
                case "null", "nullyn", "nullable" -> NULLABLE;
                case "nls" -> NLS;
                case "samplevalues", "samplevalue", "sample" -> SAMPLE;
                default -> null;
            };
        }
    }

    private Map<String, ColumnKey> defaultSynonyms() {
        Map<String, ColumnKey> synonyms = new HashMap<>();
        synonyms.put("seglvl", ColumnKey.SEG_LVL);
        synonyms.put("seglevel", ColumnKey.SEG_LVL);
        synonyms.put("segmentlevel", ColumnKey.SEG_LVL);
        synonyms.put("fieldname", ColumnKey.FIELD_NAME);
        synonyms.put("field", ColumnKey.FIELD_NAME);
        synonyms.put("description", ColumnKey.DESCRIPTION);
        synonyms.put("desc", ColumnKey.DESCRIPTION);
        synonyms.put("length", ColumnKey.LENGTH);
        synonyms.put("len", ColumnKey.LENGTH);
        synonyms.put("messagingdatatype", ColumnKey.DATATYPE);
        synonyms.put("datatype", ColumnKey.DATATYPE);
        synonyms.put("optom", ColumnKey.OPT);
        synonyms.put("opt", ColumnKey.OPT);
        synonyms.put("null", ColumnKey.NULLABLE);
        synonyms.put("nullyn", ColumnKey.NULLABLE);
        synonyms.put("nullable", ColumnKey.NULLABLE);
        synonyms.put("nls", ColumnKey.NLS);
        synonyms.put("samplevalues", ColumnKey.SAMPLE);
        synonyms.put("samplevalue", ColumnKey.SAMPLE);
        synonyms.put("sample", ColumnKey.SAMPLE);
        return synonyms;
    }

    private ExcelImportConfig defaultConfig() {
        ExcelImportConfig config = new ExcelImportConfig();
        return config;
    }

    private List<ColumnKey> requiredColumns(ExcelImportConfig config) {
        List<ColumnKey> defaults = List.of(ColumnKey.SEG_LVL, ColumnKey.FIELD_NAME, ColumnKey.DESCRIPTION,
                ColumnKey.LENGTH, ColumnKey.DATATYPE, ColumnKey.OPT, ColumnKey.NULLABLE, ColumnKey.NLS);
        if (config.getRequiredColumns() == null || config.getRequiredColumns().isEmpty()) {
            return defaults;
        }
        List<ColumnKey> result = new ArrayList<>();
        for (String name : config.getRequiredColumns()) {
            ColumnKey key = ColumnKey.fromKey(name);
            if (key != null) {
                result.add(key);
            }
        }
        return result.isEmpty() ? defaults : result;
    }

    private void configurePoiSecurity() {
        ZipSecureFile.setMinInflateRatio(0.01d);
        ZipSecureFile.setMaxEntrySize(200L * 1024 * 1024);
        ZipSecureFile.setMaxTextSize(200L * 1024 * 1024);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                builder.append(' ');
            } else {
                builder.append(c);
            }
        }
        String cleaned = builder.toString().trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
