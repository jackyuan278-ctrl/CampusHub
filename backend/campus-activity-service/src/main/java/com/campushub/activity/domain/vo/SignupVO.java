package com.campushub.activity.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SignupVO {

    private Long id;

    private Long activityId;

    private String activityTitle;

    private String coverUrl;

    private String location;

    private LocalDateTime startTime;

    /** 1已报名 2候补中 3已取消 */
    private Integer status;

    /** 候补排队序号（仅候补中时有效，按 Redis waitlist 顺序算） */
    private Integer waitPosition;

    private LocalDateTime createTime;
}





