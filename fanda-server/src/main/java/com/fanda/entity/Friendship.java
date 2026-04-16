package com.fanda.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友关系实体
 * status: pending(待确认) / accepted(已接受) / rejected(已拒绝)
 */
@Data
@TableName("fd_friendship")
public class Friendship {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起者 */
    private Long requesterId;

    /** 被请求者 */
    private Long addresseeId;

    /** pending / accepted / rejected */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
