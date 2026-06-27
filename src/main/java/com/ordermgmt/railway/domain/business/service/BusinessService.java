package com.ordermgmt.railway.domain.business.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import org.hibernate.Hibernate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.business.model.AssignmentType;
import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.model.BusinessDocument;
import com.ordermgmt.railway.domain.business.model.BusinessStatus;
import com.ordermgmt.railway.domain.business.repository.BusinessRepository;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.PurchasePositionRepository;
import com.ordermgmt.railway.dto.business.BusinessListItem;
import com.ordermgmt.railway.dto.business.BusinessListQuery;

/**
 * Service for CRUD operations on {@link Business} entities including status transitions and
 * many-to-many linking to order positions.
 */
@Service
public class BusinessService {

    /**
     * Roles allowed to mutate business records. Read access remains open to all authenticated users
     * (Vaadin route is {@code @PermitAll}).
     */
    private static final String MUTATION_ROLES = "hasAnyRole('ADMIN', 'DISPATCHER')";
    private static final String SAFE_FALLBACK_CONTENT_TYPE = "application/octet-stream";
    private static final Set<String> ALLOWED_DOCUMENT_MIME_TYPES =
            Set.of(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/zip",
                    "application/x-zip-compressed",
                    "image/png",
                    "image/jpeg",
                    "image/gif",
                    "image/webp",
                    "text/plain",
                    "text/csv");

    private final BusinessRepository businessRepository;
    private final OrderPositionRepository orderPositionRepository;
    private final PurchasePositionRepository purchasePositionRepository;

    public BusinessService(
            BusinessRepository businessRepository,
            OrderPositionRepository orderPositionRepository,
            PurchasePositionRepository purchasePositionRepository) {
        this.businessRepository = businessRepository;
        this.orderPositionRepository = orderPositionRepository;
        this.purchasePositionRepository = purchasePositionRepository;
    }

