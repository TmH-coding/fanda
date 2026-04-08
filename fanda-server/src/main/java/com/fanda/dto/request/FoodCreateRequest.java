package com.fanda.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FoodCreateRequest {
    @NotBlank(message = "食物名称不能为空")
    private String name;

    @NotBlank(message = "分类不能为空")
    private String category;

    private String subCategory;

    private BigDecimal priceMin;

    private BigDecimal priceMax;

    private List<String> tags;

    private List<String> nutrition;

    private List<String> allergens;

    private List<String> mealTime;
}
