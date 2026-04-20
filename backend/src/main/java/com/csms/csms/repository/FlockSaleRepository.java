package com.csms.csms.repository;

import com.csms.csms.entity.FlockSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlockSaleRepository extends JpaRepository<FlockSale, UUID> {

    List<FlockSale> findByFlockId(UUID flockId);

    List<FlockSale> findBySaleDateBetween(LocalDate startDate, LocalDate endDate);

    List<FlockSale> findByFlockIdAndSaleDateBetween(UUID flockId, LocalDate startDate, LocalDate endDate);
}
