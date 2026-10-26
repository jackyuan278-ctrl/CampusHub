package com.campushub.community.controller;

import com.campushub.common.Result;
import com.campushub.common.interceptor.UserContext;
import com.campushub.community.service.IFollowService;
import com.campushub.community.domain.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/follows")
@RequiredArgsConstructor
public class FollowController {

    private final IFollowService followService;

    /** 关注某人（登录） */
    @PostMapping("/{targetUserId}")
    public Result<Void> follow(@PathVariable("targetUserId") Long targetUserId) {
        followService.follow(targetUserId, UserContext.getUser());
        return Result.success();
    }

    /** 取消关注（登录） */
    @DeleteMapping("/{targetUserId}")
    public Result<Void> unfollow(@PathVariable("targetUserId") Long targetUserId) {
        followService.unfollow(targetUserId, UserContext.getUser());
        return Result.success();
    }

    /** 我的关注列表（登录） */
    @GetMapping("/mine")
    public Result<List<UserVO>> mine() {
        return Result.success(followService.queryMine(UserContext.getUser()));
    }
}





