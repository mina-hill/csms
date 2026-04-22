package com.csms.csms.repository;

import com.csms.csms.entity.VGlobalProfitLoss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface VGlobalProfitLossRepository extends JpaRepository<VGlobalProfitLoss, BigDecimal> {
}