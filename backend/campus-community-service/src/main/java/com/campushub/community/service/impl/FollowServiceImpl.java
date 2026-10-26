package com.campushub.community.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.common.BizException;
import com.campushub.community.domain.po.Follow;
import com.campushub.community.domain.po.User;
import com.campushub.community.mapper.FollowMapper;
import com.campushub.community.mapper.UserMapper;
import com.campushub.community.service.IFollowService;
import com.campushub.community.domain.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    private final UserMapper userMapper;

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
        List<User> users = userMapper.selectByIds(ids);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<UserVO> userVOS = new ArrayList<>(ids.size());
        for (Long id : ids) {
            User user = userMap.get(id);
            if (user != null) {
                userVOS.add(BeanUtil.copyProperties(user, UserVO.class));
            }
        }
        return userVOS;
    }
}





