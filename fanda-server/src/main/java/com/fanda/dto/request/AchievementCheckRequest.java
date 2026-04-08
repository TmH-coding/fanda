package com.fanda.dto.request;

import lombok.Data;

@Data
public class AchievementCheckRequest {
    private int totalRecords;
    private int streak;
    private int uniqueFoods;
    private int favoriteCount;
    private int breakfastCount;
    private int socialJoined;
}
