package com.campushub.secondhand.controller;

import com.campushub.common.Result;
import com.campushub.secondhand.domain.vo.CategoryVO;
import com.campushub.secondhand.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/items/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService categoryService;

    /** 免鉴权（网关白名单：GET /api/items/**） */
    @GetMapping
    public Result<List<CategoryVO>> listCategories() {
        return Result.success(categoryService.listCategories());
    }
}




