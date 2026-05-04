package com.ordermgmt.railway.domain.business.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.model.BusinessDocument;
import com.ordermgmt.railway.domain.business.model.BusinessStatus;
import com.ordermgmt.railway.domain.business.repository.BusinessRepository;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.PurchasePositionRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Service for CRUD operations on {@link Business} entities including status transitions
 * and many-to-many linking to order positions.
 */
@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final OrderPositionRepository orderPositionRepository;
    private final PurchasePositionRepository purchasePositionRepository;

    public BusinessService(BusinessRepository businessRepository,
                           OrderPositionRepository orderPositionRepository,
                           PurchasePositionRepository purchasePositionRepository) {
        this.businessRepository = businessRepository;
        this.orderPositionRepository = orderPositionRepository;
        this.purchasePositionRepository = purchasePositionRepository;
    }

    /**
     * Creates a new business in the given status.
     */
    @Transactional
    public Business create(String title, String description) {
        return create(title, description, List.of(), List.of());
    }

    /**
     * Creates a new business and links it to the given order/purchase positions in one transaction.
     */
    @Transactional
    public Business create(String title, String description,
                           List<UUID> orderPositionIds, List<UUID> purchasePositionIds) {
        Business business = new Business();
        business.setTitle(title);
        business.setDescription(description);
        business.setStatus(BusinessStatus.IN_BEARBEITUNG);
        if (orderPositionIds != null && !orderPositionIds.isEmpty()) {
            business.getOrderPositions().addAll(orderPositionRepository.findAllById(orderPositionIds));
        }
        if (purchasePositionIds != null && !purchasePositionIds.isEmpty()) {
            business.getPurchasePositions().addAll(purchasePositionRepository.findAllById(purchasePositionIds));
        }
        return businessRepository.save(business);
    }

    /**
     * Updates basic fields of an existing business.
     */
    @Transactional
    public Business update(UUID id, String title, String description, String assignmentType,
                           String assignmentName, String team, LocalDate validFrom,
                           LocalDate validTo, LocalDate dueDate, String tags) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + id));
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
     * Set the assignee (user or team) for a business. Pass {@code (null, null)} to clear.
     * No-op when the values are already what the caller is asking for, so repeated calls
     * from re-renders do not churn through the audit log.
     */
    @Transactional
    public Business setAssignee(UUID id, com.ordermgmt.railway.domain.business.model.AssignmentType type, String name) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + id));
        String newType = type == null ? null : type.name();
        String currentType = business.getAssignmentType();
        String currentName = business.getAssignmentName();
        if (java.util.Objects.equals(currentType, newType)
                && java.util.Objects.equals(currentName, name)) {
            return business;
        }
        business.setAssignmentType(newType);
        business.setAssignmentName(name);
        return businessRepository.save(business);
    }

    /**
     * Transition to a new status if allowed by the transition rules.
     * @return the updated business
     */
    @Transactional
    public Business setStatus(UUID id, BusinessStatus newStatus) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + id));
        if (!business.getStatus().canTransitionTo(newStatus)) {
            throw new IllegalArgumentException("Transition not allowed: "
                    + business.getStatus() + " -> " + newStatus);
        }
        business.setStatus(newStatus);
        return businessRepository.save(business);
    }

    /**
     * Get valid target statuses for the given business.
     */
    public Set<BusinessStatus> getValidTransitions(UUID id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + id));
        return business.getStatus().nextTargets();
    }

    /**
     * Link the business to an order position (many-to-many).
     */
    @Transactional
    public void linkOrderPosition(UUID businessId, UUID orderPositionId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + businessId));
        OrderPosition position = orderPositionRepository.findById(orderPositionId)
                .orElseThrow(() -> new EntityNotFoundException("OrderPosition not found: " + orderPositionId));
        business.getOrderPositions().add(position);
        businessRepository.save(business);
    }

    /**
     * Unlink an order position from the business.
     */
    @Transactional
    public void unlinkOrderPosition(UUID businessId, UUID orderPositionId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + businessId));
        business.getOrderPositions().removeIf(p -> p.getId().equals(orderPositionId));
        businessRepository.save(business);
    }

    /**
     * Get all linked order positions for a business.
     */
    @Transactional(readOnly = true)
    public List<OrderPosition> getLinkedOrderPositions(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + businessId));
        // Force initialization of lazy associations referenced by the UI.
        for (OrderPosition op : business.getOrderPositions()) {
            Hibernate.initialize(op.getOrder());
        }
        // Detach from the persistent collection so the UI can iterate / size() outside the tx.
        return new java.util.ArrayList<>(business.getOrderPositions());
    }

    /**
     * Get all order positions (for linking). Eagerly fetches {@code order} for UI rendering.
     */
    @Transactional(readOnly = true)
    public List<OrderPosition> getAllOrderPositions() {
        return orderPositionRepository.findAllWithOrder();
    }

    @Transactional
    public void linkPurchasePosition(UUID businessId, UUID purchasePositionId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + businessId));
        PurchasePosition position = purchasePositionRepository.findById(purchasePositionId)
                .orElseThrow(() -> new EntityNotFoundException("PurchasePosition not found: " + purchasePositionId));
        if (business.getPurchasePositions().stream().noneMatch(p -> p.getId().equals(purchasePositionId))) {
            business.getPurchasePositions().add(position);
            businessRepository.save(business);
        }
    }

    @Transactional
    public void unlinkPurchasePosition(UUID businessId, UUID purchasePositionId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + businessId));
        business.getPurchasePositions().removeIf(p -> p.getId().equals(purchasePositionId));
        businessRepository.save(business);
    }

    @Transactional(readOnly = true)
    public List<PurchasePosition> getLinkedPurchasePositions(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + businessId));
        for (PurchasePosition pp : business.getPurchasePositions()) {
            OrderPosition op = pp.getOrderPosition();
            if (op != null) {
                Hibernate.initialize(op);
                Hibernate.initialize(op.getOrder());
            }
        }
        return new java.util.ArrayList<>(business.getPurchasePositions());
    }

    @Transactional(readOnly = true)
    public List<PurchasePosition> getAllPurchasePositions() {
        return purchasePositionRepository.findAllWithOrderPosition();
    }

    /**
     * Add a document to the business.
     */
    @Transactional
    public BusinessDocument addDocument(UUID businessId, String filename, String contentType, byte[] data) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + businessId));
        BusinessDocument doc = new BusinessDocument();
        doc.setBusiness(business);
        doc.setFilename(filename);
        doc.setContentType(contentType);
        doc.setData(data);
        business.getDocuments().add(doc);
        businessRepository.save(business);
        return doc;
    }

    /**
     * Remove a document from the business (cascades to BusinessDocument).
     */
    @Transactional
    public void removeDocument(UUID businessId, UUID documentId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + businessId));
        business.getDocuments().removeIf(d -> d.getId().equals(documentId));
        businessRepository.save(business);
    }

    /**
     * Get all documents for a business.
     */
    @Transactional(readOnly = true)
    public List<BusinessDocument> getDocuments(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException("Business not found: " + businessId));
        return new java.util.ArrayList<>(business.getDocuments());
    }

    /**
     * Get business by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Business> getById(UUID id) {
        return businessRepository.findById(id);
    }

    /**
     * Delete a business.
     */
    @Transactional
    public void delete(UUID id) {
        businessRepository.deleteById(id);
    }

    // --- Queries for list views ---

    @Transactional(readOnly = true)
    public List<Business> listAll() {
        return businessRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Business> findByLinkedOrderPosition(UUID orderPositionId) {
        return businessRepository.findByLinkedOrderPositionId(orderPositionId);
    }

    @Transactional(readOnly = true)
    public List<Business> findByLinkedPurchasePosition(UUID purchasePositionId) {
        return businessRepository.findByLinkedPurchasePositionId(purchasePositionId);
    }

    @Transactional(readOnly = true)
    public List<Business> findByLinkedOrder(UUID orderId) {
        return businessRepository.findByLinkedOrderId(orderId);
    }

    /** {@code Map<businessId, [orderPositionCount, purchasePositionCount]>}. */
    @Transactional(readOnly = true)
    public java.util.Map<UUID, int[]> linkCounts() {
        java.util.Map<UUID, int[]> out = new java.util.HashMap<>();
        for (Object[] row : businessRepository.findAllLinkCounts()) {
            UUID id = (UUID) row[0];
            int ops = ((Number) row[1]).intValue();
            int pps = ((Number) row[2]).intValue();
            out.put(id, new int[] {ops, pps});
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Business> listSortedByValidTo() {
        return businessRepository.findAll().stream()
                .sorted((a, b) -> {
                    // First compare by priority: IN_BEARBEITUNG > FREIGEGEBEN > UEBERARBEITEN
                    int priorityDiff = Integer.compare(
                            priorityOf(a.getStatus()),
                            priorityOf(b.getStatus()));
                    if (priorityDiff != 0) return priorityDiff;
                    // Then by validTo (null at the end)
                    if (a.getValidTo() == null && b.getValidTo() == null) return 0;
                    if (a.getValidTo() == null) return 1;
                    if (b.getValidTo() == null) return -1;
                    return a.getValidTo().compareTo(b.getValidTo());
                })
                .toList();
    }

    private int priorityOf(BusinessStatus status) {
        return switch (status) {
            case IN_BEARBEITUNG -> 1;
            case FREIGEGEBEN -> 2;
            case UEBERARBEITEN -> 3;
            case ABGESCHLOSSEN -> 4;
            case ANNULLIERT -> 5;
        };
    }
}
