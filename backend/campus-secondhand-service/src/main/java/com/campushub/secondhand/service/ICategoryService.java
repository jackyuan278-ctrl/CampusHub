package com.campushub.secondhand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.secondhand.domain.po.Category;
import com.campushub.secondhand.domain.vo.CategoryVO;

import java.util.List;

public interface ICategoryService extends IService<Category> {

    List<CategoryVO> listCategories();
}




