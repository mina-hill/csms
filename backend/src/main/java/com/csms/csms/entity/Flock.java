package com.csms.csms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "flocks")
public class Flock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "flock_id")
    private UUID flockId;

    @Column(name = "flock_code", unique = true, length = 20)
    private String flockCode;

    @Column(name = "breed", nullable = false, length = 100)
    private String breed;

    @Column(name = "initial_qty", nullable = false)
    private Integer initialQty;

    @Column(name = "current_qty", nullable = false)
    private Integer currentQty;

    @Column(name = "arrival_date", nullable = false)
    private LocalDate arrivalDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "flock_status")
    private FlockStatus status = FlockStatus.ACTIVE;

    @Column(name = "supplier_id")
    private UUID supplierId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Flock() {}

    public Flock(String flockCode, String breed, Integer initialQty,
                 LocalDate arrivalDate, UUID supplierId,
                 String notes, UUID createdBy) {
        this.flockCode   = flockCode;
        this.breed       = breed;
        this.initialQty  = initialQty;
        this.currentQty  = initialQty;
        this.arrivalDate = arrivalDate;
        this.supplierId  = supplierId;
        this.notes       = notes;
        this.createdBy   = createdBy;
        this.status      = FlockStatus.ACTIVE;
    }

    public UUID getFlockId()                         { return flockId; }
    public void setFlockId(UUID v)                   { this.flockId = v; }

    public String getFlockCode()                     { return flockCode; }
    public void setFlockCode(String v)               { this.flockCode = v; }

    public String getBreed()                         { return breed; }
    public void setBreed(String v)                   { this.breed = v; }

    public Integer getInitialQty()                   { return initialQty; }
    public void setInitialQty(Integer v)             { this.initialQty = v; }

    public Integer getCurrentQty()                   { return currentQty; }
    public void setCurrentQty(Integer v)             { this.currentQty = v; }

    public LocalDate getArrivalDate()                { return arrivalDate; }
    public void setArrivalDate(LocalDate v)          { this.arrivalDate = v; }

    public LocalDate getCloseDate()                  { return closeDate; }
    public void setCloseDate(LocalDate v)            { this.closeDate = v; }

    public FlockStatus getStatus()                   { return status; }
    public void setStatus(FlockStatus v)             { this.status = v; }

    public UUID getSupplierId()                      { return supplierId; }
    public void setSupplierId(UUID v)                { this.supplierId = v; }

    public String getNotes()                         { return notes; }
    public void setNotes(String v)                   { this.notes = v; }

    public UUID getCreatedBy()                       { return createdBy; }
    public void setCreatedBy(UUID v)                 { this.createdBy = v; }

    public String getClosedBy()                      { return closedBy; }
    public void setClosedBy(String v)                { this.closedBy = v; }

    public OffsetDateTime getCreatedAt()             { return createdAt; }
    public OffsetDateTime getUpdatedAt()             { return updatedAt; }
}