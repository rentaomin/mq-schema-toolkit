package com.rtm.mq.spec.excel;

import com.rtm.mq.ir.Direction;
import com.rtm.mq.ir.FieldNode;
import com.rtm.mq.ir.GenerationReport;
import com.rtm.mq.ir.GroupIdExtractionConfig;
import com.rtm.mq.ir.MessageSchema;
import com.rtm.mq.ir.NameUtils;
import com.rtm.mq.ir.Occurrence;
import com.rtm.mq.ir.ProtocolFields;
import com.rtm.mq.ir.SchemaElement;
import com.rtm.mq.ir.SegmentNode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Imports Excel message specifications into IR schemas.
 */
public final class ExcelSchemaImporter {
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    public ImportResult importFile(Path excelPath, GroupIdExtractionConfig groupIdConfig) throws IOException {
        Objects.requireNonNull(excelPath, "excelPath");
        GenerationReport report = new GenerationReport();

        try (InputStream input = Files.newInputStream(excelPath);
             Workbook workbook = WorkbookFactory.create(input)) {
            HeaderInfo headerInfo = readHeaderInfo(workbook, report);
            MessageSchema request = null;
            MessageSchema response = null;

            Sheet requestSheet = workbook.getSheet("Request");
            if (requestSheet != null) {
                request = parseSheet(requestSheet, headerInfo, Direction.REQUEST, groupIdConfig, report);
            }
            Sheet responseSheet = workbook.getSheet("Response");
            if (responseSheet != null) {
                response = parseSheet(responseSheet, headerInfo, Direction.RESPONSE, groupIdConfig, report);
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

    private HeaderInfo readHeaderInfo(Workbook workbook, GenerationReport report) {
        HeaderInfo info = null;
        Sheet shared = workbook.getSheet("Shared Header");
        if (shared != null) {
            info = readHeaderInfo(shared);
        }
        if (info == null || info.operationId == null || info.version == null) {
            Sheet request = workbook.getSheet("Request");
            if (request != null) {
                HeaderInfo fallback = readHeaderInfo(request);
                info = merge(info, fallback);
            }
        }
        if (info == null || info.operationId == null) {
            report.addIssue("WARN", "Operation ID missing; using placeholder", "OperationId=UnknownOperation");
            info = merge(info, new HeaderInfo("UnknownOperation", info != null ? info.version : "1.0"));
        }
        if (info.version == null) {
            report.addIssue("WARN", "Version missing; using placeholder", "Version=1.0");
            info = new HeaderInfo(info.operationId, "1.0");
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
        return new HeaderInfo(operationId, version);
    }

    private HeaderInfo readHeaderInfo(Sheet sheet) {
        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 20); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                String cellValue = normalizeHeader(cellText(cell));
                if ("operationid".equals(cellValue)) {
                    String value = cellText(row.getCell(cell.getColumnIndex() + 1));
                    return new HeaderInfo(value, findVersion(sheet));
                }
            }
        }
        return new HeaderInfo(null, findVersion(sheet));
    }

    private String findVersion(Sheet sheet) {
        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 20); i++) {
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
                                     GenerationReport report) {
        HeaderMapping mapping = resolveHeaderMapping(sheet);
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

        List<SegmentNode> stack = new ArrayList<>();
        stack.add(root);

        boolean started = false;
        int emptyStreak = 0;
        for (int rowIndex = mapping.headerRow + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
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

            if (isEmptyRow(segLevelRaw, fieldName, description, lengthRaw, datatype, opt, nullableRaw, sample)) {
                if (started) {
                    emptyStreak++;
                    if (emptyStreak >= 2) {
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
                    segmentToken = segmentToken.split(":", 2)[0];
                }
                segment.setName(NameUtils.toUpperCamel(segmentToken));
                segment.setOriginalName(trimmedFieldName);
                segment.setDescription(description);
                segment.setProtocol(new ProtocolFields());
                segment.setOccurrence(new Occurrence(1, 1));

                adjustStack(stack, segLevel, report);
                SegmentNode parent = stack.get(segLevel - 1);
                parent.getElements().add(segment);
                stack.add(segment);
            } else {
                SegmentNode current = stack.get(stack.size() - 1);
                if (trimmedFieldName.equalsIgnoreCase("groupid")) {
                    FieldNode groupId = buildFieldNode(trimmedFieldName, description, lengthRaw, datatype, opt, nullableRaw, sample);
                    groupId.setProtocol(true);
                    String groupIdValue = groupIdConfig != null ? groupIdConfig.extractGroupId(description) : description;
                    if (groupIdValue != null) {
                        groupIdValue = groupIdValue.trim();
                    }
                    groupId.setDefaultValue(groupIdValue);
                    current.getProtocol().setGroupId(groupId);
                    current.getProtocol().setGroupIdValue(groupIdValue);
                    report.addGroupId(buildSegmentPath(stack), groupIdValue);
                } else if (trimmedFieldName.equalsIgnoreCase("occurenceCount")
                        || trimmedFieldName.equalsIgnoreCase("occurrenceCount")) {
                    FieldNode occurrenceCount = buildFieldNode(trimmedFieldName, description, lengthRaw, datatype, opt, nullableRaw, sample);
                    occurrenceCount.setProtocol(true);
                    current.getProtocol().setOccurrenceCount(occurrenceCount);
                    Occurrence occurrence = parseOccurrence(description);
                    if (occurrence != null) {
                        current.setOccurrence(occurrence);
                    }
                } else {
                    FieldNode field = buildFieldNode(trimmedFieldName, description, lengthRaw, datatype, opt, nullableRaw, sample);
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

    private HeaderMapping resolveHeaderMapping(Sheet sheet) {
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
        synonyms.put("samplevalues", ColumnKey.SAMPLE);
        synonyms.put("samplevalue", ColumnKey.SAMPLE);
        synonyms.put("sample", ColumnKey.SAMPLE);

        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            EnumMap<ColumnKey, Integer> columns = new EnumMap<>(ColumnKey.class);
            for (Cell cell : row) {
                String normalized = normalizeHeader(cellText(cell));
                ColumnKey key = synonyms.get(normalized);
                if (key != null) {
                    columns.putIfAbsent(key, cell.getColumnIndex());
                }
            }
            if (columns.keySet().containsAll(ColumnKey.required())) {
                return new HeaderMapping(rowIndex, columns);
            }
        }
        throw new IllegalStateException("Missing required columns: " + ColumnKey.required());
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

    private String cellText(Cell cell) {
        if (cell == null) {
            return null;
        }
        String value = DATA_FORMATTER.formatCellValue(cell);
        return value != null ? value.trim() : null;
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

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private record HeaderInfo(String operationId, String version) {
    }

    private record HeaderMapping(int headerRow, EnumMap<ColumnKey, Integer> columnIndex) {
    }

    private enum ColumnKey {
        SEG_LVL,
        FIELD_NAME,
        DESCRIPTION,
        LENGTH,
        DATATYPE,
        OPT,
        NULLABLE,
        SAMPLE;

        private static List<ColumnKey> required() {
            return List.of(SEG_LVL, FIELD_NAME, DESCRIPTION, LENGTH, DATATYPE, OPT, NULLABLE, SAMPLE);
        }
    }
}
