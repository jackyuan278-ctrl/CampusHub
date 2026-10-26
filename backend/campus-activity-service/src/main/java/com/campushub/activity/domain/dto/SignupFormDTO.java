package com.campushub.activity.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SignupFormDTO {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;
}





