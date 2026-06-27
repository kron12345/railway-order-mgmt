package com.ordermgmt.railway.domain.order.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.model.BusinessDocument;
import com.ordermgmt.railway.domain.order.model.AuditEntry;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;

/**
 * Service that reads Hibernate Envers revisions and builds audit entries for any {@code @Audited}
 * entity.
 */
@Service
@Transactional(readOnly = true)
public class AuditService {

    private static final String SYSTEM_USER = "system";
    private static final String CHANGE_CREATED = "Erstellt";
    private static final String CHANGE_DELETED = "Geloescht";
    private static final String CHANGE_UPDATED = "Geaendert";
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
    // Audited collection snapshots can lazy-load or compare unreliably; the revision still exists.
    private static final Set<String> SKIPPED_GETTERS =
            Set.of(
                    "getClass",
                    "getVersion",
                    "getUpdatedAt",
                    "getUpdatedBy",
                    "getCreatedAt",
                    "getCreatedBy",
                    "getOrderPositions",
                    "getPurchasePositions",
                    "getDocuments");

    private final EntityManager entityManager;

    public AuditService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Returns audit history for an Order. */
    public List<AuditEntry> getOrderHistory(UUID orderId) {
        return getEntityHistory(Order.class, orderId);
    }

    /** Returns audit history for an OrderPosition. */
    public List<AuditEntry> getPositionHistory(UUID positionId) {
        return getEntityHistory(OrderPosition.class, positionId);
    }

    /** Returns audit history for a ResourceNeed. */
    public List<AuditEntry> getResourceNeedHistory(UUID resourceNeedId) {
        return getEntityHistory(ResourceNeed.class, resourceNeedId);
    }

    /** Returns audit history for a PurchasePosition. */
    public List<AuditEntry> getPurchasePositionHistory(UUID purchasePositionId) {
        return getEntityHistory(PurchasePosition.class, purchasePositionId);
    }

    /** Returns audit history for a Business (status, assignee, fields, links). */
    public List<AuditEntry> getBusinessHistory(UUID businessId) {
        return getEntityHistory(Business.class, businessId);
    }

    /** Returns audit history for a single BusinessDocument. */
    public List<AuditEntry> getBusinessDocumentHistory(UUID documentId) {
        return getEntityHistory(BusinessDocument.class, documentId);
    }

    /** Generic method to retrieve audit history for any {@code @Audited} entity. */
    @SuppressWarnings("unchecked")
    public <T> List<AuditEntry> getEntityHistory(Class<T> entityClass, UUID id) {
        AuditReader reader = AuditReaderFactory.get(entityManager);

        List<Object[]> results =
                reader.createQuery()
                        .forRevisionsOfEntity(entityClass, false, true)
                        .add(AuditEntity.id().eq(id))
                        .addOrder(AuditEntity.revisionNumber().asc())
                        .getResultList();

        List<AuditEntry> entries = new ArrayList<>();
        Object previousSnapshot = null;

        for (Object[] revisionRow : results) {
            Object entity = revisionRow[0];
            DefaultRevisionEntity revisionEntity = (DefaultRevisionEntity) revisionRow[1];
            RevisionType revisionType = (RevisionType) revisionRow[2];

            int revisionNumber = revisionEntity.getId();
            String timestamp = TS_FMT.format(Instant.ofEpochMilli(revisionEntity.getTimestamp()));
            String user = resolveUser(entity);
            String type = mapRevisionType(revisionType);
            String changes = describeChanges(revisionType, previousSnapshot, entity);

            entries.add(new AuditEntry(revisionNumber, timestamp, user, type, changes));
            previousSnapshot = entity;
        }

        return entries;
    }

    private String mapRevisionType(RevisionType type) {
        return switch (type) {
            case ADD -> "ADD";
            case MOD -> "MOD";
            case DEL -> "DEL";
        };
    }

    /**
     * Attempts to extract the updatedBy/createdBy from the entity snapshot using reflection.
     * Returns "system" if no user information is available.
     */
    private String resolveUser(Object entity) {
        try {
            var updatedBy = entity.getClass().getMethod("getUpdatedBy");
            String user = (String) updatedBy.invoke(entity);
            if (hasText(user)) {
                return user;
            }
            var createdBy = entity.getClass().getMethod("getCreatedBy");
            String creator = (String) createdBy.invoke(entity);
            if (hasText(creator)) {
                return creator;
            }
        } catch (NoSuchMethodException e) {
            // Entity does not have getUpdatedBy/getCreatedBy
        } catch (Exception e) {
            // Reflection failure — fall through
        }
        return SYSTEM_USER;
    }

    /** Generates a concise description of what changed between two snapshots. */
    private String describeChanges(RevisionType type, Object previous, Object current) {
        if (type == RevisionType.ADD) {
            return CHANGE_CREATED;
        }
        if (type == RevisionType.DEL) {
            return CHANGE_DELETED;
        }

        if (previous == null) {
            return CHANGE_UPDATED;
        }

        return compareSnapshots(previous, current);
    }

    /** Compares two entity snapshots field-by-field and returns a summary of changed fields. */
    private String compareSnapshots(Object previous, Object current) {
        List<String> changedFields = new ArrayList<>();
        try {
            for (var method : current.getClass().getMethods()) {
                if (!method.getName().startsWith("get") || method.getParameterCount() > 0) {
                    continue;
                }
                if (shouldSkipField(method.getName())) {
                    continue;
                }

                Object previousValue = method.invoke(previous);
                Object currentValue = method.invoke(current);

                if (!objectsEqual(previousValue, currentValue)) {
                    changedFields.add(fieldName(method.getName()));
                }
            }
        } catch (Exception e) {
            return CHANGE_UPDATED;
        }

        if (changedFields.isEmpty()) {
            return CHANGE_UPDATED;
        }

        return String.join(", ", changedFields);
    }

    private boolean shouldSkipField(String methodName) {
        return SKIPPED_GETTERS.contains(methodName);
    }

    private String fieldName(String getterName) {
        return getterName.substring(3, 4).toLowerCase() + getterName.substring(4);
    }

    private boolean objectsEqual(Object previousValue, Object currentValue) {
        if (previousValue == currentValue) {
            return true;
        }
        if (previousValue == null || currentValue == null) {
            return false;
        }
        return previousValue.equals(currentValue);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
