package com.campushub.common.domain;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 分页查询参数，参数名与前端契约一致（page/pageSize）
 */
@Data
public class PageQuery {

    public static final int MIN_PAGE_NUMBER = 1;
    public static final int MIN_PAGE_SIZE = 1;
    public static final int DEFAULT_PAGE = MIN_PAGE_NUMBER;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;

    private Integer page = DEFAULT_PAGE;
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    public <T> Page<T> toMpPage() {
        int pageNo = page == null || page < MIN_PAGE_NUMBER ? DEFAULT_PAGE : page;
        int size = pageSize == null || pageSize < MIN_PAGE_SIZE
                ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return new Page<>(pageNo, size);
    }
}




