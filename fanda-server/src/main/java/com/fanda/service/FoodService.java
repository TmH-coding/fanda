package com.fanda.service;

import com.fanda.dto.request.FoodCreateRequest;
import com.fanda.dto.response.FoodItemResponse;

import java.util.List;

public interface FoodService {
    List<FoodItemResponse> getAll(Long userId);
    List<FoodItemResponse> getByCategory(String category, Long userId);
    List<FoodItemResponse> search(String keyword, Long userId);
    FoodItemResponse addCustom(FoodCreateRequest request, Long userId);
}
