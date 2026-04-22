package com.csms.csms.repository;

import com.csms.csms.entity.VResourceConsumption;
import com.csms.csms.entity.VResourceConsumptionId; // Import the new class
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface VResourceConsumptionRepository extends JpaRepository<VResourceConsumption, VResourceConsumptionId> {

    List<VResourceConsumption> findByFlockId(UUID flockId);

    List<VResourceConsumption> findByResourceType(String resourceType);
}
