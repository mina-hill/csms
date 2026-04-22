package com.csms.csms.entity;

import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
@Entity
@Table(name = "v_resource_consumption")
@Immutable
public class VResourceConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "rowid")
    private Long rowId;

    @Column(name = "flock_id")
    private UUID flockId;

    @Column(name = "breed")
    private String breed;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "total_qty")
    private Integer totalQty;

    @Column(name = "unit")
    private String unit;

    // Constructors
    public VResourceConsumption() {}

    // Getters only (immutable)
    public Long getRowId() { return rowId; }

    public UUID getFlockId() { return flockId; }

    public String getBreed() { return breed; }

    public String getResourceType() { return resourceType; }

    public Integer getTotalQty() { return totalQty; }

    public String getUnit() { return unit; }
}