package com.ordermgmt.railway.domain.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceType;

/** Repository for resource needs linked to order positions. */
@Repository
public interface ResourceNeedRepository extends JpaRepository<ResourceNeed, UUID> {

    @Query(
            "SELECT rn FROM ResourceNeed rn"
                    + " LEFT JOIN FETCH rn.catalogItem"
                    + " WHERE rn.orderPosition.id = :positionId")
    List<ResourceNeed> findByOrderPositionIdWithCatalogItem(
            @Param("positionId") UUID orderPositionId);

    List<ResourceNeed> findByOrderPositionId(UUID orderPositionId);

    List<ResourceNeed> findByOrderPositionIdAndResourceTypeAndCoverageType(
            UUID orderPositionId, ResourceType resourceType, CoverageType coverageType);
}
