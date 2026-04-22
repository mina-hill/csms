package com.csms.csms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "v_profit_loss")
@Immutable
public class VProfitLoss {

    @Id
    @Column(name = "flock_id")
    private UUID flockId;

    @Column(name = "breed")
    private String breed;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "flock_revenue")
    private BigDecimal flockRevenue;

    @Column(name = "estimated_feed_cost")
    private BigDecimal estimatedFeedCost;

    @Column(name = "estimated_medicine_cost")
    private BigDecimal estimatedMedicineCost;

    @Column(name = "brada_cost")
    private BigDecimal bradaCost;

    @Column(name = "expense_cost")
    private BigDecimal expenseCost;

    @Column(name = "net_profit")
    private BigDecimal netProfit;

    // Constructors
    public VProfitLoss() {}

    // Getters only (immutable)
    public UUID getFlockId() { return flockId; }

    public String getBreed() { return breed; }

    public LocalDate getArrivalDate() { return arrivalDate; }

    public LocalDate getCloseDate() { return closeDate; }

    public BigDecimal getFlockRevenue() { return flockRevenue; }

    public BigDecimal getEstimatedFeedCost() { return estimatedFeedCost; }

    public BigDecimal getEstimatedMedicineCost() { return estimatedMedicineCost; }

    public BigDecimal getBradaCost() { return bradaCost; }

    public BigDecimal getExpenseCost() { return expenseCost; }

    public BigDecimal getNetProfit() { return netProfit; }
}