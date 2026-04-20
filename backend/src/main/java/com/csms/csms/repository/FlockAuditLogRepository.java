package com.csms.csms.repository;

import com.csms.csms.entity.FlockAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FlockAuditLogRepository extends JpaRepository<FlockAuditLog, UUID> {
    List<FlockAuditLog> findByFlockId(UUID flockId);
    List<FlockAuditLog> findByFlockIdOrderByChangedAtDesc(UUID flockId);
}