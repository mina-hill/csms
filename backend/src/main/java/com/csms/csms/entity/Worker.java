package com.csms.csms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workers")
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "worker_id")
    private UUID workerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "role", nullable = false, length = 100)
    private String role;

    @Column(name = "contact", length = 20)
    private String contact;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Column(name = "salary_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal salaryRate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public Worker() {}

    public Worker(String name, String role, LocalDate joinDate, BigDecimal salaryRate) {
        this.name = name;
        this.role = role;
        this.joinDate = joinDate;
        this.salaryRate = salaryRate;
        this.isActive = true;
    }

    public Worker(String name, String role, String contact, LocalDate joinDate, BigDecimal salaryRate) {
        this.name = name;
        this.role = role;
        this.contact = contact;
        this.joinDate = joinDate;
        this.salaryRate = salaryRate;
        this.isActive = true;
    }

    // Getters & Setters
    public UUID getWorkerId() {
        return workerId;
    }

    public void setWorkerId(UUID workerId) {
        this.workerId = workerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public BigDecimal getSalaryRate() {
        return salaryRate;
    }

    public void setSalaryRate(BigDecimal salaryRate) {
        this.salaryRate = salaryRate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Worker{" +
                "workerId=" + workerId +
                ", name='" + name + '\'' +
                ", role='" + role + '\'' +
                ", salaryRate=" + salaryRate +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}