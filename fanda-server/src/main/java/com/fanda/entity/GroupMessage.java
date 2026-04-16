package com.fanda.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拼饭组留言
 */
@Data
@TableName("fd_group_message")
public class GroupMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long userId;

    /** 发言人昵称（冗余，避免联表） */
    private String username;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
