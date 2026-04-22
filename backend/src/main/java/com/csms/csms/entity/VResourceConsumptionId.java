package com.csms.csms.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class VResourceConsumptionId implements Serializable {
    private UUID flockId;
    private String resourceType;

    public VResourceConsumptionId() {}

    public VResourceConsumptionId(UUID flockId, String resourceType) {
        this.flockId = flockId;
        this.resourceType = resourceType;
    }

    // Hibernate needs equals and hashCode to compare keys
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VResourceConsumptionId that = (VResourceConsumptionId) o;
        return Objects.equals(flockId, that.flockId) && 
               Objects.equals(resourceType, that.resourceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flockId, resourceType);
    }
}
