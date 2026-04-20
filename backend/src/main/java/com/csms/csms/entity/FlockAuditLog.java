package com.csms.csms.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "flock_audit_log")
public class FlockAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "flock_id", nullable = false)
    private UUID flockId;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(name = "changed_at", insertable = false, updatable = false)
    private OffsetDateTime changedAt;

    public FlockAuditLog() {}

    public FlockAuditLog(UUID flockId, UUID changedBy,
                         String oldValues, String newValues) {
        this.flockId   = flockId;
        this.changedBy = changedBy;
        this.oldValues = oldValues;
        this.newValues = newValues;
    }

    public UUID getLogId()                       { return logId; }
    public void setLogId(UUID v)                 { this.logId = v; }

    public UUID getFlockId()                     { return flockId; }
    public void setFlockId(UUID v)               { this.flockId = v; }

    public UUID getChangedBy()                   { return changedBy; }
    public void setChangedBy(UUID v)             { this.changedBy = v; }

    public String getOldValues()                 { return oldValues; }
    public void setOldValues(String v)           { this.oldValues = v; }

    public String getNewValues()                 { return newValues; }
    public void setNewValues(String v)           { this.newValues = v; }

    public OffsetDateTime getChangedAt()         { return changedAt; }
}