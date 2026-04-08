package com.fanda.controller;

import com.fanda.dto.request.AchievementCheckRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.AchievementUnlock;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.repository.AchievementUnlockRepository;
import com.fanda.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementUnlockRepository achievementUnlockRepository;
    private final UserRepository userRepository;

    // Achievement definitions
    private static final List<Map<String, Object>> ACHIEVEMENT_DEFS = List.of(
            Map.of("id", "first_meal", "name", "初次记录", "description", "记录第一顿饭", "icon", "utensils", "condition", "totalRecords", "threshold", 1),
            Map.of("id", "week_streak", "name", "坚持一周", "description", "连续记录7天", "icon", "fire", "condition", "streak", "threshold", 7),
            Map.of("id", "food_explorer", "name", "美食探索家", "description", "尝试10种不同食物", "icon", "compass", "condition", "uniqueFoods", "threshold", 10),
            Map.of("id", "favorite_5", "name", "收藏达人", "description", "收藏5种食物", "icon", "heart", "condition", "favoriteCount", "threshold", 5),
            Map.of("id", "early_bird", "name", "早餐达人", "description", "记录10次早餐", "icon", "sunrise", "condition", "breakfastCount", "threshold", 10),
            Map.of("id", "social_butterfly", "name", "社交达人", "description", "参加3次拼饭", "icon", "users", "condition", "socialJoined", "threshold", 3),
            Map.of("id", "hundred_meals", "name", "百餐纪念", "description", "记录100顿饭", "icon", "award", "condition", "totalRecords", "threshold", 100)
    );

    @GetMapping
    public ApiResponse<Map<String, Object>> list(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        List<String> unlocked = achievementUnlockRepository.findByUserId(userId)
                .stream()
                .map(AchievementUnlock::getAchievementId)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("unlocked", unlocked);
        return ApiResponse.ok(result);
    }

    @PostMapping("/check")
    @Transactional
    public ApiResponse<List<Map<String, Object>>> check(
            @RequestBody @Valid AchievementCheckRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        List<Map<String, Object>> newlyUnlocked = new ArrayList<>();

        for (Map<String, Object> def : ACHIEVEMENT_DEFS) {
            String achievementId = (String) def.get("id");

            // Skip if already unlocked
            if (achievementUnlockRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
                continue;
            }

            String condition = (String) def.get("condition");
            int threshold = (int) def.get("threshold");
            int currentValue = getValueForCondition(request, condition);

            if (currentValue >= threshold) {
                AchievementUnlock unlock = new AchievementUnlock();
                unlock.setUserId(userId);
                unlock.setAchievementId(achievementId);
                unlock.setUnlockedAt(LocalDateTime.now());
                achievementUnlockRepository.save(unlock);

                Map<String, Object> unlockInfo = new HashMap<>();
                unlockInfo.put("id", achievementId);
                unlockInfo.put("name", def.get("name"));
                unlockInfo.put("description", def.get("description"));
                unlockInfo.put("icon", def.get("icon"));
                newlyUnlocked.add(unlockInfo);
            }
        }

        return ApiResponse.ok(newlyUnlocked);
    }

    private int getValueForCondition(AchievementCheckRequest request, String condition) {
        return switch (condition) {
            case "totalRecords" -> request.getTotalRecords();
            case "streak" -> request.getStreak();
            case "uniqueFoods" -> request.getUniqueFoods();
            case "favoriteCount" -> request.getFavoriteCount();
            case "breakfastCount" -> request.getBreakfastCount();
            case "socialJoined" -> request.getSocialJoined();
            default -> 0;
        };
    }

    private Long getCurrentUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .getId();
    }
}
