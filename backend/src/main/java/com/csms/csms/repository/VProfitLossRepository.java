package com.csms.csms.repository;

import com.csms.csms.entity.VProfitLoss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VProfitLossRepository extends JpaRepository<VProfitLoss, UUID> {
}