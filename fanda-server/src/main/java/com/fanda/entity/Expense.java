package com.fanda.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("fd_expense")
public class Expense {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate expenseDate;

    private BigDecimal amount;

    private String mealType;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
