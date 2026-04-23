package com.csms.csms.repository;

import com.csms.csms.entity.FeedUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedUsageRepository extends JpaRepository<FeedUsage, UUID> {

    List<FeedUsage> findByFlockId(UUID flockId);

    List<FeedUsage> findByFeedTypeId(UUID feedTypeId);

    List<FeedUsage> findByFlockIdAndFeedTypeId(UUID flockId, UUID feedTypeId);

    List<FeedUsage> findByUsageDateBetween(LocalDate startDate, LocalDate endDate);

    List<FeedUsage> findByFlockIdAndUsageDateBetween(UUID flockId, LocalDate startDate, LocalDate endDate);

    List<FeedUsage> findByFeedTypeIdAndUsageDateBetween(UUID feedTypeId, LocalDate startDate, LocalDate endDate);

    List<FeedUsage> findByFlockIdAndFeedTypeIdAndUsageDateBetween(UUID flockId, UUID feedTypeId, LocalDate startDate, LocalDate endDate);

    Optional<FeedUsage> findByFlockIdAndFeedTypeIdAndUsageDateAndShift(
            UUID flockId,
            UUID feedTypeId,
            LocalDate usageDate,
            String shift
    );
}