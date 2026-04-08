package com.fanda.service.impl;

import com.fanda.dto.request.FoodCreateRequest;
import com.fanda.dto.response.FoodItemResponse;
import com.fanda.entity.*;
import com.fanda.repository.FoodItemRepository;
import com.fanda.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {

    private final FoodItemRepository foodItemRepository;

    @Override
    public List<FoodItemResponse> getAll(Long userId) {
        List<FoodItem> items = foodItemRepository.findByIsSystemTrueOrUserId(userId);
        return items.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<FoodItemResponse> getByCategory(String category, Long userId) {
        List<FoodItem> items = foodItemRepository.findByIsSystemTrueOrUserId(userId);
        return items.stream()
                .filter(item -> category.equals(item.getCategory()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FoodItemResponse> search(String keyword, Long userId) {
        List<FoodItem> items = foodItemRepository.findByNameContaining(keyword);
        return items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsSystem()) ||
                        (item.getUserId() != null && item.getUserId().equals(userId)))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FoodItemResponse addCustom(FoodCreateRequest request, Long userId) {
        FoodItem foodItem = new FoodItem();
        foodItem.setFoodCode("u_" + System.currentTimeMillis());
        foodItem.setName(request.getName());
        foodItem.setCategory(request.getCategory());
        foodItem.setSubCategory(request.getSubCategory());
        foodItem.setPriceMin(request.getPriceMin());
        foodItem.setPriceMax(request.getPriceMax());
        foodItem.setIsSystem(false);
        foodItem.setUserId(userId);

        if (request.getTags() != null) {
            for (String tag : request.getTags()) {
                FoodTag foodTag = new FoodTag();
                foodTag.setTag(tag);
                foodTag.setFoodItem(foodItem);
                foodItem.getTags().add(foodTag);
            }
        }

        if (request.getNutrition() != null) {
            for (String nutrition : request.getNutrition()) {
                FoodNutrition foodNutrition = new FoodNutrition();
                foodNutrition.setNutritionType(nutrition);
                foodNutrition.setFoodItem(foodItem);
                foodItem.getNutritions().add(foodNutrition);
            }
        }

        if (request.getAllergens() != null) {
            for (String allergen : request.getAllergens()) {
                FoodAllergen foodAllergen = new FoodAllergen();
                foodAllergen.setAllergen(allergen);
                foodAllergen.setFoodItem(foodItem);
                foodItem.getAllergens().add(foodAllergen);
            }
        }

        if (request.getMealTime() != null) {
            for (String mealTime : request.getMealTime()) {
                FoodMealTime foodMealTime = new FoodMealTime();
                foodMealTime.setMealTime(mealTime);
                foodMealTime.setFoodItem(foodItem);
                foodItem.getMealTimes().add(foodMealTime);
            }
        }

        foodItem = foodItemRepository.save(foodItem);
        return toResponse(foodItem);
    }

    private FoodItemResponse toResponse(FoodItem item) {
        List<BigDecimal> priceRange = new ArrayList<>();
        if (item.getPriceMin() != null) {
            priceRange.add(item.getPriceMin());
        }
        if (item.getPriceMax() != null) {
            priceRange.add(item.getPriceMax());
        }

        return FoodItemResponse.builder()
                .id(item.getFoodCode())
                .name(item.getName())
                .category(item.getCategory())
                .subCategory(item.getSubCategory())
                .priceRange(priceRange)
                .tags(item.getTags() != null
                        ? item.getTags().stream().map(FoodTag::getTag).collect(Collectors.toList())
                        : new ArrayList<>())
                .nutrition(item.getNutritions() != null
                        ? item.getNutritions().stream().map(FoodNutrition::getNutritionType).collect(Collectors.toList())
                        : new ArrayList<>())
                .allergens(item.getAllergens() != null
                        ? item.getAllergens().stream().map(FoodAllergen::getAllergen).collect(Collectors.toList())
                        : new ArrayList<>())
                .mealTime(item.getMealTimes() != null
                        ? item.getMealTimes().stream().map(FoodMealTime::getMealTime).collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }
}
