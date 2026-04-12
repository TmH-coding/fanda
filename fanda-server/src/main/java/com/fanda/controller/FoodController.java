package com.fanda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.dto.request.FoodCreateRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.dto.response.FoodItemResponse;
import com.fanda.entity.*;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.mapper.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodItemMapper foodItemMapper;
    private final FoodTagMapper foodTagMapper;
    private final FoodNutritionMapper foodNutritionMapper;
    private final FoodAllergenMapper foodAllergenMapper;
    private final FoodMealTimeMapper foodMealTimeMapper;
    private final UserMapper userMapper;

    @GetMapping
    public ApiResponse<List<FoodItemResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            Authentication authentication) {

        List<FoodItem> foods;

        if (keyword != null && !keyword.isBlank()) {
            foods = foodItemMapper.selectList(
                    new LambdaQueryWrapper<FoodItem>().like(FoodItem::getName, keyword));
        } else if (category != null && !category.isBlank()) {
            foods = foodItemMapper.selectList(
                    new LambdaQueryWrapper<FoodItem>().eq(FoodItem::getCategory, category));
        } else if (authentication != null) {
            Long userId = getCurrentUserId(authentication);
            foods = foodItemMapper.selectList(
                    new LambdaQueryWrapper<FoodItem>()
                            .eq(FoodItem::getIsSystem, true)
                            .or().eq(FoodItem::getUserId, userId));
        } else {
            foods = foodItemMapper.selectList(
                    new LambdaQueryWrapper<FoodItem>().eq(FoodItem::getIsSystem, true));
        }

        foods.forEach(this::loadChildren);
        return ApiResponse.ok(foods.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{foodCode}")
    public ApiResponse<FoodItemResponse> getByCode(@PathVariable String foodCode) {
        FoodItem food = foodItemMapper.selectOne(
                new LambdaQueryWrapper<FoodItem>().eq(FoodItem::getFoodCode, foodCode));
        if (food == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        loadChildren(food);
        return ApiResponse.ok(toResponse(food));
    }

    @PostMapping
    public ApiResponse<FoodItemResponse> create(
            @RequestBody @Valid FoodCreateRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        FoodItem food = new FoodItem();
        food.setFoodCode("U_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        food.setName(request.getName());
        food.setCategory(request.getCategory());
        food.setSubCategory(request.getSubCategory());
        food.setPriceMin(request.getPriceMin());
        food.setPriceMax(request.getPriceMax());
        food.setIsSystem(false);
        food.setUserId(userId);
        foodItemMapper.insert(food);

        if (request.getTags() != null) {
            for (String t : request.getTags()) {
                FoodTag tag = new FoodTag();
                tag.setFoodId(food.getId());
                tag.setTag(t);
                foodTagMapper.insert(tag);
            }
        }
        if (request.getNutrition() != null) {
            for (String n : request.getNutrition()) {
                FoodNutrition fn = new FoodNutrition();
                fn.setFoodId(food.getId());
                fn.setNutritionType(n);
                foodNutritionMapper.insert(fn);
            }
        }
        if (request.getAllergens() != null) {
            for (String a : request.getAllergens()) {
                FoodAllergen fa = new FoodAllergen();
                fa.setFoodId(food.getId());
                fa.setAllergen(a);
                foodAllergenMapper.insert(fa);
            }
        }
        if (request.getMealTime() != null) {
            for (String m : request.getMealTime()) {
                FoodMealTime fmt = new FoodMealTime();
                fmt.setFoodId(food.getId());
                fmt.setMealTime(m);
                foodMealTimeMapper.insert(fmt);
            }
        }

        loadChildren(food);
        return ApiResponse.ok(toResponse(food));
    }

    private void loadChildren(FoodItem food) {
        Long id = food.getId();
        food.setTags(foodTagMapper.selectList(
                new LambdaQueryWrapper<FoodTag>().eq(FoodTag::getFoodId, id)));
        food.setNutritions(foodNutritionMapper.selectList(
                new LambdaQueryWrapper<FoodNutrition>().eq(FoodNutrition::getFoodId, id)));
        food.setAllergens(foodAllergenMapper.selectList(
                new LambdaQueryWrapper<FoodAllergen>().eq(FoodAllergen::getFoodId, id)));
        food.setMealTimes(foodMealTimeMapper.selectList(
                new LambdaQueryWrapper<FoodMealTime>().eq(FoodMealTime::getFoodId, id)));
    }

    private FoodItemResponse toResponse(FoodItem food) {
        List<BigDecimal> priceRange = new ArrayList<>();
        if (food.getPriceMin() != null) priceRange.add(food.getPriceMin());
        if (food.getPriceMax() != null) priceRange.add(food.getPriceMax());

        return FoodItemResponse.builder()
                .id(food.getFoodCode())
                .name(food.getName())
                .category(food.getCategory())
                .subCategory(food.getSubCategory())
                .priceRange(priceRange)
                .tags(food.getTags().stream().map(FoodTag::getTag).collect(Collectors.toList()))
                .nutrition(food.getNutritions().stream().map(FoodNutrition::getNutritionType).collect(Collectors.toList()))
                .allergens(food.getAllergens().stream().map(FoodAllergen::getAllergen).collect(Collectors.toList()))
                .mealTime(food.getMealTimes().stream().map(FoodMealTime::getMealTime).collect(Collectors.toList()))
                .build();
    }

    private Long getCurrentUserId(Authentication auth) {
        String username = auth.getName();
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return user.getId();
    }
}
