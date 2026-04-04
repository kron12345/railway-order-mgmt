package com.ordermgmt.railway.domain.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.ResourceCatalogItem;

/** Repository for resource catalog items (vehicle types, personnel qualifications). */
@Repository
public interface ResourceCatalogItemRepository extends JpaRepository<ResourceCatalogItem, UUID> {

    List<ResourceCatalogItem> findByCategoryAndActiveTrue(String category);

    List<ResourceCatalogItem> findByCategoryOrderBySortOrderAsc(String category);
}
