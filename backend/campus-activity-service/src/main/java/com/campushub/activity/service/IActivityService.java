package com.campushub.activity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.activity.domain.dto.ActivityFormDTO;
import com.campushub.activity.domain.dto.ActivityPageQuery;
import com.campushub.activity.domain.po.Activity;
import com.campushub.activity.domain.vo.ActivityVO;
import com.campushub.activity.domain.dto.PageDTO;

public interface IActivityService extends IService<Activity> {

    /**
     * 发布活动：校验时间区间合理（报名截止 <= 活动开始等）→ 入库 status=1
     * → 初始化 Redis signup:stock:{id}=quota，返回活动ID
     */
    Long publish(ActivityFormDTO form, Long publisherId);

    /**
     * 分页查询：keyword 走 MySQL LIKE、status 精确匹配；报名中的活动附 remainingQuota
     */
    PageDTO<ActivityVO> queryActivityPage(ActivityPageQuery query);

    /**
     * 详情：查库 + Redis 算 remainingQuota；userId 非空时补 mySignupStatus
     */
    ActivityVO queryActivityById(Long id, Long userId);

    /**
     * 发布人取消活动：校验归属 → status=4；已报名用户如何处理（退名额/通知）由你设计
     */
    void cancelActivity(Long id, Long publisherId);

    void saveDoc(Activity activity);

    void deleteDoc(Long id);
}





