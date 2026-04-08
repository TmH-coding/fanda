package com.fanda.repository;

import com.fanda.entity.AchievementUnlock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AchievementUnlockRepository extends JpaRepository<AchievementUnlock, Long> {
    List<AchievementUnlock> findByUserId(Long userId);
    boolean existsByUserIdAndAchievementId(Long userId, String achievementId);
}
