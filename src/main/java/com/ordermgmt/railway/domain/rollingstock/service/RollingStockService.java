package com.ordermgmt.railway.domain.rollingstock.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.rollingstock.model.RollingStockItem;
import com.ordermgmt.railway.domain.rollingstock.repository.RollingStockRepository;

/**
 * CRUD facade for {@link RollingStockItem} master data. Wraps {@link RollingStockRepository} with
 * transaction and authorization boundaries used by the rolling-stock admin view.
 */
@Service
@Transactional
public class RollingStockService {

    private final RollingStockRepository rollingStockRepository;

    public RollingStockService(RollingStockRepository rollingStockRepository) {
        this.rollingStockRepository = rollingStockRepository;
    }

    @Transactional(readOnly = true)
    public List<RollingStockItem> findAll() {
        return rollingStockRepository.findAllByOrderByDesignationAsc();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public RollingStockItem save(RollingStockItem rollingStockItem) {
        return rollingStockRepository.save(rollingStockItem);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID id) {
        rollingStockRepository.deleteById(id);
    }
}
