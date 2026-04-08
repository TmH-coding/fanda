package com.fanda.service.impl;

import com.fanda.dto.request.BudgetUpdateRequest;
import com.fanda.entity.Budget;
import com.fanda.repository.BudgetRepository;
import com.fanda.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;

    @Override
    public Budget getBudget(Long userId) {
        return budgetRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Budget budget = new Budget();
                    budget.setUserId(userId);
                    budget.setMonthly(new BigDecimal("2000"));
                    budget.setWeekly(new BigDecimal("500"));
                    return budgetRepository.save(budget);
                });
    }

    @Override
    @Transactional
    public Budget updateBudget(BudgetUpdateRequest request, Long userId) {
        Budget budget = budgetRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Budget b = new Budget();
                    b.setUserId(userId);
                    return b;
                });

        budget.setMonthly(request.getMonthly());
        budget.setWeekly(request.getWeekly());
        return budgetRepository.save(budget);
    }
}
