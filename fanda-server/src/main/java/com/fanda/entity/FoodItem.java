package com.fanda.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fd_food_item")
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "food_code", length = 20, unique = true, nullable = false)
    private String foodCode;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "sub_category", length = 50)
    private String subCategory;

    @Column(name = "price_min")
    private BigDecimal priceMin;

    @Column(name = "price_max")
    private BigDecimal priceMax;

    @Column(name = "is_system")
    private Boolean isSystem;

    @Column(name = "user_id")
    private Long userId;

    @OneToMany(mappedBy = "foodItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FoodTag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "foodItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FoodNutrition> nutritions = new ArrayList<>();

    @OneToMany(mappedBy = "foodItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FoodAllergen> allergens = new ArrayList<>();

    @OneToMany(mappedBy = "foodItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FoodMealTime> mealTimes = new ArrayList<>();
}
