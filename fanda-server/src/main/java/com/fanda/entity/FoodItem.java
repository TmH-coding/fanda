package com.fanda.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("fd_food_item")
public class FoodItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String foodCode;

    private String name;

    private String category;

    private String subCategory;

    private BigDecimal priceMin;

    private BigDecimal priceMax;

    private Boolean isSystem;

    private Long userId;

    @TableField(exist = false)
    private List<FoodTag> tags = new ArrayList<>();

    @TableField(exist = false)
    private List<FoodNutrition> nutritions = new ArrayList<>();

    @TableField(exist = false)
    private List<FoodAllergen> allergens = new ArrayList<>();

    @TableField(exist = false)
    private List<FoodMealTime> mealTimes = new ArrayList<>();
}
