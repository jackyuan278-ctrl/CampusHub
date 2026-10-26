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
}





