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
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 3) continue;

                ResourceCatalogItem item = new ResourceCatalogItem();
                item.setCode(parts[0].trim());
                item.setName(parts[1].trim());
                item.setCategory(parts[2].trim());
                item.setActive(parts.length > 3 && "true".equalsIgnoreCase(parts[3].trim()));
                item.setSortOrder(
                        parts.length > 4 && !parts[4].trim().isEmpty()
                                ? Integer.parseInt(parts[4].trim())
                                : 0);
                catalogRepo.save(item);
                count++;
            }
            return count;
        } catch (DataAccessException e) {
            log.error("Catalog CSV import failed — rolling back transaction", e);
            throw new IOException("Import failed: " + e.getMessage(), e);
        }
    }
}
