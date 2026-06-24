package com.ordermgmt.railway.domain.order.model;

import java.util.UUID;

/**
 * A single requested resource within an inbound R2P order. Either references a catalog item ({@code
 * catalogItemId}) or is free text ({@code description}) — both are supported.
 */
public record R2pResourceRequest(
        ResourceType resourceType, String description, int quantity, UUID catalogItemId) {}
