package com.ordermgmt.railway.domain.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.R2pInboxEntry;
import com.ordermgmt.railway.domain.order.model.R2pInboxStatus;

/** Repository for inbound R2P inbox entries. */
@Repository
public interface R2pInboxEntryRepository extends JpaRepository<R2pInboxEntry, UUID> {

    List<R2pInboxEntry> findByStatusOrderByReceivedAtDesc(R2pInboxStatus status);
}
