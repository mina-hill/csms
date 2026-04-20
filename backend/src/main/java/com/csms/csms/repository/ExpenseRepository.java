package com.csms.csms.repository;

import com.csms.csms.entity.Expense;
import com.csms.csms.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByCategoryAndExpenseDateBetween(ExpenseCategory category, LocalDate startDate, LocalDate endDate);

    List<Expense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate);

    List<Expense> findByFlockId(UUID flockId);

    List<Expense> findByCategory(ExpenseCategory category);

    List<Expense> findByFlockIdAndExpenseDateBetween(UUID flockId, LocalDate startDate, LocalDate endDate);
}
