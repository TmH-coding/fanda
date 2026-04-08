package com.fanda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItemResponse {
    private String id;
    private String name;
    private String category;
    private String subCategory;
    private List<BigDecimal> priceRange;
    private List<String> tags;
    private List<String> nutrition;
    private List<String> allergens;
    private List<String> mealTime;
}
