package com.ordermgmt.railway.domain.order.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.ResourceCatalogItem;
import com.ordermgmt.railway.domain.order.repository.ResourceCatalogItemRepository;

import lombok.RequiredArgsConstructor;

/** Transactional CSV import for the resource catalog. */
@Service
@RequiredArgsConstructor
public class ResourceCatalogImportService {

    private static final Logger log = LoggerFactory.getLogger(ResourceCatalogImportService.class);
    private static final int HEADER_LINE_COUNT = 1;
    private static final int CODE_COLUMN = 0;
    private static final int NAME_COLUMN = 1;
    private static final int CATEGORY_COLUMN = 2;
    private static final int ACTIVE_COLUMN = 3;
    private static final int SORT_ORDER_COLUMN = 4;
    private static final int REQUIRED_COLUMN_COUNT = 3;

    private final ResourceCatalogItemRepository catalogRepo;

    /**
     * Imports catalog items from CSV bytes. The entire import runs in a single transaction so that
     * a failure at any row rolls back all changes.
     *
     * @param csvBytes raw CSV content
     * @return the number of imported items
     * @throws IOException if CSV parsing fails
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public int importCsv(byte[] csvBytes) throws IOException {
        try {
            String content = new String(csvBytes, StandardCharsets.UTF_8);
            String[] lines = content.split("\\r?\\n");
            int count = 0;
            for (int lineIndex = HEADER_LINE_COUNT; lineIndex < lines.length; lineIndex++) {
                ResourceCatalogItem item = parseCatalogItem(lines[lineIndex]);
                if (item == null) {
                    continue;
                }
                catalogRepo.save(item);
                count++;
            }
            return count;
        } catch (DataAccessException e) {
            log.error("Catalog CSV import failed — rolling back transaction", e);
            throw new IOException("Import failed: " + e.getMessage(), e);
        }
    }

    private ResourceCatalogItem parseCatalogItem(String csvLine) {
        String line = csvLine.trim();
        if (line.isEmpty()) {
            return null;
        }
        String[] columns = line.split(",", -1);
        if (columns.length < REQUIRED_COLUMN_COUNT) {
            return null;
        }

        ResourceCatalogItem item = new ResourceCatalogItem();
        item.setCode(columns[CODE_COLUMN].trim());
        item.setName(columns[NAME_COLUMN].trim());
        item.setCategory(columns[CATEGORY_COLUMN].trim());
        item.setActive(
                columns.length > ACTIVE_COLUMN
                        && "true".equalsIgnoreCase(columns[ACTIVE_COLUMN].trim()));
        item.setSortOrder(parseSortOrder(columns));
        return item;
    }

    private int parseSortOrder(String[] columns) {
        if (columns.length <= SORT_ORDER_COLUMN || columns[SORT_ORDER_COLUMN].trim().isEmpty()) {
            return 0;
        }
        return Integer.parseInt(columns[SORT_ORDER_COLUMN].trim());
    }
}
