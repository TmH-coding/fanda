package com.fanda.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SocialGroupCreateRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "时间不能为空")
    private String time;

    @NotBlank(message = "地点不能为空")
    private String location;

    private Integer maxPeople;

    private List<String> candidates;

    private List<String> tags;
}
