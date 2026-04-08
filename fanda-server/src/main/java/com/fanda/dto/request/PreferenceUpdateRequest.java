package com.fanda.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PreferenceUpdateRequest {
    private Integer spicyLevel;

    private List<String> favCategories;

    private List<String> allergies;

    private List<String> dislike;
}
