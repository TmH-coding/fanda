package com.fanda.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("fd_social_group")
public class SocialGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private String creatorName;

    private String avatar;

    private String title;

    private String mealTime;

    private String location;

    private Integer maxPeople;

    private Integer currentPeople;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<SocialTag> tags = new ArrayList<>();

    @TableField(exist = false)
    private List<SocialCandidate> candidates = new ArrayList<>();

    @TableField(exist = false)
    private List<SocialMember> members = new ArrayList<>();
}
