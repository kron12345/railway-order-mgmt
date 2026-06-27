package com.ordermgmt.railway.domain.order.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.ResourceCatalogItem;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceOrigin;
import com.ordermgmt.railway.domain.order.model.ResourcePriority;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.ResourceCatalogItemRepository;
import com.ordermgmt.railway.domain.order.repository.ResourceNeedRepository;

import lombok.RequiredArgsConstructor;

/** Manages resource needs for order positions. */
@Service
@RequiredArgsConstructor
@Transactional
public class ResourceNeedService {

    private final ResourceNeedRepository resourceNeedRepository;
    private final OrderPositionRepository orderPositionRepository;
    private final ResourceCatalogItemRepository catalogItemRepository;

    /**
     * Creates default resource needs for a FAHRPLAN position. Generates CAPACITY (EXTERNAL),
     * VEHICLE (INTERNAL), and PERSONNEL (INTERNAL) needs with AUTO origin.
     *
     * @param position the order position to create defaults for
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void createDefaultResources(OrderPosition position) {
        if (position.getType() != PositionType.FAHRPLAN) {
            return;
        }

        LocalDate validFrom = resolveValidFrom(position);
        LocalDate validTo = resolveValidTo(position);
        createDefaultNeedIfMissing(
                position, ResourceType.CAPACITY, CoverageType.EXTERNAL, validFrom, validTo);
        createDefaultNeedIfMissing(
                position, ResourceType.VEHICLE, CoverageType.INTERNAL, validFrom, validTo);
        createDefaultNeedIfMissing(
                position, ResourceType.PERSONNEL, CoverageType.INTERNAL, validFrom, validTo);
    }

    /**
     * Adds a new resource need to a position.
     *
     * @return the persisted resource need
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResourceNeed addResource(
            UUID positionId,
            ResourceType type,
            UUID catalogItemId,
            int quantity,
            CoverageType coverage,
            String description) {
        return addResource(
                positionId,
                type,
                catalogItemId,
                quantity,
                coverage,
                description,
                ResourceOrigin.MANUAL);
    }

    /** As {@link #addResource}, but with an explicit origin (e.g. {@link ResourceOrigin#R2P}). */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResourceNeed addResource(
            UUID positionId,
            ResourceType type,
            UUID catalogItemId,
            int quantity,
            CoverageType coverage,
            String description,
            ResourceOrigin origin) {
        if (type == null) {
            throw new IllegalArgumentException("Resource type must not be null");
        }
        if (coverage == null) {
            throw new IllegalArgumentException("Coverage type must not be null");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        OrderPosition position = getOrderPosition(positionId);

        ResourceNeed need = new ResourceNeed();
        need.setOrderPosition(position);
        need.setResourceType(type);
        need.setCoverageType(coverage);
        need.setQuantity(quantity);
        need.setDescription(description);
        need.setOrigin(origin);
        need.setPriority(ResourcePriority.MEDIUM);
        need.setValidFrom(resolveValidFrom(position));
        need.setValidTo(resolveValidTo(position));

        if (catalogItemId != null) {
            need.setCatalogItem(getCatalogItem(catalogItemId));
        }

        ResourceNeed saved = resourceNeedRepository.save(need);
        // Keep the parent's managed collection in sync — OrderPosition.resourceNeeds uses
        // orphanRemoval=true, so a later save of a stale OrderPosition would otherwise delete this
        // need (and cascade-delete its purchases).
        if (position.getResourceNeeds() != null && !position.getResourceNeeds().contains(saved)) {
            position.getResourceNeeds().add(saved);
        }
        return saved;
    }

    /** Removes a resource need by its ID. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void removeResource(UUID resourceNeedId) {
        resourceNeedRepository.deleteById(resourceNeedId);
    }

    /** Sets a demand's own Verkehrstage (validity date-set) and von/nach route (FAHRPLAN). */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void updateVerkehrstageAndRoute(
            UUID needId, String validity, String fromLocation, String toLocation) {
        ResourceNeed need = resourceNeedRepository.findById(needId).orElseThrow();
        need.setValidity(validity);
        need.setFromLocation(fromLocation);
        need.setToLocation(toLocation);
        resourceNeedRepository.save(need);
    }

    /** Returns all resource needs for a given order position with catalog items eagerly loaded. */
    @Transactional(readOnly = true)
    public List<ResourceNeed> getResourcesForPosition(UUID positionId) {
        return resourceNeedRepository.findByOrderPositionIdWithCatalogItem(positionId);
    }

    private ResourceNeed buildNeed(
            OrderPosition position, ResourceType type, CoverageType coverage) {
        ResourceNeed need = new ResourceNeed();
        need.setOrderPosition(position);
        need.setResourceType(type);
        need.setCoverageType(coverage);
        need.setOrigin(ResourceOrigin.AUTO);
        need.setPriority(ResourcePriority.MEDIUM);
        need.setQuantity(1);
        return need;
    }

    private void createDefaultNeedIfMissing(
            OrderPosition position,
            ResourceType resourceType,
            CoverageType coverageType,
            LocalDate validFrom,
            LocalDate validTo) {
        if (!noNeedExists(position, resourceType)) {
            return;
        }
        ResourceNeed need = buildNeed(position, resourceType, coverageType);
        need.setValidFrom(validFrom);
        need.setValidTo(validTo);
        if (resourceType == ResourceType.CAPACITY) {
            linkFahrplanId(position, need);
        }
        resourceNeedRepository.save(need);
    }

    private boolean noNeedExists(OrderPosition position, ResourceType type) {
        return position.getResourceNeeds().stream()
                .noneMatch(need -> need.getResourceType() == type);
    }

    private void linkFahrplanId(OrderPosition position, ResourceNeed capacity) {
        position.getResourceNeeds().stream()
                .filter(need -> need.getResourceType() == ResourceType.CAPACITY)
                .findFirst()
                .ifPresent(
                        existing -> capacity.setLinkedFahrplanId(existing.getLinkedFahrplanId()));
    }

    private OrderPosition getOrderPosition(UUID positionId) {
        return orderPositionRepository
                .findById(positionId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Order position not found: " + positionId));
    }

    private ResourceCatalogItem getCatalogItem(UUID catalogItemId) {
        return catalogItemRepository
                .findById(catalogItemId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Catalog item not found: " + catalogItemId));
    }

    private LocalDate resolveValidFrom(OrderPosition position) {
        if (position.getStart() != null) {
            return position.getStart().toLocalDate();
        }
        return null;
    }

    private LocalDate resolveValidTo(OrderPosition position) {
        if (position.getEnd() != null) {
            return position.getEnd().toLocalDate();
        }
        return null;
    }
}
