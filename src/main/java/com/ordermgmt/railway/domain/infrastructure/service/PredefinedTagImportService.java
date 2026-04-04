package com.ordermgmt.railway.domain.infrastructure.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;

import lombok.RequiredArgsConstructor;

/** Imports predefined tags from CSV files into the tag catalog. */
@Service
@RequiredArgsConstructor
public class PredefinedTagImportService {

    private static final CSVFormat CSV_FORMAT =
            CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build();

    private final PredefinedTagRepository tagRepository;

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public int importCsv(InputStream csvStream) throws IOException {
        int imported = 0;

        try (CSVParser parser =
                CSV_FORMAT.parse(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            for (CSVRecord record : parser) {
                if (isBlankRecord(record)) {
                    continue;
                }

                TagRecord tagRecord = toRecord(record);
                PredefinedTag tag =
                        tagRepository
                                .findByNameAndCategory(tagRecord.name(), tagRecord.category())
                                .orElseGet(PredefinedTag::new);
                tag.setName(tagRecord.name());
                tag.setCategory(tagRecord.category());
                tag.setColor(tagRecord.color());
                tag.setSortOrder(tagRecord.sortOrder());
                tag.setActive(tagRecord.active());
                tagRepository.save(tag);
                imported++;
            }
        }

        return imported;
    }

    private TagRecord toRecord(CSVRecord record) {
        String name = valueAt(record, 0);
        if (name.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }

        String category = normalizeCategory(valueAt(record, 1));
        String color = blankToNull(valueAt(record, 2));
        int sortOrder = parseInteger(valueAt(record, 3));
        boolean active = parseBoolean(valueAt(record, 4));

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

    private boolean isBlankRecord(CSVRecord record) {
        for (int i = 0; i < record.size(); i++) {
            if (!record.get(i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String valueAt(CSVRecord record, int index) {
        return record.size() > index ? record.get(index).trim() : "";
    }

    private String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }

    private record TagRecord(
            String name, String category, String color, int sortOrder, boolean active) {}
}
