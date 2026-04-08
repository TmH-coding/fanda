package com.fanda.controller;

import com.fanda.dto.request.FoodCreateRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.dto.response.FoodItemResponse;
import com.fanda.entity.*;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.repository.FoodItemRepository;
import com.fanda.repository.UserRepository;
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

    private final FoodItemRepository foodItemRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<FoodItemResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            Authentication authentication) {

        List<FoodItem> foods;

        if (keyword != null && !keyword.isBlank()) {
            foods = foodItemRepository.findByNameContaining(keyword);
        } else if (category != null && !category.isBlank()) {
            foods = foodItemRepository.findByCategory(category);
        } else if (authentication != null) {
            Long userId = getCurrentUserId(authentication);
            foods = foodItemRepository.findByIsSystemTrueOrUserId(userId);
        } else {
            foods = foodItemRepository.findByIsSystemTrueOrUserId(null);
        }

        List<FoodItemResponse> result = foods.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    @GetMapping("/{foodCode}")
    public ApiResponse<FoodItemResponse> getByCode(@PathVariable String foodCode) {
        FoodItem food = foodItemRepository.findByFoodCode(foodCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
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

        if (request.getTags() != null) {
            List<FoodTag> tags = new ArrayList<>();
            for (String t : request.getTags()) {
                FoodTag tag = new FoodTag();
                tag.setTag(t);
                tag.setFoodItem(food);
                tags.add(tag);
            }
            food.setTags(tags);
        }

        if (request.getNutrition() != null) {
            List<FoodNutrition> nutritions = new ArrayList<>();
            for (String n : request.getNutrition()) {
                FoodNutrition fn = new FoodNutrition();
                fn.setNutritionType(n);
                fn.setFoodItem(food);
                nutritions.add(fn);
            }
            food.setNutritions(nutritions);
        }

        if (request.getAllergens() != null) {
            List<FoodAllergen> allergens = new ArrayList<>();
            for (String a : request.getAllergens()) {
                FoodAllergen fa = new FoodAllergen();
                fa.setAllergen(a);
                fa.setFoodItem(food);
                allergens.add(fa);
            }
            food.setAllergens(allergens);
        }

        if (request.getMealTime() != null) {
            List<FoodMealTime> mealTimes = new ArrayList<>();
            for (String m : request.getMealTime()) {
                FoodMealTime fmt = new FoodMealTime();
                fmt.setMealTime(m);
                fmt.setFoodItem(food);
                mealTimes.add(fmt);
            }
            food.setMealTimes(mealTimes);
        }

        foodItemRepository.save(food);
        return ApiResponse.ok(toResponse(food));
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
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .getId();
    }
}
