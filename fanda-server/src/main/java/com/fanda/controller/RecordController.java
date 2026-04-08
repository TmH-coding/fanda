package com.fanda.controller;

import com.fanda.dto.request.RecordCreateRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.MealRecord;
import com.fanda.entity.RecordNutrition;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.repository.MealRecordRepository;
import com.fanda.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final MealRecordRepository mealRecordRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<MealRecord>> list(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        List<MealRecord> records;

        if (date != null && !date.isBlank()) {
            LocalDate localDate = LocalDate.parse(date);
            records = mealRecordRepository.findByUserIdAndRecordDate(userId, localDate);
        } else if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            records = mealRecordRepository.findByUserIdAndRecordDateBetween(userId, start, end);
        } else {
            records = mealRecordRepository.findByUserIdOrderByRecordDateDesc(userId);
        }

        return ApiResponse.ok(records);
    }

    @PostMapping
    public ApiResponse<MealRecord> create(
            @RequestBody @Valid RecordCreateRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        MealRecord record = new MealRecord();
        record.setUserId(userId);
        record.setRecordDate(LocalDate.parse(request.getDate()));
        record.setMealType(request.getMealType());
        record.setFoodName(request.getFoodName());
        record.setFoodId(request.getFoodId());
        record.setCost(request.getCost());

        if (request.getNutrition() != null) {
            List<RecordNutrition> nutritions = new ArrayList<>();
            for (String n : request.getNutrition()) {
                RecordNutrition rn = new RecordNutrition();
                rn.setNutritionType(n);
                rn.setMealRecord(record);
                nutritions.add(rn);
            }
            record.setNutritions(nutritions);
        }

        mealRecordRepository.save(record);
        return ApiResponse.ok(record);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        MealRecord record = mealRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        mealRecordRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private Long getCurrentUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .getId();
    }
}
