package com.csms.csms.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "other_sales")
public class OtherSale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private OtherSaleCategory category;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "buyer_name", length = 120)
    private String buyerName;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    public OtherSale() {}

    public OtherSale(LocalDate saleDate, OtherSaleCategory category, String description, String buyer, BigDecimal amount) {
        this.saleDate = saleDate;
        this.category = category;
        this.description = description;
        this.buyerName = buyer;
        this.amount = amount;
    }

    @JsonProperty("id")
    public UUID getId() {
        return saleId;
    }

    @JsonProperty("id")
    public void setId(UUID saleId) {
        this.saleId = saleId;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public OtherSaleCategory getCategory() {
        return category;
    }

    public void setCategory(OtherSaleCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBuyer() {
        return buyerName;
    }

    public void setBuyer(String buyer) {
        this.buyerName = buyer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
