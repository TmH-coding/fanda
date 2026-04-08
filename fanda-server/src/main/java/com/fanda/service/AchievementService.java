package com.fanda.service;

import com.fanda.dto.request.AchievementCheckRequest;

import java.util.List;
import java.util.Map;

public interface AchievementService {
    List<String> getUnlocked(Long userId);
    List<Map<String, Object>> check(AchievementCheckRequest request, Long userId);
}
