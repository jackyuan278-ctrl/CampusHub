package com.campushub.community.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.api.dto.UserBriefDTO;
import com.campushub.common.BizException;
import com.campushub.community.client.UserLookup;
import com.campushub.community.domain.po.Follow;
import com.campushub.community.mapper.FollowMapper;
import com.campushub.community.service.IFollowService;
import com.campushub.community.domain.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    private final UserLookup userLookup;

    @Override
    public void follow(Long targetUserId, Long userId) {
        if (targetUserId.equals(userId)) {
            throw new BizException(400,"不能关注自己");
        }
        boolean exists = lambdaQuery().eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, targetUserId).exists();
        if (exists) {
            throw new BizException(400,"请勿重复关注");
        }
        Follow follow = new Follow();
        follow.setFollowUserId(targetUserId);
        follow.setUserId(userId);
        follow.setCreateTime(LocalDateTime.now());
        save(follow);
    }

    @Override
    public void unfollow(Long targetUserId, Long userId) {
        remove(Wrappers.<Follow>lambdaQuery()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, targetUserId));
    }

    @Override
    public List<UserVO> queryMine(Long userId) {
        List<Follow> list = this.lambdaQuery().eq(Follow::getUserId, userId).orderByDesc(Follow::getCreateTime).list();
        ArrayList<Long> ids = new ArrayList<>(list.size());
        for (Follow follow : list){
            Long followUserId = follow.getFollowUserId();
            ids.add(followUserId);
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        // 关注列表可能很长，超过批量上限的尾部用户查不到；
        // 这时保留条目、把昵称头像留空交给前端兜底，不能让整页关注凭空消失
        Map<Long, UserBriefDTO> userMap = userLookup.findByIds(ids);
        List<UserVO> userVOS = new ArrayList<>(ids.size());
        for (Long id : ids) {
            UserBriefDTO user = userMap.get(id);
            UserVO vo = user == null ? new UserVO() : BeanUtil.copyProperties(user, UserVO.class);
            // 权威库 tb_user 没有 role 列，UserVO.role 不再有值，前端也不读
            vo.setId(id);
            userVOS.add(vo);
        }
        return userVOS;
    }
}





