package com.ordermgmt.railway.domain.order.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.AuditEntry;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;

/**
 * Service that reads Hibernate Envers revisions and builds audit entries for any {@code @Audited}
 * entity.
 */
@Service
@Transactional(readOnly = true)
public class AuditService {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

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

        for (Object[] triple : results) {
            Object entity = triple[0];
            DefaultRevisionEntity revEntity = (DefaultRevisionEntity) triple[1];
            RevisionType revType = (RevisionType) triple[2];

            int revNumber = revEntity.getId();
            String timestamp = TS_FMT.format(Instant.ofEpochMilli(revEntity.getTimestamp()));
            String user = resolveUser(entity);
            String type = mapRevisionType(revType);
            String changes = describeChanges(revType, previousSnapshot, entity);

            entries.add(new AuditEntry(revNumber, timestamp, user, type, changes));
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
            var method = entity.getClass().getMethod("getUpdatedBy");
            String user = (String) method.invoke(entity);
            if (user != null && !user.isBlank()) {
                return user;
            }
            var createdMethod = entity.getClass().getMethod("getCreatedBy");
            String creator = (String) createdMethod.invoke(entity);
            if (creator != null && !creator.isBlank()) {
                return creator;
            }
        } catch (NoSuchMethodException e) {
            // Entity does not have getUpdatedBy/getCreatedBy
        } catch (Exception e) {
            // Reflection failure — fall through
        }
        return "system";
    }

    /** Generates a concise description of what changed between two snapshots. */
    private String describeChanges(RevisionType type, Object previous, Object current) {
        if (type == RevisionType.ADD) {
            return "Erstellt";
        }
        if (type == RevisionType.DEL) {
            return "Geloescht";
        }

        if (previous == null) {
            return "Geaendert";
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

                Object oldVal = method.invoke(previous);
                Object newVal = method.invoke(current);

                if (!objectsEqual(oldVal, newVal)) {
                    String fieldName =
                            method.getName().substring(3, 4).toLowerCase()
                                    + method.getName().substring(4);
                    changedFields.add(fieldName);
                }
            }
        } catch (Exception e) {
            return "Geaendert";
        }

        if (changedFields.isEmpty()) {
            return "Geaendert";
        }

        return String.join(", ", changedFields);
    }

    private boolean shouldSkipField(String methodName) {
        return "getClass".equals(methodName)
                || "getVersion".equals(methodName)
                || "getUpdatedAt".equals(methodName)
                || "getUpdatedBy".equals(methodName)
                || "getCreatedAt".equals(methodName)
                || "getCreatedBy".equals(methodName);
    }

    private boolean objectsEqual(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
