package com.ordermgmt.railway.domain.rollingstock.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.rollingstock.model.RollingStockItem;
import com.ordermgmt.railway.domain.rollingstock.model.VehicleCategory;
import com.ordermgmt.railway.domain.rollingstock.repository.RollingStockRepository;

/**
 * CRUD facade for {@link RollingStockItem} master data. Wraps
 * {@link RollingStockRepository} with active/category filters used by the Vehicle
 * Planning vehicle picker and the rolling-stock admin view.
 */
@Service
@Transactional
public class RollingStockService {

    private final RollingStockRepository repository;

    public RollingStockService(RollingStockRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<RollingStockItem> findAll() {
        return repository.findAllByOrderByDesignationAsc();
    }

    @Transactional(readOnly = true)
    public List<RollingStockItem> findActive() {
        return repository.findByActiveTrueOrderByDesignationAsc();
    }

    @Transactional(readOnly = true)
    public List<RollingStockItem> findByCategory(VehicleCategory category) {
        return repository.findByVehicleCategoryAndActiveTrueOrderByDesignationAsc(category);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public RollingStockItem save(RollingStockItem item) {
        return repository.save(item);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
