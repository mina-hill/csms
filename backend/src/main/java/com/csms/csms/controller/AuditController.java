package com.csms.csms.controller;

import com.csms.csms.auth.CsmsAccessHelper;
import com.csms.csms.entity.AuditLog;
import com.csms.csms.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * GET  /api/audit — full log, newest first
 * GET  /api/audit?flockId=uuid — events for that flock (any entity_type)
 * GET  /api/audit?table=... or ?entityType=... — filter by entity type (legacy table= supported)
 * GET  /api/audit?flockId=...&entityType=... — both
 * POST /api/audit — optional manual entry (prefer server-side writers)
 */
@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    @Autowired
    @Qualifier("auditLogRepository")
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CsmsAccessHelper accessHelper;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLog(
            @RequestParam(required = false) String table,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID flockId) {

        String type = (entityType != null && !entityType.isBlank())
                ? entityType
                : (table != null && !table.isBlank() ? table : null);

        if (flockId != null && type != null) {
            return ResponseEntity.ok(
                    auditLogRepository.findByFlockIdAndEntityTypeOrderByLoggedAtDesc(flockId, type));
        }
        if (flockId != null) {
            return ResponseEntity.ok(
                    auditLogRepository.findByFlockIdOrderByLoggedAtDesc(flockId));
        }
        if (type != null) {
            List<AuditLog> byEntity = auditLogRepository.findByEntityTypeOrderByLoggedAtDesc(type);
            if (!byEntity.isEmpty()) {
                return ResponseEntity.ok(byEntity);
            }
            return ResponseEntity.ok(
                    auditLogRepository.findByTableNameOrderByLoggedAtDesc(type));
        }
        return ResponseEntity.ok(
                auditLogRepository.findAllByOrderByLoggedAtDesc());
    }

    @PostMapping
    public ResponseEntity<AuditLog> createAuditEntry(
            @RequestBody AuditEntryRequest req,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        if (req.getAction() == null || req.getAction().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String et = (req.getEntityType() != null && !req.getEntityType().isBlank())
                ? req.getEntityType()
                : req.getTableName();
        if (et == null || et.isBlank()) {
            et = "manual";
        }
        AuditLog entry = new AuditLog(
                req.getUserId(),
                req.getAction(),
                et,
                req.getRecordId(),
                req.getFlockId(),
                req.getDetails()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auditLogRepository.save(entry));
    }
}

class AuditEntryRequest {
    private UUID   userId;
    private String action;
    private String tableName;
    private String entityType;
    private UUID   recordId;
    private UUID   flockId;
    private String details;

    public AuditEntryRequest() {}

    public UUID   getUserId()               { return userId; }
    public void   setUserId(UUID v)         { this.userId = v; }

    public String getAction()              { return action; }
    public void   setAction(String v)      { this.action = v; }

    public String getTableName()           { return tableName; }
    public void   setTableName(String v)   { this.tableName = v; }

    public String getEntityType()          { return entityType; }
    public void   setEntityType(String v)  { this.entityType = v; }

    public UUID   getRecordId()             { return recordId; }
    public void   setRecordId(UUID v)       { this.recordId = v; }

    public UUID   getFlockId()              { return flockId; }
    public void   setFlockId(UUID v)        { this.flockId = v; }

    public String getDetails()             { return details; }
    public void   setDetails(String v)     { this.details = v; }
}
