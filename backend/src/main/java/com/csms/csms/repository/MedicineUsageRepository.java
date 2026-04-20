package com.csms.csms.repository;

import com.csms.csms.entity.MedicineUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicineUsageRepository extends JpaRepository<MedicineUsage, UUID> {

    List<MedicineUsage> findByFlockId(UUID flockId);

    List<MedicineUsage> findByMedicineId(UUID medicineId);

    List<MedicineUsage> findByFlockIdAndMedicineId(UUID flockId, UUID medicineId);

    List<MedicineUsage> findByUsageDateBetween(LocalDate startDate, LocalDate endDate);

    List<MedicineUsage> findByFlockIdAndUsageDateBetween(UUID flockId, LocalDate startDate, LocalDate endDate);

    List<MedicineUsage> findByMedicineIdAndUsageDateBetween(UUID medicineId, LocalDate startDate, LocalDate endDate);

    List<MedicineUsage> findByFlockIdAndMedicineIdAndUsageDateBetween(UUID flockId, UUID medicineId, LocalDate startDate, LocalDate endDate);
}