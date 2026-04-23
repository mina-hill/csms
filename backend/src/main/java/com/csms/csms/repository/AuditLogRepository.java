package com.csms.csms.repository;

import com.csms.csms.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Used by AuditController GET /audit — full log newest-first.
     */
    List<AuditLog> findAllByOrderByLoggedAtDesc();

    /**
     * Used by AuditController GET /audit?table=... to filter by domain table.
     */
    List<AuditLog> findByTableNameOrderByLoggedAtDesc(String tableName);

    List<AuditLog> findByEntityTypeOrderByLoggedAtDesc(String entityType);

    List<AuditLog> findByFlockIdOrderByLoggedAtDesc(UUID flockId);

    List<AuditLog> findByFlockIdAndEntityTypeOrderByLoggedAtDesc(UUID flockId, String entityType);
}
