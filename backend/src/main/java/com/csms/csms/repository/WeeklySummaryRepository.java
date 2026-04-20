package com.csms.csms.repository;

import com.csms.csms.entity.WeeklySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeeklySummaryRepository extends JpaRepository<WeeklySummary, UUID> {

    /**
     * Returns all weekly snapshots for a flock ordered by week ascending.
     * Used by WeeklySummaryController GET /weekly-summary/{flockId}.
     */
    List<WeeklySummary> findByFlockIdOrderByWeekNumber(UUID flockId);

    /**
     * Unique-constraint guard: (flock_id, week_number) must be unique.
     * Used before upsert in POST /weekly-summary.
     */
    Optional<WeeklySummary> findByFlockIdAndWeekNumber(UUID flockId, Integer weekNumber);
}
