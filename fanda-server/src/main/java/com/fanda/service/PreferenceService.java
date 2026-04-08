package com.fanda.service;

import com.fanda.dto.request.PreferenceUpdateRequest;

import java.util.Map;

public interface PreferenceService {
    Map<String, Object> getPreference(Long userId);
    void updatePreference(PreferenceUpdateRequest request, Long userId);
    boolean toggleFavorite(Long userId, Long foodId);
}
