package com.csms.csms.repository;

import com.csms.csms.entity.BradaPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BradaPurchaseRepository extends JpaRepository<BradaPurchase, UUID> {

    List<BradaPurchase> findByFlockId(UUID flockId);

    List<BradaPurchase> findBySupplierId(UUID supplierId);

    List<BradaPurchase> findByPurchaseDateBetween(LocalDate startDate, LocalDate endDate);

    List<BradaPurchase> findByFlockIdAndPurchaseDateBetween(UUID flockId, LocalDate startDate, LocalDate endDate);

    List<BradaPurchase> findBySupplierIdAndPurchaseDateBetween(UUID supplierId, LocalDate startDate, LocalDate endDate);
}