package com.ordermgmt.railway.domain.rollingstock.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.rollingstock.model.RollingStockItem;
import com.ordermgmt.railway.domain.rollingstock.model.VehicleCategory;

@Repository
public interface RollingStockRepository
        extends JpaRepository<RollingStockItem, UUID>,
                RevisionRepository<RollingStockItem, UUID, Long> {

    List<RollingStockItem> findByActiveTrueOrderByDesignationAsc();

    List<RollingStockItem> findByVehicleCategoryAndActiveTrueOrderByDesignationAsc(
            VehicleCategory category);

    List<RollingStockItem> findAllByOrderByDesignationAsc();
}
