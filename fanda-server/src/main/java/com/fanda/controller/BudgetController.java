package com.fanda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.dto.request.BudgetUpdateRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.Budget;
import com.fanda.entity.User;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.mapper.BudgetMapper;
import com.fanda.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetMapper budgetMapper;
    private final UserMapper userMapper;

    @GetMapping
    public ApiResponse<Budget> get(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        Budget budget = budgetMapper.selectOne(
                new LambdaQueryWrapper<Budget>().eq(Budget::getUserId, userId));
        if (budget == null) {
            budget = new Budget();
            budget.setUserId(userId);
            budget.setMonthly(new BigDecimal("2000"));
            budget.setWeekly(new BigDecimal("500"));
            budgetMapper.insert(budget);
        }

        return ApiResponse.ok(budget);
    }

    @PutMapping
    public ApiResponse<Budget> update(
            @RequestBody @Valid BudgetUpdateRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        Budget budget = budgetMapper.selectOne(
                new LambdaQueryWrapper<Budget>().eq(Budget::getUserId, userId));
        if (budget == null) {
            budget = new Budget();
            budget.setUserId(userId);
        }

        budget.setMonthly(request.getMonthly());
        budget.setWeekly(request.getWeekly());

        if (budget.getId() == null) {
            budgetMapper.insert(budget);
        } else {
            budgetMapper.updateById(budget);
        }

        return ApiResponse.ok(budget);
    }

    private Long getCurrentUserId(Authentication auth) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return user.getId();
    }
}
