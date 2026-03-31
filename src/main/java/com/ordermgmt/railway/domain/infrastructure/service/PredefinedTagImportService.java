package com.ordermgmt.railway.domain.infrastructure.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;

import lombok.RequiredArgsConstructor;

/** Imports predefined tags from CSV files into the tag catalog. */
@Service
@RequiredArgsConstructor
public class PredefinedTagImportService {

    private final PredefinedTagRepository tagRepository;

    @Transactional
    public int importCsv(InputStream csvStream) throws IOException {
        List<String[]> rows = parseCsv(csvStream);
        int imported = 0;

        for (String[] row : rows) {
            if (isBlankRow(row)) {
                continue;
            }

            TagRecord record = toRecord(row);
            PredefinedTag tag =
                    tagRepository
                            .findByNameAndCategory(record.name(), record.category())
                            .orElseGet(PredefinedTag::new);
            tag.setName(record.name());
            tag.setCategory(record.category());
            tag.setColor(record.color());
            tag.setSortOrder(record.sortOrder());
            tag.setActive(record.active());
            tagRepository.save(tag);
            imported++;
        }

        return imported;
    }

    private List<String[]> parseCsv(InputStream inputStream) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    rows.add(splitCsvLine(line));
                }
            }
        }
        return rows;
    }

    private TagRecord toRecord(String[] row) {
        String name = valueAt(row, 0);
        if (name.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }

        String category = normalizeCategory(valueAt(row, 1));
        String color = blankToNull(valueAt(row, 2));
        int sortOrder = parseInteger(valueAt(row, 3));
        boolean active = parseBoolean(valueAt(row, 4));

        return new TagRecord(name, category, color, sortOrder, active);
    }

    private String normalizeCategory(String value) {
        String normalized = value.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ORDER", "POSITION", "GENERAL" -> normalized;
            default -> throw new IllegalArgumentException("Unsupported tag category: " + value);
        };
    }

    private int parseInteger(String value) {
        if (value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid sort_order value: " + value, e);
        }
    }

    private boolean parseBoolean(String value) {
        if (value.isBlank()) {
            return true;
        }

        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "ja" -> true;
            case "false", "0", "no", "nein" -> false;
            default -> throw new IllegalArgumentException("Invalid active value: " + value);
        };
    }

    private boolean isBlankRow(String[] row) {
        for (String value : row) {
            if (!value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private String valueAt(String[] row, int index) {
        return row.length > index ? row[index].trim().replace("\"", "") : "";
    }

    private String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }

    private record TagRecord(
            String name, String category, String color, int sortOrder, boolean active) {}
}
