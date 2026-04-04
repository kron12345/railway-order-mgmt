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

        // CAPACITY — external (infrastructure ordering via TTT)
        if (noNeedExists(position, ResourceType.CAPACITY)) {
            ResourceNeed capacity =
                    buildNeed(position, ResourceType.CAPACITY, CoverageType.EXTERNAL);
            capacity.setValidFrom(validFrom);
            capacity.setValidTo(validTo);
            linkFahrplanId(position, capacity);
            resourceNeedRepository.save(capacity);
        }

        // VEHICLE — internal (rolling stock)
        if (noNeedExists(position, ResourceType.VEHICLE)) {
            ResourceNeed vehicle = buildNeed(position, ResourceType.VEHICLE, CoverageType.INTERNAL);
            vehicle.setValidFrom(validFrom);
            vehicle.setValidTo(validTo);
            resourceNeedRepository.save(vehicle);
        }

        // PERSONNEL — internal (crew planning)
        if (noNeedExists(position, ResourceType.PERSONNEL)) {
            ResourceNeed personnel =
                    buildNeed(position, ResourceType.PERSONNEL, CoverageType.INTERNAL);
            personnel.setValidFrom(validFrom);
            personnel.setValidTo(validTo);
            resourceNeedRepository.save(personnel);
        }
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
        OrderPosition position =
                orderPositionRepository
                        .findById(positionId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Order position not found: " + positionId));

        ResourceNeed need = new ResourceNeed();
        need.setOrderPosition(position);
        need.setResourceType(type);
        need.setCoverageType(coverage);
        need.setQuantity(quantity);
        need.setDescription(description);
        need.setOrigin(ResourceOrigin.MANUAL);
        need.setPriority(ResourcePriority.MEDIUM);
        need.setValidFrom(resolveValidFrom(position));
        need.setValidTo(resolveValidTo(position));

        if (catalogItemId != null) {
            ResourceCatalogItem item =
                    catalogItemRepository
                            .findById(catalogItemId)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Catalog item not found: " + catalogItemId));
            need.setCatalogItem(item);
        }

        return resourceNeedRepository.save(need);
    }

    /** Removes a resource need by its ID. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void removeResource(UUID resourceNeedId) {
        resourceNeedRepository.deleteById(resourceNeedId);
    }

    /** Returns all resource needs for a given order position. */
    @Transactional(readOnly = true)
    public List<ResourceNeed> getResourcesForPosition(UUID positionId) {
        return resourceNeedRepository.findByOrderPositionId(positionId);
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

    private boolean noNeedExists(OrderPosition position, ResourceType type) {
        return position.getResourceNeeds().stream()
                .noneMatch(need -> need.getResourceType() == type);
    }

    private void linkFahrplanId(OrderPosition position, ResourceNeed capacity) {
        position.getResourceNeeds().stream()
                .filter(rn -> rn.getResourceType() == ResourceType.CAPACITY)
                .findFirst()
                .ifPresent(
                        existing -> capacity.setLinkedFahrplanId(existing.getLinkedFahrplanId()));
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
