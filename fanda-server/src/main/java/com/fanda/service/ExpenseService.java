package com.fanda.service;

import com.fanda.dto.request.ExpenseCreateRequest;
import com.fanda.entity.Expense;

import java.util.List;

public interface ExpenseService {
    List<Expense> getAll(Long userId);
    List<Expense> getByMonth(Long userId, int year, int month);
    Expense create(ExpenseCreateRequest request, Long userId);
    void delete(Long id, Long userId);
}
