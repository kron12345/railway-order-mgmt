package com.ordermgmt.railway.domain.infrastructure.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;

/** Repository for railway operational points. */
@Repository
public interface OperationalPointRepository extends JpaRepository<OperationalPoint, UUID> {

    Optional<OperationalPoint> findByUopid(String uopid);

    List<OperationalPoint> findByUopidIn(Collection<String> uopids);

    List<OperationalPoint> findByCountry(String country);

    List<OperationalPoint> findByNameContainingIgnoreCase(String name);

    Optional<OperationalPoint> findFirstByNameIgnoreCase(String name);

    Page<OperationalPoint> findByNameContainingIgnoreCaseOrUopidContainingIgnoreCase(
            String name, String uopid, Pageable pageable);

    long countByNameContainingIgnoreCaseOrUopidContainingIgnoreCase(String name, String uopid);

    long countByCountry(String country);

    void deleteByCountry(String country);
}
