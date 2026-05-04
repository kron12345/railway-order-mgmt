package com.ordermgmt.railway.domain.business.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.business.model.Business;

/** Repository for persisting and querying businesses. */
@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    @Query("SELECT b.id, SIZE(b.orderPositions), SIZE(b.purchasePositions) FROM Business b")
    List<Object[]> findAllLinkCounts();
}
