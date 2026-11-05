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

    /**
     * 报名状态（由 status + 报名时间窗口**派生**，不落库）：
     * OPEN 可报名 / NOT_STARTED 报名未开始 / CLOSED 报名已截止 /
     * ONGOING 活动进行中 / ENDED 已结束 / CANCELLED 已取消
     *
     * 为什么单独给一个字段：status=1 只代表「活动已发布」，报名是否开放还要看
     * signup_start_time / signup_end_time。前端只按 status 显示会把「报名未开始」
     * 标成「报名中」，用户点报名又被后端拒绝。派生出来统一口径，避免各端各算一遍。
     */
    private String signupState;

    /** 剩余名额（Redis signup:stock:{id}，未初始化时视为 quota） */
    private Integer remainingQuota;

    /** 当前用户报名状态：null未报名 1已报名 2候补中（未登录恒为 null） */
    private Integer mySignupStatus;
}





