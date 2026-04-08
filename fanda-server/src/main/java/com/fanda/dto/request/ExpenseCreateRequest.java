package com.fanda.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseCreateRequest {
    @NotBlank(message = "日期不能为空")
    private String date;

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @NotBlank(message = "餐次不能为空")
    private String mealType;

    private String description;
}
