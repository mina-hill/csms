package com.csms.csms.repository;

import com.csms.csms.entity.FeedPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeedPurchaseRepository extends JpaRepository<FeedPurchase, UUID> {

    List<FeedPurchase> findByFeedTypeId(UUID feedTypeId);

    List<FeedPurchase> findBySupplierId(UUID supplierId);

    List<FeedPurchase> findByPurchaseDateBetween(LocalDate startDate, LocalDate endDate);

    List<FeedPurchase> findByFeedTypeIdAndPurchaseDateBetween(UUID feedTypeId, LocalDate startDate, LocalDate endDate);

    List<FeedPurchase> findBySupplierIdAndPurchaseDateBetween(UUID supplierId, LocalDate startDate, LocalDate endDate);
}