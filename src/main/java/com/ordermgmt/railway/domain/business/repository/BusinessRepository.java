package com.ordermgmt.railway.domain.business.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.business.model.Business;

/** Repository for persisting and querying businesses. */
@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {}
