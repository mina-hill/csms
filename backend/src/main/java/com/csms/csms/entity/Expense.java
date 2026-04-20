package com.csms.csms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "expense_id")
    private UUID expenseId;

    // FIX-4: SQL column is a Postgres custom enum type (expense_category), not a plain VARCHAR.
    // Must use columnDefinition so Hibernate passes the right type to the driver.
    // @Enumerated(EnumType.STRING) alone produces a cast error against a custom Postgres enum.
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, columnDefinition = "expense_category")
    private ExpenseCategory category;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    // flock_id is optional (nullable) — no nullable = false here
    @Column(name = "flock_id")
    private UUID flockId;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    // FIX-5: DB-stamped timestamp — insertable=false, updatable=false, no setter.
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public Expense() {}

    public Expense(ExpenseCategory category, BigDecimal amount, LocalDate expenseDate) {
        this.category = category;
        this.amount = amount;
        this.expenseDate = expenseDate;
    }

    public Expense(ExpenseCategory category, BigDecimal amount, String description, LocalDate expenseDate) {
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.expenseDate = expenseDate;
    }

    // Getters & Setters
    public UUID getExpenseId() { return expenseId; }
    public void setExpenseId(UUID expenseId) { this.expenseId = expenseId; }

    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) { this.category = category; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID flockId) { this.flockId = flockId; }

    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }

    // FIX-5: getter only — no setter for DB-stamped timestamp
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
