package com.campushub.community.controller;

import com.campushub.common.Result;
import com.campushub.community.service.ITagService;
import com.campushub.community.domain.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final ITagService tagService;

    /** 全部标签（免登录，供提问/筛选使用） */
    @GetMapping
    public Result<List<TagVO>> list() {
        return Result.success(tagService.listAll());
    }
}





