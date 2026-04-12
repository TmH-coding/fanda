package com.fanda.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RecordCreateRequest {
    @NotBlank(message = "日期不能为空")
    private String date;

    @NotBlank(message = "餐次不能为空")
    private String mealType;

    @NotBlank(message = "食物名称不能为空")
    private String foodName;

    private String foodCode;

    @NotNull(message = "花费不能为空")
    private BigDecimal cost;

    private List<String> nutrition;
}
