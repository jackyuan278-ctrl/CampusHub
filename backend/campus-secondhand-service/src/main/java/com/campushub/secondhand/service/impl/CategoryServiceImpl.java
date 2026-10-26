package com.campushub.secondhand.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.secondhand.domain.po.Category;
import com.campushub.secondhand.domain.vo.CategoryVO;
import com.campushub.secondhand.mapper.CategoryMapper;
import com.campushub.secondhand.service.ICategoryService;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    @Override
    public List<CategoryVO> listCategories() {
        List<Category> list = lambdaQuery().orderByAsc(Category::getSort).list();
        List<CategoryVO> categoryVOList = new ArrayList<>();
        for (Category category : list) {
            CategoryVO categoryVO = BeanUtil.copyProperties(category, CategoryVO.class);
            categoryVOList.add(categoryVO);
        }
        return categoryVOList;
    }
}




