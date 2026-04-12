package com.fanda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.dto.request.ExpenseCreateRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.Expense;
import com.fanda.entity.User;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.mapper.ExpenseMapper;
import com.fanda.mapper.UserMapper;
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

    private final ExpenseMapper expenseMapper;
    private final UserMapper userMapper;

    @GetMapping
    public ApiResponse<List<Expense>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        List<Expense> expenses;

        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            expenses = expenseMapper.selectList(new LambdaQueryWrapper<Expense>()
                    .eq(Expense::getUserId, userId)
                    .between(Expense::getExpenseDate, ym.atDay(1), ym.atEndOfMonth()));
        } else {
            expenses = expenseMapper.selectList(new LambdaQueryWrapper<Expense>()
                    .eq(Expense::getUserId, userId)
                    .orderByDesc(Expense::getExpenseDate));
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
        expenseMapper.insert(expense);

        return ApiResponse.ok(expense);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        Expense expense = expenseMapper.selectById(id);
        if (expense == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (!expense.getUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);

        expenseMapper.deleteById(id);
        return ApiResponse.ok();
    }

    private Long getCurrentUserId(Authentication auth) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return user.getId();
    }
}
