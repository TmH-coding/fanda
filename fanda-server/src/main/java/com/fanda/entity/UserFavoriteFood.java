package com.fanda.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("fd_user_favorite_food")
public class UserFavoriteFood {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long foodId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
