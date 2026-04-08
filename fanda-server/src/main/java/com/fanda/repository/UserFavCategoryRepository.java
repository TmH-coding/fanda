package com.fanda.repository;

import com.fanda.entity.UserFavCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserFavCategoryRepository extends JpaRepository<UserFavCategory, Long> {
    List<UserFavCategory> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
