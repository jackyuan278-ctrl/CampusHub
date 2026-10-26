package com.campushub.activity.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityFormDTO {

    @NotBlank(message = "活动标题不能为空")
    private String title;

    @NotBlank(message = "活动介绍不能为空")
    private String description;

    private String coverUrl;

    @NotBlank(message = "活动地点不能为空")
    private String location;

    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "活动结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "报名开始时间不能为空")
    private LocalDateTime signupStartTime;

    @NotNull(message = "报名截止时间不能为空")
    private LocalDateTime signupEndTime;

    @NotNull(message = "总名额不能为空")
    @Min(value = 1, message = "名额至少 1 个")
    private Integer quota;
}





