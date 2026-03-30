package com.ordermgmt.railway.domain.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.infrastructure.model.SectionOfLine;

/** Repository for railway sections of line. */
@Repository
public interface SectionOfLineRepository extends JpaRepository<SectionOfLine, UUID> {

    List<SectionOfLine> findByCountry(String country);

    long countByCountry(String country);

    void deleteByCountry(String country);
}
