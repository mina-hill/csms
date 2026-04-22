package com.csms.csms.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /** Legacy filter key; kept in sync with entityType for new rows. */
    @Column(name = "table_name", length = 50)
    private String tableName;

    /** Domain entity / table semantic name, e.g. flocks, daily_mortality, weekly_weight. */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "record_id")
    private UUID recordId;

    /** Flock scope for filtering (nullable for global events). */
    @Column(name = "flock_id")
    private UUID flockId;

    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "logged_at", insertable = false, updatable = false)
    private OffsetDateTime loggedAt;

    public AuditLog() {}

    /**
     * Preferred constructor: one event model with explicit entity type and optional flock scope.
     */
    public AuditLog(UUID userId, String action, String entityType,
                    UUID recordId, UUID flockId, String details) {
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.tableName = entityType;
        this.recordId = recordId;
        this.flockId = flockId;
        this.details = details;
    }

    public UUID getLogId() { return logId; }
    public void setLogId(UUID v) { this.logId = v; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }

    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }

    public String getTableName() { return tableName; }
    public void setTableName(String v) { this.tableName = v; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String v) { this.entityType = v; }

    public UUID getRecordId() { return recordId; }
    public void setRecordId(UUID v) { this.recordId = v; }

    @JsonProperty("flockId")
    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID v) { this.flockId = v; }

    public String getDetails() { return details; }
    public void setDetails(String v) { this.details = v; }

    public OffsetDateTime getLoggedAt() { return loggedAt; }
}
