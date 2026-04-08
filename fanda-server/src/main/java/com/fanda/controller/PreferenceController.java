package com.fanda.controller;

import com.fanda.dto.request.PreferenceUpdateRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.*;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserFavCategoryRepository userFavCategoryRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final UserDislikeRepository userDislikeRepository;
    private final UserFavoriteFoodRepository userFavoriteFoodRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> get(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        UserPreference pref = userPreferenceRepository.findByUserId(userId).orElse(null);

        List<String> favCategories = userFavCategoryRepository.findByUserId(userId)
                .stream().map(UserFavCategory::getCategory).collect(Collectors.toList());

        List<String> allergies = userAllergyRepository.findByUserId(userId)
                .stream().map(UserAllergy::getAllergy).collect(Collectors.toList());

        List<String> dislike = userDislikeRepository.findByUserId(userId)
                .stream().map(UserDislike::getDislike).collect(Collectors.toList());

        List<Long> favorites = userFavoriteFoodRepository.findByUserId(userId)
                .stream().map(UserFavoriteFood::getFoodId).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("spicyLevel", pref != null ? pref.getSpicyLevel() : 0);
        result.put("favCategories", favCategories);
        result.put("allergies", allergies);
        result.put("dislike", dislike);
        result.put("favorites", favorites);

        return ApiResponse.ok(result);
    }

    @PutMapping
    @Transactional
    public ApiResponse<Void> update(
            @RequestBody @Valid PreferenceUpdateRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        // Update spicy level
        if (request.getSpicyLevel() != null) {
            UserPreference pref = userPreferenceRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        UserPreference p = new UserPreference();
                        p.setUserId(userId);
                        return p;
                    });
            pref.setSpicyLevel(request.getSpicyLevel());
            userPreferenceRepository.save(pref);
        }

        // Update favorite categories
        if (request.getFavCategories() != null) {
            userFavCategoryRepository.deleteByUserId(userId);
            for (String cat : request.getFavCategories()) {
                UserFavCategory fc = new UserFavCategory();
                fc.setUserId(userId);
                fc.setCategory(cat);
                userFavCategoryRepository.save(fc);
            }
        }

        // Update allergies
        if (request.getAllergies() != null) {
            userAllergyRepository.deleteByUserId(userId);
            for (String allergy : request.getAllergies()) {
                UserAllergy ua = new UserAllergy();
                ua.setUserId(userId);
                ua.setAllergy(allergy);
                userAllergyRepository.save(ua);
            }
        }

        // Update dislikes
        if (request.getDislike() != null) {
            userDislikeRepository.deleteByUserId(userId);
            for (String d : request.getDislike()) {
                UserDislike ud = new UserDislike();
                ud.setUserId(userId);
                ud.setDislike(d);
                userDislikeRepository.save(ud);
            }
        }

        return ApiResponse.ok();
    }

    @PostMapping("/favorite/{foodId}")
    @Transactional
    public ApiResponse<Map<String, Object>> toggleFavorite(
            @PathVariable Long foodId,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        boolean exists = userFavoriteFoodRepository.existsByUserIdAndFoodId(userId, foodId);
        if (exists) {
            userFavoriteFoodRepository.deleteByUserIdAndFoodId(userId, foodId);
        } else {
            UserFavoriteFood fav = new UserFavoriteFood();
            fav.setUserId(userId);
            fav.setFoodId(foodId);
            userFavoriteFoodRepository.save(fav);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("favorited", !exists);
        return ApiResponse.ok(result);
    }

    private Long getCurrentUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .getId();
    }
}
