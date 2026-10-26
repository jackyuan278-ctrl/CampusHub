package com.campushub.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.community.domain.po.Follow;
import com.campushub.community.domain.vo.UserVO;

import java.util.List;

/**
 * 关注服务：关注/取关 + 我的关注列表。
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注：不能关注自己；tb_follow 唯一键 (user_id, follow_user_id) 兜底，重复关注抛异常
     */
    void follow(Long targetUserId, Long userId);

    /**
     * 取关：未关注静默成功（幂等）
     */
    void unfollow(Long targetUserId, Long userId);

    /**
     * 我的关注列表：批量查用户信息（selectBatchIds 防 N+1），组装 UserVO
     */
    List<UserVO> queryMine(Long userId);
}





