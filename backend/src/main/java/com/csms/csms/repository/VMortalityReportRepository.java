package com.csms.csms.repository;

import com.csms.csms.entity.VMortalityReport;
import com.csms.csms.entity.VMortalityReportId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Repository
public interface VMortalityReportRepository extends JpaRepository<VMortalityReport, VMortalityReportId> {

    List<VMortalityReport> findByFlockId(UUID flockId);

    List<VMortalityReport> findByRecordDateBetween(LocalDate startDate, LocalDate endDate);

    List<VMortalityReport> findByFlockIdAndRecordDateBetween(UUID flockId, LocalDate startDate, LocalDate endDate);
}
