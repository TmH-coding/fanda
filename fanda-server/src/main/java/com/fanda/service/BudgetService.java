package com.fanda.service;

import com.fanda.dto.request.BudgetUpdateRequest;
import com.fanda.entity.Budget;

public interface BudgetService {
    Budget getBudget(Long userId);
    Budget updateBudget(BudgetUpdateRequest request, Long userId);
}
