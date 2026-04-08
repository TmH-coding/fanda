package com.fanda.repository;

import com.fanda.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserIdOrderByExpenseDateDesc(Long userId);
    List<Expense> findByUserIdAndExpenseDateBetween(Long userId, LocalDate start, LocalDate end);
}
