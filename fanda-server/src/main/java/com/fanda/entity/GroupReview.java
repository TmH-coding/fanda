package com.fanda.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拼饭活动评价
 */
@Data
@TableName("fd_group_review")
public class GroupReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long userId;

    /** 评分 1-5 */
    private Integer rating;

    /** 评价文字 */
    private String content;

    /** 用户昵称（冗余，避免联表） */
    private String username;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
