package com.acme.supportpilot.eval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small CSV reader for evaluation data: a header row, commas between fields, and double quotes
 * around fields that contain commas, quotes ("") or line breaks. Each row becomes a map from
 * column name to value.
 */
public final class EvalCsv {

    private EvalCsv() {
    }

    public static List<Map<String, String>> read(Path file) throws IOException {
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    static List<Map<String, String>> parse(String text) {
        List<List<String>> rows = rows(text.replace("\r\n", "\n"));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> header = rows.get(0);
        List<Map<String, String>> result = new ArrayList<>();
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.size() == 1 && row.get(0).isBlank()) {
                continue;   // empty line
            }
            if (row.size() != header.size()) {
                throw new IllegalArgumentException("Row " + (r + 1) + " has " + row.size() + " fields, expected " + header.size());
            }
            Map<String, String> map = new LinkedHashMap<>();
            for (int c = 0; c < header.size(); c++) {
                map.put(header.get(c).strip(), row.get(c).strip());
            }
            result.add(map);
        }
        return result;
    }

    private static List<List<String>> rows(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                }
                else if (ch == '"') {
                    quoted = false;
                }
                else {
                    field.append(ch);
                }
            }
            else if (ch == '"') {
                quoted = true;
            }
            else if (ch == ',') {
                row.add(field.toString());
                field.setLength(0);
            }
            else if (ch == '\n') {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            }
            else {
                field.append(ch);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }

}
