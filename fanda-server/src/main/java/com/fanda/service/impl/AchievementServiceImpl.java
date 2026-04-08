package com.fanda.service.impl;

import com.fanda.dto.request.AchievementCheckRequest;
import com.fanda.entity.AchievementUnlock;
import com.fanda.repository.AchievementUnlockRepository;
import com.fanda.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementUnlockRepository achievementUnlockRepository;

    // Achievement metadata: id -> [name, description, icon]
    private static final Map<String, String[]> ACHIEVEMENT_META = new LinkedHashMap<>();

    static {
        ACHIEVEMENT_META.put("first_record", new String[]{"初次记录", "完成第一次饮食记录", "star"});
        ACHIEVEMENT_META.put("streak_3", new String[]{"连续3天", "连续记录3天", "fire"});
        ACHIEVEMENT_META.put("streak_7", new String[]{"坚持一周", "连续记录7天", "fire"});
        ACHIEVEMENT_META.put("streak_30", new String[]{"月度达人", "连续记录30天", "trophy"});
        ACHIEVEMENT_META.put("budget_master", new String[]{"预算大师", "连续一周不超预算", "coin"});
        ACHIEVEMENT_META.put("explorer_10", new String[]{"美食探索者", "尝试10种不同食物", "compass"});
        ACHIEVEMENT_META.put("explorer_30", new String[]{"美食冒险家", "尝试30种不同食物", "map"});
        ACHIEVEMENT_META.put("collector_20", new String[]{"收藏达人", "收藏20个美食", "heart"});
        ACHIEVEMENT_META.put("early_bird", new String[]{"早餐达人", "记录10次早餐", "sun"});
        ACHIEVEMENT_META.put("social_butterfly", new String[]{"社交蝴蝶", "参与5次拼饭", "people"});
    }

    @Override
    public List<String> getUnlocked(Long userId) {
        return achievementUnlockRepository.findByUserId(userId).stream()
                .map(AchievementUnlock::getAchievementId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<Map<String, Object>> check(AchievementCheckRequest request, Long userId) {
        List<Map<String, Object>> newlyUnlocked = new ArrayList<>();

        Map<String, Boolean> conditions = new LinkedHashMap<>();
        conditions.put("first_record", request.getTotalRecords() >= 1);
        conditions.put("streak_3", request.getStreak() >= 3);
        conditions.put("streak_7", request.getStreak() >= 7);
        conditions.put("streak_30", request.getStreak() >= 30);
        conditions.put("budget_master", false); // Checked externally or via separate logic
        conditions.put("explorer_10", request.getUniqueFoods() >= 10);
        conditions.put("explorer_30", request.getUniqueFoods() >= 30);
        conditions.put("collector_20", request.getFavoriteCount() >= 20);
        conditions.put("early_bird", request.getBreakfastCount() >= 10);
        conditions.put("social_butterfly", request.getSocialJoined() >= 5);

        for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
            String achievementId = entry.getKey();
            boolean met = entry.getValue();

            if (met && !achievementUnlockRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
                AchievementUnlock unlock = new AchievementUnlock();
                unlock.setUserId(userId);
                unlock.setAchievementId(achievementId);
                unlock.setUnlockedAt(LocalDateTime.now());
                achievementUnlockRepository.save(unlock);

                String[] meta = ACHIEVEMENT_META.get(achievementId);
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("id", achievementId);
                info.put("name", meta != null ? meta[0] : achievementId);
                info.put("description", meta != null ? meta[1] : "");
                info.put("icon", meta != null ? meta[2] : "star");
                newlyUnlocked.add(info);
            }
        }

        return newlyUnlocked;
    }
}
