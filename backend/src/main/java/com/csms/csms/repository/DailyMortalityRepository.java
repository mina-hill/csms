package com.csms.csms.repository;

import com.csms.csms.entity.DailyMortality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyMortalityRepository extends JpaRepository<DailyMortality, UUID> {

    /**
     * Used by DailyRecordController to fetch mortality records for a flock
     * within a date range (e.g. for weekly summary aggregation and reports).
     */
    List<DailyMortality> findByFlockIdAndRecordDateBetween(
            UUID flockId, LocalDate start, LocalDate end);

    /**
     * Used by the controller to check for the unique constraint
     * (flock_id, record_date, shift) before inserting.
     */
    Optional<DailyMortality> findByFlockIdAndRecordDateAndShift(
            UUID flockId, LocalDate recordDate, String shift);

    /**
     * Used to compute cumulative mortality for a flock up to a date.
     */
    List<DailyMortality> findByFlockId(UUID flockId);
}
