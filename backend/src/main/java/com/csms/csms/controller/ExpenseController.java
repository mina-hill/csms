package com.csms.csms.controller;

import com.csms.csms.entity.Expense;
import com.csms.csms.entity.ExpenseCategory;
import com.csms.csms.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    /**
     * POST /api/expenses
     * US-026: Record an expense.
     */
    @PostMapping
    public ResponseEntity<?> createExpense(@RequestBody ExpenseRequest request) {
        if (request.getCategory() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "category is required"));
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount must be positive"));
        }
        if (request.getExpenseDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "expenseDate is required"));
        }

        Expense expense = new Expense(
                request.getCategory(),
                request.getAmount(),
                request.getDescription(),
                request.getExpenseDate()
        );
        expense.setFlockId(request.getFlockId());       // nullable — safe to set null
        expense.setRecordedBy(request.getRecordedBy());

        return ResponseEntity.status(HttpStatus.CREATED).body(expenseRepository.save(expense));
    }

    /**
     * GET /api/expenses
     * Query params: category, startDate, endDate, flockId — all optional.
     * Priority: (category + dates) > (flock + dates) > dates only > flock only > category only > all.
     */
    @GetMapping
    public ResponseEntity<List<Expense>> getExpenses(
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) UUID flockId) {

        if (category != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(expenseRepository.findByCategoryAndExpenseDateBetween(category, startDate, endDate));
        }
        if (flockId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(expenseRepository.findByFlockIdAndExpenseDateBetween(flockId, startDate, endDate));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(expenseRepository.findByExpenseDateBetween(startDate, endDate));
        }
        if (flockId != null) {
            return ResponseEntity.ok(expenseRepository.findByFlockId(flockId));
        }
        if (category != null) {
            return ResponseEntity.ok(expenseRepository.findByCategory(category));
        }
        return ResponseEntity.ok(expenseRepository.findAll());
    }

    /**
     * GET /api/expenses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpenseById(@PathVariable UUID id) {
        return expenseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/expenses/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable UUID id, @RequestBody ExpenseRequest request) {
        return expenseRepository.findById(id)
                .map(expense -> {
                    if (request.getCategory() != null) expense.setCategory(request.getCategory());
                    if (request.getAmount() != null)   expense.setAmount(request.getAmount());
                    if (request.getDescription() != null) expense.setDescription(request.getDescription());
                    if (request.getExpenseDate() != null) expense.setExpenseDate(request.getExpenseDate());
                    expense.setFlockId(request.getFlockId());   // nullable — allow unlinking
                    return (ResponseEntity<?>) ResponseEntity.ok(expenseRepository.save(expense));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/expenses/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        if (expenseRepository.existsById(id)) {
            expenseRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

// ===== DTO =====

class ExpenseRequest {
    private ExpenseCategory category;
    private BigDecimal amount;
    private String description;
    private LocalDate expenseDate;
    private UUID flockId;
    private UUID recordedBy;

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
}
