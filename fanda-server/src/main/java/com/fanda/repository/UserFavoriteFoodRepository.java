package com.fanda.repository;

import com.fanda.entity.UserFavoriteFood;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserFavoriteFoodRepository extends JpaRepository<UserFavoriteFood, Long> {
    List<UserFavoriteFood> findByUserId(Long userId);
    Optional<UserFavoriteFood> findByUserIdAndFoodId(Long userId, Long foodId);
    boolean existsByUserIdAndFoodId(Long userId, Long foodId);
    void deleteByUserIdAndFoodId(Long userId, Long foodId);
    long countByUserId(Long userId);
}
