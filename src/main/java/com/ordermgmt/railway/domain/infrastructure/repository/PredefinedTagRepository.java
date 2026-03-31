package com.ordermgmt.railway.domain.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;

/** Repository for predefined tags. */
@Repository
public interface PredefinedTagRepository extends JpaRepository<PredefinedTag, UUID> {

    List<PredefinedTag> findByCategoryOrderBySortOrder(String category);

    List<PredefinedTag> findByActiveTrueOrderBySortOrder();

    List<PredefinedTag> findAllByOrderByCategoryAscSortOrderAsc();

    Optional<PredefinedTag> findByNameAndCategory(String name, String category);
}
