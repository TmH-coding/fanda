package com.fanda.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fd_record_nutrition")
public class RecordNutrition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nutrition_type", length = 20)
    private String nutritionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    @JsonIgnore
    private MealRecord mealRecord;
}
