package com.fanda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.Expense;
import com.fanda.entity.MealRecord;
import com.fanda.entity.RecordNutrition;
import com.fanda.entity.User;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.mapper.ExpenseMapper;
import com.fanda.mapper.MealRecordMapper;
import com.fanda.mapper.RecordNutritionMapper;
import com.fanda.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final MealRecordMapper mealRecordMapper;
    private final RecordNutritionMapper recordNutritionMapper;
    private final ExpenseMapper expenseMapper;
    private final UserMapper userMapper;

    /**
     * 消费趋势统计
     * 返回: 按天消费、按餐次消费、月总计、日均
     */
    @GetMapping("/expense")
    public ApiResponse<Map<String, Object>> expenseStats(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        YearMonth ym = YearMonth.of(year, month);

        List<Expense> expenses = expenseMapper.selectList(new LambdaQueryWrapper<Expense>()
                .eq(Expense::getUserId, userId)
                .between(Expense::getExpenseDate, ym.atDay(1), ym.atEndOfMonth())
                .orderByAsc(Expense::getExpenseDate));

        // 按天聚合
        Map<String, BigDecimal> byDay = new LinkedHashMap<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            byDay.put(String.format("%02d", d), BigDecimal.ZERO);
        }
        expenses.forEach(e -> {
            String day = String.format("%02d", e.getExpenseDate().getDayOfMonth());
            byDay.merge(day, e.getAmount(), BigDecimal::add);
        });

        // 按餐次聚合
        Map<String, BigDecimal> byMealType = new LinkedHashMap<>();
        expenses.forEach(e -> {
            String mt = e.getMealType() != null ? e.getMealType() : "其他";
            byMealType.merge(mt, e.getAmount(), BigDecimal::add);
        });

        // 总计
        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 日均（仅统计有消费的天）
        long activeDays = byDay.values().stream().filter(v -> v.compareTo(BigDecimal.ZERO) > 0).count();
        BigDecimal avgPerDay = activeDays > 0
                ? total.divide(BigDecimal.valueOf(activeDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byDay", byDay);
        result.put("byMealType", byMealType);
        result.put("total", total);
        result.put("avgPerDay", avgPerDay);
        result.put("activeDays", activeDays);

        return ApiResponse.ok(result);
    }

    /**
     * 营养摄入统计
     * 返回: 各营养类型出现次数（月维度）
     */
    @GetMapping("/nutrition")
    public ApiResponse<Map<String, Object>> nutritionStats(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        YearMonth ym = YearMonth.of(year, month);

        // 当月所有记录 ID
        List<MealRecord> records = mealRecordMapper.selectList(new LambdaQueryWrapper<MealRecord>()
                .eq(MealRecord::getUserId, userId)
                .between(MealRecord::getRecordDate, ym.atDay(1), ym.atEndOfMonth())
                .select(MealRecord::getId));

        List<Long> recordIds = records.stream().map(MealRecord::getId).collect(Collectors.toList());

        Map<String, Long> nutritionCount = new LinkedHashMap<>();
        if (!recordIds.isEmpty()) {
            List<RecordNutrition> nutritions = recordNutritionMapper.selectList(
                    new LambdaQueryWrapper<RecordNutrition>().in(RecordNutrition::getRecordId, recordIds));
            nutritionCount = nutritions.stream()
                    .collect(Collectors.groupingBy(RecordNutrition::getNutritionType, Collectors.counting()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nutritionCount", nutritionCount);
        result.put("totalRecords", records.size());

        return ApiResponse.ok(result);
    }

    /**
     * 用餐习惯统计
     * 返回: 连续打卡天数、最爱食物、最爱餐次、总记录数
     */
    @GetMapping("/habit")
    public ApiResponse<Map<String, Object>> habitStats(Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        List<MealRecord> allRecords = mealRecordMapper.selectList(new LambdaQueryWrapper<MealRecord>()
                .eq(MealRecord::getUserId, userId)
                .orderByDesc(MealRecord::getRecordDate));

        // 连续打卡天数
        int streak = calcStreak(allRecords);

        // 最爱食物 Top5
        Map<String, Long> foodCount = allRecords.stream()
                .collect(Collectors.groupingBy(MealRecord::getFoodName, Collectors.counting()));
        List<Map<String, Object>> topFoods = foodCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        // 最爱餐次
        Map<String, Long> mealTypeCount = allRecords.stream()
                .filter(r -> r.getMealType() != null)
                .collect(Collectors.groupingBy(MealRecord::getMealType, Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRecords", allRecords.size());
        result.put("streak", streak);
        result.put("topFoods", topFoods);
        result.put("mealTypeCount", mealTypeCount);

        return ApiResponse.ok(result);
    }

    private int calcStreak(List<MealRecord> records) {
        if (records.isEmpty()) return 0;
        Set<LocalDate> dates = records.stream().map(MealRecord::getRecordDate).collect(Collectors.toSet());
        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate cur = dates.contains(today) ? today : today.minusDays(1);
        while (dates.contains(cur)) {
            streak++;
            cur = cur.minusDays(1);
        }
        return streak;
    }

    private Long getCurrentUserId(Authentication auth) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return user.getId();
    }
}
