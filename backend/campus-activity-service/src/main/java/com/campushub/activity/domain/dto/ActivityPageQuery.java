package com.campushub.activity.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityPageQuery extends PageQuery {

    /** 标题关键词（骨架期走 MySQL LIKE；ES 版走 IActivitySearchService） */
    private String keyword;

    /** 状态筛选：1报名中 2进行中 3已结束 4已取消 */
    private Integer status;

    /**
     * 报名状态筛选（OPEN / NOT_STARTED / CLOSED），与 status 互斥使用。
     * 为什么不在前端筛：列表是后端分页的，前端只筛当前页会让总数和翻页都失真。
     */
    private String signupState;
}





