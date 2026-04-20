package com.csms.csms.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    // nullable — SET NULL on user delete
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "table_name", length = 50)
    private String tableName;

    @Column(name = "record_id")
    private UUID recordId;

    // JSONB stored as TEXT from the app side
    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "logged_at", insertable = false, updatable = false)
    private OffsetDateTime loggedAt;

    public AuditLog() {}

    public AuditLog(UUID userId, String action, String tableName,
                    UUID recordId, String details) {
        this.userId    = userId;
        this.action    = action;
        this.tableName = tableName;
        this.recordId  = recordId;
        this.details   = details;
    }

    public UUID getLogId()                           { return logId; }
    public void setLogId(UUID v)                     { this.logId = v; }

    public UUID getUserId()                          { return userId; }
    public void setUserId(UUID v)                    { this.userId = v; }

    public String getAction()                        { return action; }
    public void setAction(String v)                  { this.action = v; }

    public String getTableName()                     { return tableName; }
    public void setTableName(String v)               { this.tableName = v; }

    public UUID getRecordId()                        { return recordId; }
    public void setRecordId(UUID v)                  { this.recordId = v; }

    public String getDetails()                       { return details; }
    public void setDetails(String v)                 { this.details = v; }

    public OffsetDateTime getLoggedAt()              { return loggedAt; }
}
