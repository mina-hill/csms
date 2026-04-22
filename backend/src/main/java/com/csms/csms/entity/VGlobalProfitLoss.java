package com.csms.csms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;
@Entity
@Table(name = "v_global_profit_loss")
@Immutable
public class VGlobalProfitLoss {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "rowid")
    private Long rowId;

    @Column(name = "total_revenue")
    private BigDecimal totalRevenue;

    @Column(name = "total_cash_outflow")
    private BigDecimal totalCashOutflow;

    @Column(name = "net_profit")
    private BigDecimal netProfit;

    // Constructors
    public VGlobalProfitLoss() {}

    // Getters only (immutable)
    public Long getRowId() { return rowId; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }

    public BigDecimal getTotalCashOutflow() { return totalCashOutflow; }

    public BigDecimal getNetProfit() { return netProfit; }
}