package com.fanda.controller;

import com.fanda.dto.request.BudgetUpdateRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.Budget;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.repository.BudgetRepository;
import com.fanda.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<Budget> get(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        Budget budget = budgetRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Budget b = new Budget();
                    b.setUserId(userId);
                    b.setMonthly(new java.math.BigDecimal("2000"));
                    b.setWeekly(new java.math.BigDecimal("500"));
                    return budgetRepository.save(b);
                });

        return ApiResponse.ok(budget);
    }

    @PutMapping
    public ApiResponse<Budget> update(
            @RequestBody @Valid BudgetUpdateRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        Budget budget = budgetRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Budget b = new Budget();
                    b.setUserId(userId);
                    return b;
                });

        budget.setMonthly(request.getMonthly());
        budget.setWeekly(request.getWeekly());
        budgetRepository.save(budget);

        return ApiResponse.ok(budget);
    }

    private Long getCurrentUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .getId();
    }
}
