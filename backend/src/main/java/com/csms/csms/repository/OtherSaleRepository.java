package com.csms.csms.repository;

import com.csms.csms.entity.OtherSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface OtherSaleRepository extends JpaRepository<OtherSale, UUID> {

    List<OtherSale> findBySaleDateBetween(LocalDate startDate, LocalDate endDate);

    List<OtherSale> findAllByOrderBySaleDateDescSaleIdDesc();
}
