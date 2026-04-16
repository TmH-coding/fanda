package com.fanda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.dto.request.PreferenceUpdateRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.*;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.mapper.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    private final UserPreferenceMapper userPreferenceMapper;
    private final UserFavCategoryMapper userFavCategoryMapper;
    private final UserAllergyMapper userAllergyMapper;
    private final UserDislikeMapper userDislikeMapper;
    private final UserFavoriteFoodMapper userFavoriteFoodMapper;
    private final UserMapper userMapper;

    @GetMapping
    @Cacheable(value = "userPrefs", key = "#authentication.name")
    public ApiResponse<Map<String, Object>> get(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        UserPreference pref = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId));

        List<String> favCategories = userFavCategoryMapper.selectList(
                new LambdaQueryWrapper<UserFavCategory>().eq(UserFavCategory::getUserId, userId))
                .stream().map(UserFavCategory::getCategory).collect(Collectors.toList());

        List<String> allergies = userAllergyMapper.selectList(
                new LambdaQueryWrapper<UserAllergy>().eq(UserAllergy::getUserId, userId))
                .stream().map(UserAllergy::getAllergy).collect(Collectors.toList());

        List<String> dislike = userDislikeMapper.selectList(
                new LambdaQueryWrapper<UserDislike>().eq(UserDislike::getUserId, userId))
                .stream().map(UserDislike::getDislike).collect(Collectors.toList());

        List<Long> favorites = userFavoriteFoodMapper.selectList(
                new LambdaQueryWrapper<UserFavoriteFood>().eq(UserFavoriteFood::getUserId, userId))
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
    @CacheEvict(value = "userPrefs", key = "#authentication.name")
    public ApiResponse<Void> update(
            @RequestBody @Valid PreferenceUpdateRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        if (request.getSpicyLevel() != null) {
            UserPreference pref = userPreferenceMapper.selectOne(
                    new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId));
            if (pref == null) {
                pref = new UserPreference();
                pref.setUserId(userId);
                pref.setSpicyLevel(request.getSpicyLevel());
                userPreferenceMapper.insert(pref);
            } else {
                pref.setSpicyLevel(request.getSpicyLevel());
                userPreferenceMapper.updateById(pref);
            }
        }

        if (request.getFavCategories() != null) {
            userFavCategoryMapper.delete(
                    new LambdaQueryWrapper<UserFavCategory>().eq(UserFavCategory::getUserId, userId));
            for (String cat : request.getFavCategories()) {
                UserFavCategory fc = new UserFavCategory();
                fc.setUserId(userId);
                fc.setCategory(cat);
                userFavCategoryMapper.insert(fc);
            }
        }

        if (request.getAllergies() != null) {
            userAllergyMapper.delete(
                    new LambdaQueryWrapper<UserAllergy>().eq(UserAllergy::getUserId, userId));
            for (String allergy : request.getAllergies()) {
                UserAllergy ua = new UserAllergy();
                ua.setUserId(userId);
                ua.setAllergy(allergy);
                userAllergyMapper.insert(ua);
            }
        }

        if (request.getDislike() != null) {
            userDislikeMapper.delete(
                    new LambdaQueryWrapper<UserDislike>().eq(UserDislike::getUserId, userId));
            for (String d : request.getDislike()) {
                UserDislike ud = new UserDislike();
                ud.setUserId(userId);
                ud.setDislike(d);
                userDislikeMapper.insert(ud);
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

        Long count = userFavoriteFoodMapper.selectCount(new LambdaQueryWrapper<UserFavoriteFood>()
                .eq(UserFavoriteFood::getUserId, userId)
                .eq(UserFavoriteFood::getFoodId, foodId));
        boolean exists = count > 0;

        if (exists) {
            userFavoriteFoodMapper.delete(new LambdaQueryWrapper<UserFavoriteFood>()
                    .eq(UserFavoriteFood::getUserId, userId)
                    .eq(UserFavoriteFood::getFoodId, foodId));
        } else {
            UserFavoriteFood fav = new UserFavoriteFood();
            fav.setUserId(userId);
            fav.setFoodId(foodId);
            userFavoriteFoodMapper.insert(fav);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("favorited", !exists);
        return ApiResponse.ok(result);
    }

    private Long getCurrentUserId(Authentication auth) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return user.getId();
    }
}
