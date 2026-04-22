package com.csms.csms.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class VMortalityReportId implements Serializable {
    private UUID flockId;
    private LocalDate recordDate;

    public VMortalityReportId() {}

    public VMortalityReportId(UUID flockId, LocalDate recordDate) {
        this.flockId = flockId;
        this.recordDate = recordDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VMortalityReportId that = (VMortalityReportId) o;
        return Objects.equals(flockId, that.flockId) && 
               Objects.equals(recordDate, that.recordDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flockId, recordDate);
    }
}
