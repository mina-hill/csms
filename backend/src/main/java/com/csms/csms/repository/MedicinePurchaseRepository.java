package com.csms.csms.repository;

import com.csms.csms.entity.MedicinePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicinePurchaseRepository extends JpaRepository<MedicinePurchase, UUID> {

    List<MedicinePurchase> findByMedicineId(UUID medicineId);

    List<MedicinePurchase> findBySupplierId(UUID supplierId);

    List<MedicinePurchase> findByPurchaseDateBetween(LocalDate startDate, LocalDate endDate);

    List<MedicinePurchase> findByMedicineIdAndPurchaseDateBetween(UUID medicineId, LocalDate startDate, LocalDate endDate);

    List<MedicinePurchase> findBySupplierIdAndPurchaseDateBetween(UUID supplierId, LocalDate startDate, LocalDate endDate);
}