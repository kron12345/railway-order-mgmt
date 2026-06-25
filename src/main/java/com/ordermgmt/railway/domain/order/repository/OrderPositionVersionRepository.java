package com.ordermgmt.railway.domain.order.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.OrderPositionVersion;

/** Repository for the version trail of an order-position expression. */
@Repository
public interface OrderPositionVersionRepository extends JpaRepository<OrderPositionVersion, UUID> {

    List<OrderPositionVersion> findByOrderPositionIdOrderByVersionNumberAsc(UUID orderPositionId);

    /** Batched load for a set of positions (avoids one query per row). */
    List<OrderPositionVersion> findByOrderPositionIdIn(Collection<UUID> orderPositionIds);
}
