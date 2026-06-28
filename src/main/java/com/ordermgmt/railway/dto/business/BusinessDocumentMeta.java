package com.ordermgmt.railway.dto.business;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata projection for a business document — everything the UI lists, WITHOUT the {@code byte[]}
 * blob. The blob is fetched on demand (only for the actual download) so listing a business's
 * documents never drags the file contents into memory.
 */
public record BusinessDocumentMeta(
        UUID id, String filename, String contentType, Instant createdAt) {}
