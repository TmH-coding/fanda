package com.fanda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("fd_food_nutrition")
public class FoodNutrition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long foodId;

    private String nutritionType;
}
