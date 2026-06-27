package com.ordermgmt.railway.domain.business.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.business.model.BusinessDocument;
import com.ordermgmt.railway.dto.business.BusinessDocumentMeta;

/** Repository for business documents — metadata-only listing plus on-demand blob fetch. */
@Repository
public interface BusinessDocumentRepository extends JpaRepository<BusinessDocument, UUID> {

    /** Document metadata for a business (no blob), ordered oldest-first. */
    @Query(
            "select new com.ordermgmt.railway.dto.business.BusinessDocumentMeta("
                    + "d.id, d.filename, d.contentType, d.createdAt) "
                    + "from BusinessDocument d where d.business.id = :businessId "
                    + "order by d.createdAt")
    List<BusinessDocumentMeta> findMetaByBusinessId(@Param("businessId") UUID businessId);

    /** The blob of a single document, fetched only for the actual download. */
    @Query("select d.data from BusinessDocument d where d.id = :id")
    byte[] findDataById(@Param("id") UUID id);
}
