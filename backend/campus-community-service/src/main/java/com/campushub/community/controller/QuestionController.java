package com.campushub.community.controller;

import com.campushub.common.Result;
import com.campushub.common.interceptor.UserContext;
import com.campushub.community.domain.dto.PageDTO;
import com.campushub.community.domain.dto.QuestionFormDTO;
import com.campushub.community.domain.dto.QuestionPageQuery;
import com.campushub.community.service.IQuestionService;
import com.campushub.community.domain.vo.QuestionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final IQuestionService questionService;

    /** 发布问题（登录） */
    @PostMapping
    public Result<Long> publish(@RequestBody @Valid QuestionFormDTO form) {
        return Result.success(questionService.publish(form, UserContext.getUser()));
    }

    /** 问题分页列表（免登录，keyword/tagId/sort=hot|new） */
    @GetMapping
    public Result<PageDTO<QuestionVO>> page(QuestionPageQuery query) {
        return Result.success(questionService.queryPage(query, UserContext.getUser()));
    }

    /** 热点榜（免登录；ZSet 热度分排序，缓存三大问题深挖区） */
    @GetMapping("/hot")
    public Result<List<QuestionVO>> hot(@RequestParam(defaultValue = "10") Integer top) {
        return Result.success(questionService.queryHot(top));
    }

    /** 问题详情（免登录；带 token 时附带 liked 状态） */
    @GetMapping("/{id}")
    public Result<QuestionVO> detail(@PathVariable("id") Long id) {
        return Result.success(questionService.queryDetail(id, UserContext.getUser()));
    }

    /** 点赞问题（登录） */
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable("id") Long id) {
        questionService.like(id, UserContext.getUser());
        return Result.success();
    }

    /** 取消点赞（登录） */
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable("id") Long id) {
        questionService.unlike(id, UserContext.getUser());
        return Result.success();
    }
}





