package com.csms.csms.repository;

import com.csms.csms.entity.FeedSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeedSaleRepository extends JpaRepository<FeedSale, UUID> {

    List<FeedSale> findBySaleDateBetween(LocalDate startDate, LocalDate endDate);
}