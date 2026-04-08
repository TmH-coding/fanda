package com.fanda.repository;

import com.fanda.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    Optional<FoodItem> findByFoodCode(String foodCode);
    List<FoodItem> findByCategory(String category);
    List<FoodItem> findByIsSystemTrueOrUserId(Long userId);
    List<FoodItem> findByNameContaining(String keyword);
    boolean existsByFoodCode(String foodCode);
}
