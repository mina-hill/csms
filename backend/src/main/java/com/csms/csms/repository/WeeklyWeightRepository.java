package com.csms.csms.repository;

import com.csms.csms.entity.WeeklyWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeeklyWeightRepository extends JpaRepository<WeeklyWeight, UUID> {

    /**
     * Returns all weight records for a flock ordered by week ascending.
     * Used by DailyRecordController to list records and by WeeklySummaryController
     * to look up the avg_weight_kg for a given week.
     */
    List<WeeklyWeight> findByFlockIdOrderByWeekNumber(UUID flockId);

    /**
     * Unique-constraint guard: prevent duplicate (flock_id, week_number) before insert.
     */
    Optional<WeeklyWeight> findByFlockIdAndWeekNumber(UUID flockId, Integer weekNumber);
}
