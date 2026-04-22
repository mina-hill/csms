package com.csms.csms.repository;

import com.csms.csms.entity.VResourceConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VResourceConsumptionRepository extends JpaRepository<VResourceConsumption, Long> {

    List<VResourceConsumption> findByFlockId(UUID flockId);

    List<VResourceConsumption> findByResourceType(String resourceType);
}