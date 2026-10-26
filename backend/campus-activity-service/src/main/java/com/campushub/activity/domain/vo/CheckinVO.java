package com.campushub.activity.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 签到状态：今天是否已签 + 连续天数 + 本月已签日期
 */
@Data
public class CheckinVO {

    /** 今天是否已签到 */
    private Boolean signedToday;

    /** 连续签到天数 */
    private Integer streak;

    /** 本月已签日期列表（yyyy-MM-dd） */
    private List<String> checkinDates;
}





