package com.fanda.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetUpdateRequest {
    @NotNull(message = "月预算不能为空")
    private BigDecimal monthly;

    @NotNull(message = "周预算不能为空")
    private BigDecimal weekly;
}
