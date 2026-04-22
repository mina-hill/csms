package com.csms.csms.repository;

import com.csms.csms.entity.VGlobalProfitLoss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VGlobalProfitLossRepository extends JpaRepository<VGlobalProfitLoss, Long> {
}