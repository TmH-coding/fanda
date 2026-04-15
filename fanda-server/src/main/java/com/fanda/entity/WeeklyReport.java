package com.fanda.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fd_weekly_report")
public class WeeklyReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 报告所在的周一日期（作为唯一标识本周的key）
    private LocalDate weekStart;

    // AI 生成的营养报告内容
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
