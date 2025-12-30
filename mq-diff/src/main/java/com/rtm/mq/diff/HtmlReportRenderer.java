package com.rtm.mq.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders diff report as HTML.
 */
public final class HtmlReportRenderer {
    public String render(DiffReport report, byte[] expected, byte[] actual) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!doctype html>\n");
        builder.append("<html><head><meta charset=\"utf-8\" />\n");
        builder.append("<style>");
        builder.append("body{font-family:Arial, sans-serif;padding:16px;}");
        builder.append("table{border-collapse:collapse;width:100%;}");
        builder.append("th,td{border:1px solid #ccc;padding:6px;vertical-align:top;}");
        builder.append("th{background:#f3f3f3;}");
        builder.append("mark{background:#ffef8a;}");
        builder.append("pre{white-space:pre-wrap;word-break:break-all;}");
        builder.append("</style></head><body>\n");
        builder.append("<h2>Diff Report</h2>\n");
        builder.append("<table>\n");
        builder.append("<tr><th>Path</th><th>Kind</th><th>Expected</th><th>Actual</th><th>Expected Offset</th><th>Actual Offset</th></tr>\n");
        for (DiffEntry entry : report.getEntries()) {
            builder.append("<tr>");
            builder.append("<td>").append(escape(entry.path())).append("</td>");
            builder.append("<td>").append(entry.kind()).append("</td>");
            builder.append("<td>").append(escape(String.valueOf(entry.expected()))).append("</td>");
            builder.append("<td>").append(escape(String.valueOf(entry.actual()))).append("</td>");
            builder.append("<td>").append(formatOffset(entry.expectedOffset())).append("</td>");
            builder.append("<td>").append(formatOffset(entry.actualOffset())).append("</td>");
            builder.append("</tr>\n");
        }
        builder.append("</table>\n");

        if (expected != null) {
            builder.append("<h3>Expected Message</h3>\n");
            builder.append("<pre>").append(highlightHex(expected, report, true)).append("</pre>\n");
        }
        if (actual != null) {
            builder.append("<h3>Actual Message</h3>\n");
            builder.append("<pre>").append(highlightHex(actual, report, false)).append("</pre>\n");
        }
        builder.append("</body></html>\n");
        return builder.toString();
    }

    private String highlightHex(byte[] data, DiffReport report, boolean expected) {
        boolean[] highlight = new boolean[data.length];
        for (DiffEntry entry : report.getEntries()) {
            DiffEntry.Offset offset = expected ? entry.expectedOffset() : entry.actualOffset();
            if (offset == null) {
                continue;
            }
            int start = offset.start();
            int end = Math.min(data.length, start + offset.length());
            for (int i = start; i < end; i++) {
                if (i >= 0 && i < highlight.length) {
                    highlight[i] = true;
                }
            }
        }
        List<String> parts = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            String hex = String.format("%02X", data[i]);
            if (highlight[i]) {
                parts.add("<mark>" + hex + "</mark>");
            } else {
                parts.add(hex);
            }
        }
        return String.join(" ", parts);
    }

    private String formatOffset(DiffEntry.Offset offset) {
        if (offset == null) {
            return "";
        }
        return offset.start() + ":" + offset.length();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
