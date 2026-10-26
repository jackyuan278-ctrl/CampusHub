package com.campushub.activity.service;

import com.campushub.activity.es.ActivityDoc;
import com.campushub.activity.domain.dto.PageDTO;

public interface IActivitySearchService {

    /**
     * ES 关键词搜索（IK 分词 + 高亮），由你实现（面试深挖区）；
     * ActivityDoc 的写入/同步（发布活动时写 ES、取消时删）也在此模块内设计
     */
    PageDTO<ActivityDoc> search(String keyword, int page, int pageSize);
}





