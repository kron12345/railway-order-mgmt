package com.ordermgmt.railway.domain.infrastructure.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Operational points within a lat/lon bounding box — the map's viewport working set (capped).
     */
    List<OperationalPoint> findByLatitudeBetweenAndLongitudeBetween(
            double minLat, double maxLat, double minLon, double maxLon, Pageable pageable);

    long countByNameContainingIgnoreCaseOrUopidContainingIgnoreCase(String name, String uopid);

    /**
     * Paged name/UOPID search returning only the page content (no embedded count) — the fetch half
     * of a lazy grid; pair with {@link #countByNameContainingIgnoreCaseOrUopidContainingIgnoreCase}
     * for the count half. The Pageable carries the grid's server-side sort.
     */
    @Query(
            "select op from OperationalPoint op "
                    + "where lower(op.name) like lower(concat('%', :q, '%')) "
                    + "or lower(op.uopid) like lower(concat('%', :q, '%'))")
    List<OperationalPoint> searchByNameOrUopid(@Param("q") String q, Pageable pageable);

    long countByCountry(String country);

    void deleteByCountry(String country);
}
