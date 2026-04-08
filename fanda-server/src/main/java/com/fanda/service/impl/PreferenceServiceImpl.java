package com.fanda.service.impl;

import com.fanda.dto.request.PreferenceUpdateRequest;
import com.fanda.entity.*;
import com.fanda.repository.*;
import com.fanda.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreferenceServiceImpl implements PreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final UserFavCategoryRepository userFavCategoryRepository;
    private final UserDislikeRepository userDislikeRepository;
    private final UserFavoriteFoodRepository userFavoriteFoodRepository;

    @Override
    public Map<String, Object> getPreference(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();

        UserPreference pref = userPreferenceRepository.findByUserId(userId).orElse(null);
        result.put("spicyLevel", pref != null ? pref.getSpicyLevel() : 0);

        List<UserFavCategory> favCategories = userFavCategoryRepository.findByUserId(userId);
        result.put("favCategories", favCategories.stream()
                .map(UserFavCategory::getCategory)
                .collect(Collectors.toList()));

        List<UserAllergy> allergies = userAllergyRepository.findByUserId(userId);
        result.put("allergies", allergies.stream()
                .map(UserAllergy::getAllergy)
                .collect(Collectors.toList()));

        List<UserDislike> dislikes = userDislikeRepository.findByUserId(userId);
        result.put("dislike", dislikes.stream()
                .map(UserDislike::getDislike)
                .collect(Collectors.toList()));

        List<UserFavoriteFood> favorites = userFavoriteFoodRepository.findByUserId(userId);
        result.put("favorites", favorites.stream()
                .map(f -> f.getFoodId().toString())
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    @Transactional
    public void updatePreference(PreferenceUpdateRequest request, Long userId) {
        // Update spicy level
        UserPreference pref = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPreference p = new UserPreference();
                    p.setUserId(userId);
                    return p;
                });
        if (request.getSpicyLevel() != null) {
            pref.setSpicyLevel(request.getSpicyLevel());
        }
        userPreferenceRepository.save(pref);

        // Delete and recreate allergies
        if (request.getAllergies() != null) {
            userAllergyRepository.deleteByUserId(userId);
            for (String allergy : request.getAllergies()) {
                UserAllergy ua = new UserAllergy();
                ua.setUserId(userId);
                ua.setAllergy(allergy);
                userAllergyRepository.save(ua);
            }
        }

        // Delete and recreate fav categories
        if (request.getFavCategories() != null) {
            userFavCategoryRepository.deleteByUserId(userId);
            for (String category : request.getFavCategories()) {
                UserFavCategory uc = new UserFavCategory();
                uc.setUserId(userId);
                uc.setCategory(category);
                userFavCategoryRepository.save(uc);
            }
        }

        // Delete and recreate dislikes
        if (request.getDislike() != null) {
            userDislikeRepository.deleteByUserId(userId);
            for (String dislike : request.getDislike()) {
                UserDislike ud = new UserDislike();
                ud.setUserId(userId);
                ud.setDislike(dislike);
                userDislikeRepository.save(ud);
            }
        }
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long userId, Long foodId) {
        if (userFavoriteFoodRepository.existsByUserIdAndFoodId(userId, foodId)) {
            userFavoriteFoodRepository.deleteByUserIdAndFoodId(userId, foodId);
            return false;
        } else {
            UserFavoriteFood fav = new UserFavoriteFood();
            fav.setUserId(userId);
            fav.setFoodId(foodId);
            userFavoriteFoodRepository.save(fav);
            return true;
        }
    }
}
