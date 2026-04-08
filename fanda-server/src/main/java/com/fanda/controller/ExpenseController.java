package com.fanda.controller;

import com.fanda.dto.request.ExpenseCreateRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.Expense;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.repository.ExpenseRepository;
import com.fanda.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<Expense>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        List<Expense> expenses;

        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
        } else {
            expenses = expenseRepository.findByUserIdOrderByExpenseDateDesc(userId);
        }

        return ApiResponse.ok(expenses);
    }

    @PostMapping
    public ApiResponse<Expense> create(
            @RequestBody @Valid ExpenseCreateRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setExpenseDate(LocalDate.parse(request.getDate()));
        expense.setAmount(request.getAmount());
        expense.setMealType(request.getMealType());
        expense.setDescription(request.getDescription());

        expenseRepository.save(expense);
        return ApiResponse.ok(expense);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!expense.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        expenseRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private Long getCurrentUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .getId();
    }
}
