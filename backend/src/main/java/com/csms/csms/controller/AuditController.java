package com.csms.csms.controller;

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
 * AuditController
 * ───────────────
 * GET  /api/audit           — full system audit log, newest-first
 * GET  /api/audit?table=... — filter by table_name
 * POST /api/audit           — write a single entry (used by frontend client-side actions)
 */
@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    @Autowired
    @Qualifier("auditLogRepository")
    private AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLog(
            @RequestParam(required = false) String table) {

        if (table != null && !table.isBlank()) {
            return ResponseEntity.ok(
                    auditLogRepository.findByTableNameOrderByLoggedAtDesc(table));
        }
        return ResponseEntity.ok(
                auditLogRepository.findAllByOrderByLoggedAtDesc());
    }

    @PostMapping
    public ResponseEntity<AuditLog> createAuditEntry(
            @RequestBody AuditEntryRequest req) {

        if (req.getAction() == null || req.getAction().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AuditLog entry = new AuditLog(
                req.getUserId(),
                req.getAction(),
                req.getTableName(),
                req.getRecordId(),
                req.getDetails()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auditLogRepository.save(entry));
    }
}

// ── DTO ───────────────────────────────────────────────────────────────────────

class AuditEntryRequest {
    private UUID   userId;
    private String action;
    private String tableName;
    private UUID   recordId;
    private String details;

    public AuditEntryRequest() {}

    public UUID   getUserId()               { return userId; }
    public void   setUserId(UUID v)         { this.userId = v; }

    public String getAction()              { return action; }
    public void   setAction(String v)      { this.action = v; }

    public String getTableName()           { return tableName; }
    public void   setTableName(String v)   { this.tableName = v; }

    public UUID   getRecordId()             { return recordId; }
    public void   setRecordId(UUID v)       { this.recordId = v; }

    public String getDetails()             { return details; }
    public void   setDetails(String v)     { this.details = v; }
}