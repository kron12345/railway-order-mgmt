package com.ordermgmt.railway.domain.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.FristRegel;

/** Repository for configurable deadline rules (Frist-Regeln). */
@Repository
public interface FristRegelRepository extends JpaRepository<FristRegel, UUID> {

    List<FristRegel> findByEnabledTrue();
}
