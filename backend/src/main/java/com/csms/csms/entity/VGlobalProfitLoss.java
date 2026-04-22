package com.csms.csms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;
import java.util.UUID;

@Entity
@Table(name = "v_global_profit_loss")
@Immutable
public class VGlobalProfitLoss {

   @Id // Map the ID to an existing column so Hibernate stops looking for "id"

    @Column(name = "total_revenue")
    private BigDecimal totalRevenue;

    @Column(name = "total_cash_outflow")
    private BigDecimal totalCashOutflow;

    @Column(name = "net_profit")
    private BigDecimal netProfit;

    public VGlobalProfitLoss() {}

    //public UUID getId() { return id; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal getTotalCashOutflow() { return totalCashOutflow; }
    public BigDecimal getNetProfit() { return netProfit; }
}