    /** Load a business by id or throw — the single find-or-throw used by all mutators/readers. */
    private Business requireBusiness(UUID id) {
        return businessRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + id));
    }

    private OrderPosition requireOrderPosition(UUID orderPositionId) {
        return orderPositionRepository
                .findById(orderPositionId)
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "OrderPosition not found: " + orderPositionId));
    }

    private PurchasePosition requirePurchasePosition(UUID purchasePositionId) {
        return purchasePositionRepository
                .findById(purchasePositionId)
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "PurchasePosition not found: " + purchasePositionId));
    }

    /** Creates a new business in the given status. */
    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public Business create(String title, String description) {
        return create(title, description, List.of(), List.of());
    }

    /**
     * Creates a new business and links it to the given order/purchase positions in one transaction.
     */
    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public Business create(
            String title,
            String description,
            List<UUID> orderPositionIds,
            List<UUID> purchasePositionIds) {
        Business business = new Business();
        business.setTitle(title);
        business.setDescription(description);
        business.setStatus(BusinessStatus.IN_BEARBEITUNG);
        if (orderPositionIds != null && !orderPositionIds.isEmpty()) {
            business.getOrderPositions()
                    .addAll(orderPositionRepository.findAllById(orderPositionIds));
        }
        if (purchasePositionIds != null && !purchasePositionIds.isEmpty()) {
            business.getPurchasePositions()
                    .addAll(purchasePositionRepository.findAllById(purchasePositionIds));
        }
        return businessRepository.save(business);
    }

    /** Updates basic fields of an existing business. */
    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public Business update(
            UUID id,
            String title,
            String description,
            String assignmentType,
            String assignmentName,
            String team,
            LocalDate validFrom,
            LocalDate validTo,
            LocalDate dueDate,
            String tags) {
        Business business = requireBusiness(id);
        business.setTitle(title);
        business.setDescription(description);
        business.setAssignmentType(assignmentType);
        business.setAssignmentName(assignmentName);
        business.setTeam(team);
        business.setValidFrom(validFrom);
        business.setValidTo(validTo);
        business.setDueDate(dueDate);
        business.setTags(tags);
        return businessRepository.save(business);
    }

    /**
     * Set the assignee (user or team) for a business. Pass {@code (null, null)} to clear. No-op
     * when the values are already what the caller is asking for, so repeated calls from re-renders
     * do not churn through the audit log.
     */
    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public Business setAssignee(UUID id, AssignmentType type, String name) {
        Business business = requireBusiness(id);
        String newType = type == null ? null : type.name();
        String currentType = business.getAssignmentType();
        String currentName = business.getAssignmentName();
        if (Objects.equals(currentType, newType) && Objects.equals(currentName, name)) {
            return business;
        }
        business.setAssignmentType(newType);
        business.setAssignmentName(name);
        return businessRepository.save(business);
    }

    /**
     * Transition to a new status if allowed by the transition rules.
     *
     * @return the updated business
     */
    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public Business setStatus(UUID id, BusinessStatus newStatus) {
        Business business = requireBusiness(id);
        if (!business.getStatus().canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                    "Transition not allowed: " + business.getStatus() + " -> " + newStatus);
        }
        business.setStatus(newStatus);
        return businessRepository.save(business);
    }

    /** Link the business to an order position (many-to-many). */
    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public void linkOrderPosition(UUID businessId, UUID orderPositionId) {
        Business business = requireBusiness(businessId);
        OrderPosition position = requireOrderPosition(orderPositionId);
        business.getOrderPositions().add(position);
        businessRepository.save(business);
    }

    /** Unlink an order position from the business. */
    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public void unlinkOrderPosition(UUID businessId, UUID orderPositionId) {
        Business business = requireBusiness(businessId);
        business.getOrderPositions().removeIf(p -> p.getId().equals(orderPositionId));
        businessRepository.save(business);
    }

    /**
     * Reconciles ALL business links of one order position to exactly {@code businessIds} in a
     * single transaction (links the added, unlinks the removed) — replaces the per-link/unlink loop
     * the UI used to issue. Managed entities flush on commit.
     */
    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public void setOrderPositionLinks(UUID orderPositionId, Set<UUID> businessIds) {
        OrderPosition position = requireOrderPosition(orderPositionId);
        Set<UUID> targetBusinessIds = businessIds == null ? Set.of() : businessIds;
        Set<UUID> currentlyLinkedBusinessIds = new HashSet<>();

        for (Business business : businessRepository.findByLinkedOrderPositionId(orderPositionId)) {
            currentlyLinkedBusinessIds.add(business.getId());
            if (!targetBusinessIds.contains(business.getId())) {
                business.getOrderPositions().removeIf(p -> p.getId().equals(orderPositionId));
            }
        }

        for (Business business : businessRepository.findAllById(targetBusinessIds)) {
            if (!currentlyLinkedBusinessIds.contains(business.getId())) {
                business.getOrderPositions().add(position);
            }
        }
    }

    /** Get all linked order positions for a business. */
    @Transactional(readOnly = true)
    public List<OrderPosition> getLinkedOrderPositions(UUID businessId) {
        Business business = requireBusiness(businessId);
        initializeOrders(business.getOrderPositions());
        return new ArrayList<>(business.getOrderPositions());
    }

    /** A business plus its three UI collections, loaded together for the read view. */
    public record BusinessReadModel(
            Business business,
            List<OrderPosition> orderPositions,
            List<PurchasePosition> purchasePositions,
            List<BusinessDocument> documents) {}

    /**
     * Loads a business and its linked order/purchase positions + documents in ONE transaction (one
     * findById), so the read view no longer re-fetches the same aggregate per card.
     */
    @Transactional(readOnly = true)
    public Optional<BusinessReadModel> loadReadModel(UUID businessId) {
        return businessRepository
                .findById(businessId)
                .map(
                        business -> {
                            initializeOrders(business.getOrderPositions());
                            initializePurchasePositions(business.getPurchasePositions());
                            Hibernate.initialize(business.getDocuments());
                            return new BusinessReadModel(
                                    business,
                                    new ArrayList<>(business.getOrderPositions()),
                                    new ArrayList<>(business.getPurchasePositions()),
                                    new ArrayList<>(business.getDocuments()));
                        });
    }

    /** Get all order positions (for linking). Eagerly fetches {@code order} for UI rendering. */
    @Transactional(readOnly = true)
    public List<OrderPosition> getAllOrderPositions() {
        return orderPositionRepository.findAllWithOrder();
    }

    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public void linkPurchasePosition(UUID businessId, UUID purchasePositionId) {
        Business business = requireBusiness(businessId);
        PurchasePosition position = requirePurchasePosition(purchasePositionId);
        if (business.getPurchasePositions().stream()
                .noneMatch(p -> p.getId().equals(purchasePositionId))) {
            business.getPurchasePositions().add(position);
            businessRepository.save(business);
        }
    }

    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public void unlinkPurchasePosition(UUID businessId, UUID purchasePositionId) {
        Business business = requireBusiness(businessId);
        business.getPurchasePositions().removeIf(p -> p.getId().equals(purchasePositionId));
        businessRepository.save(business);
    }

    @Transactional(readOnly = true)
    public List<PurchasePosition> getLinkedPurchasePositions(UUID businessId) {
        Business business = requireBusiness(businessId);
        initializePurchasePositions(business.getPurchasePositions());
        return new ArrayList<>(business.getPurchasePositions());
    }

    @Transactional(readOnly = true)
    public List<PurchasePosition> getAllPurchasePositions() {
        return purchasePositionRepository.findAllWithOrderPosition();
    }

    /**
     * Add a document to the business. Sanitises the client-supplied MIME type against {@link
     * #ALLOWED_DOCUMENT_MIME_TYPES}; anything else is stored as {@code application/octet-stream} so
     * it cannot trigger inline rendering.
     */
    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public BusinessDocument addDocument(
            UUID businessId, String filename, String contentType, byte[] data) {
        Business business = requireBusiness(businessId);
        BusinessDocument doc = new BusinessDocument();
        doc.setBusiness(business);
        doc.setFilename(filename);
        doc.setContentType(sanitiseContentType(contentType));
        doc.setData(data);
        business.getDocuments().add(doc);
        businessRepository.save(business);
        return doc;
    }

    private static String sanitiseContentType(String raw) {
        if (raw == null) {
            return SAFE_FALLBACK_CONTENT_TYPE;
        }
        String trimmed = raw.toLowerCase(Locale.ROOT).trim();
        // Strip parameters such as ; charset=utf-8 before matching the whitelist.
        int parameterStart = trimmed.indexOf(';');
        String baseContentType =
                parameterStart < 0 ? trimmed : trimmed.substring(0, parameterStart).trim();
        return ALLOWED_DOCUMENT_MIME_TYPES.contains(baseContentType)
                ? baseContentType
                : SAFE_FALLBACK_CONTENT_TYPE;
    }

    /** Remove a document from the business (cascades to BusinessDocument). */
    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public void removeDocument(UUID businessId, UUID documentId) {
        Business business = requireBusiness(businessId);
        business.getDocuments().removeIf(d -> d.getId().equals(documentId));
        businessRepository.save(business);
    }

    /** Get all documents for a business. */
    @Transactional(readOnly = true)
    public List<BusinessDocument> getDocuments(UUID businessId) {
        Business business = requireBusiness(businessId);
        return new ArrayList<>(business.getDocuments());
    }

    /** Get business by ID. */
    @Transactional(readOnly = true)
    public Optional<Business> getById(UUID id) {
        return businessRepository.findById(id);
    }

    /** Delete a business. */
    @PreAuthorize(MUTATION_ROLES)
    @Transactional
    public void delete(UUID id) {
        businessRepository.deleteById(id);
    }

    // --- Queries for list views ---

    @Transactional(readOnly = true)
    public List<Business> listAll() {
        return businessRepository.findAll();
    }

    /**
     * Lazy business list (P3): a {@code Slice} of {@link BusinessListItem} projections for the
     * given filter, with the sort made stable via an id tie-breaker. No total count (Slice fetches
     * pageSize+1) — the list shows "loaded / more", not a total.
     */
    @Transactional(readOnly = true)
    public Slice<BusinessListItem> searchBusinesses(BusinessListQuery q, Pageable pageable) {
        return businessRepository.searchBusinesses(
                blankToNull(q.text()),
                q.status(),
                q.validFromMin(),
                q.validToMax(),
                blankToNull(q.tags()),
                blankToNull(q.assignee()),
                stableSort(pageable, "title"));
    }

    /**
     * Appends an id tie-breaker (or a default field + id when unsorted) for stable paging. Rebuilds
     * the Pageable via {@code PageRequest.of(getPageNumber(), getPageSize())}; this preserves the
     * exact offset ONLY for page-aligned offsets (multiples of the page size). The lazy list
     * callers always append whole pages, so their offsets stay aligned — do not feed this a
     * non-aligned {@code OffsetPageable} or the reconstructed page will skip rows.
     */
    private static Pageable stableSort(Pageable pageable, String defaultField) {
        Sort sort =
                pageable.getSort().isSorted()
                        ? pageable.getSort().and(Sort.by(Sort.Direction.ASC, "id"))
                        : Sort.by(Sort.Direction.ASC, defaultField)
                                .and(Sort.by(Sort.Direction.ASC, "id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Transactional(readOnly = true)
    public List<Business> findByLinkedOrderPosition(UUID orderPositionId) {
        return businessRepository.findByLinkedOrderPositionId(orderPositionId);
    }

    /** Batched linked-business lookup for many positions at once (avoids one query per row). */
    @Transactional(readOnly = true)
    public Map<UUID, List<Business>> findByLinkedOrderPositions(Collection<UUID> orderPositionIds) {
        return groupByFirstId(
                businessRepository.findBusinessesByOrderPositionIds(orderPositionIds));
    }

    /** Batched linked-business lookup for many purchase positions at once. */
    @Transactional(readOnly = true)
    public Map<UUID, List<Business>> findByLinkedPurchasePositions(
            Collection<UUID> purchasePositionIds) {
        return groupByFirstId(
                businessRepository.findBusinessesByPurchasePositionIds(purchasePositionIds));
    }

    private Map<UUID, List<Business>> groupByFirstId(List<Object[]> rows) {
        Map<UUID, List<Business>> businessesByLinkedPositionId = new HashMap<>();
        for (Object[] row : rows) {
            businessesByLinkedPositionId
                    .computeIfAbsent((UUID) row[0], ignored -> new ArrayList<>())
                    .add((Business) row[1]);
        }
        return businessesByLinkedPositionId;
    }

    private void initializeOrders(Collection<OrderPosition> orderPositions) {
        for (OrderPosition orderPosition : orderPositions) {
            Hibernate.initialize(orderPosition.getOrder());
        }
    }

    private void initializePurchasePositions(Collection<PurchasePosition> purchasePositions) {
        for (PurchasePosition purchasePosition : purchasePositions) {
            OrderPosition orderPosition = purchasePosition.getOrderPosition();
            if (orderPosition != null) {
                Hibernate.initialize(orderPosition);
                Hibernate.initialize(orderPosition.getOrder());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Business> findByLinkedOrder(UUID orderId) {
        return businessRepository.findByLinkedOrderId(orderId);
    }
}
