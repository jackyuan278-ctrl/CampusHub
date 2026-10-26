package com.campushub.community.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 问题分页查询：keyword 标题模糊、tagId 标签过滤、sort=hot|new
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionPageQuery extends PageQuery {

    private String keyword;

    private Long tagId;

    /** hot=按热度排序 new=按时间排序 */
    private String sort = "new";
}





