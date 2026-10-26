package com.campushub.activity.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityVO {

    private Long id;

    private String title;

    private String description;

    private String coverUrl;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime signupStartTime;

    private LocalDateTime signupEndTime;

    private Integer quota;

    /** 1报名中 2进行中 3已结束 4已取消 */
    private Integer status;

    /** 剩余名额（Redis signup:stock:{id}，未初始化时视为 quota） */
    private Integer remainingQuota;

    /** 当前用户报名状态：null未报名 1已报名 2候补中（未登录恒为 null） */
    private Integer mySignupStatus;
}





