package com.fanda.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoteRequest {
    @NotNull(message = "候选项ID不能为空")
    private Long candidateId;
}
