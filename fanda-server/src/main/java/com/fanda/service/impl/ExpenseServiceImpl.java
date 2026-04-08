package com.fanda.service.impl;

import com.fanda.dto.request.ExpenseCreateRequest;
import com.fanda.entity.Expense;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.repository.ExpenseRepository;
import com.fanda.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Override
    public List<Expense> getAll(Long userId) {
        return expenseRepository.findByUserIdOrderByExpenseDateDesc(userId);
    }

    @Override
    public List<Expense> getByMonth(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
    }

    @Override
    @Transactional
    public Expense create(ExpenseCreateRequest request, Long userId) {
        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setExpenseDate(LocalDate.parse(request.getDate()));
        expense.setAmount(request.getAmount());
        expense.setMealType(request.getMealType());
        expense.setDescription(request.getDescription());
        return expenseRepository.save(expense);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!expense.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        expenseRepository.delete(expense);
    }
}
