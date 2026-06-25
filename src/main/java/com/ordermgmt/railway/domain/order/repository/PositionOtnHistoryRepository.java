package com.ordermgmt.railway.domain.order.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.PositionOtnHistory;

/** Repository for the OTN history of a train-identity position. */
@Repository
public interface PositionOtnHistoryRepository extends JpaRepository<PositionOtnHistory, UUID> {

    List<PositionOtnHistory> findByOrderPositionIdOrderByValidFromAsc(UUID orderPositionId);

    /** Positions that ever carried this OTN — keeps a renamed train findable by its old number. */
    List<PositionOtnHistory> findByOtn(String otn);

    /** Batched load for a set of positions (avoids one query per row). */
    List<PositionOtnHistory> findByOrderPositionIdIn(Collection<UUID> orderPositionIds);
}
