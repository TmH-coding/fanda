package com.fanda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.dto.request.RecordCreateRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.FoodItem;
import com.fanda.entity.MealRecord;
import com.fanda.entity.RecordNutrition;
import com.fanda.entity.User;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.mapper.FoodItemMapper;
import com.fanda.mapper.MealRecordMapper;
import com.fanda.mapper.RecordNutritionMapper;
import com.fanda.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final MealRecordMapper mealRecordMapper;
    private final RecordNutritionMapper recordNutritionMapper;
    private final FoodItemMapper foodItemMapper;
    private final UserMapper userMapper;

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
            records = mealRecordMapper.selectList(new LambdaQueryWrapper<MealRecord>()
                    .eq(MealRecord::getUserId, userId)
                    .eq(MealRecord::getRecordDate, localDate));
        } else if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            records = mealRecordMapper.selectList(new LambdaQueryWrapper<MealRecord>()
                    .eq(MealRecord::getUserId, userId)
                    .between(MealRecord::getRecordDate, ym.atDay(1), ym.atEndOfMonth()));
        } else {
            records = mealRecordMapper.selectList(new LambdaQueryWrapper<MealRecord>()
                    .eq(MealRecord::getUserId, userId)
                    .orderByDesc(MealRecord::getRecordDate));
        }

        records.forEach(r -> r.setNutritions(recordNutritionMapper.selectList(
                new LambdaQueryWrapper<RecordNutrition>().eq(RecordNutrition::getRecordId, r.getId()))));

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
        record.setCost(request.getCost());

        // foodCode → 数字 id
        if (request.getFoodCode() != null && !request.getFoodCode().isBlank()) {
            FoodItem food = foodItemMapper.selectOne(
                    new LambdaQueryWrapper<FoodItem>().eq(FoodItem::getFoodCode, request.getFoodCode()));
            if (food != null) record.setFoodId(food.getId());
        }
        mealRecordMapper.insert(record);

        if (request.getNutrition() != null) {
            for (String n : request.getNutrition()) {
                RecordNutrition rn = new RecordNutrition();
                rn.setRecordId(record.getId());
                rn.setNutritionType(n);
                recordNutritionMapper.insert(rn);
            }
        }

        return ApiResponse.ok(record);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        MealRecord record = mealRecordMapper.selectById(id);
        if (record == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (!record.getUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);

        recordNutritionMapper.delete(
                new LambdaQueryWrapper<RecordNutrition>().eq(RecordNutrition::getRecordId, id));
        mealRecordMapper.deleteById(id);
        return ApiResponse.ok();
    }

    private Long getCurrentUserId(Authentication auth) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return user.getId();
    }
}
