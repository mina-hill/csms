package com.csms.csms.entity;

import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.Immutable;


@Entity
@Table(name = "v_resource_consumption")
@Immutable
@IdClass(VResourceConsumptionId.class) // 1. Add this link
public class VResourceConsumption {

    @Id // 2. Keep this as ID
    @Column(name = "flock_id")
    private UUID flockId;

    @Id // 3. ADD THIS as an ID too
    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "breed")
    private String breed;

    @Column(name = "total_qty")
    private Integer totalQty;

    @Column(name = "unit")
    private String unit;

    public VResourceConsumption() {}

    public UUID getFlockId() { return flockId; }
    public String getBreed() { return breed; }
    public String getResourceType() { return resourceType; }
    public Integer getTotalQty() { return totalQty; }
    public String getUnit() { return unit; }
}
