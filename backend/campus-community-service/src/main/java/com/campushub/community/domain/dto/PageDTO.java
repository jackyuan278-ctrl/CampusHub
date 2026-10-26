package com.campushub.community.domain.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应结构，字段与前端契约保持一致（{list, total}）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {

    private Long total;
    private List<T> list;

    public static <T> PageDTO<T> of(Page<T> page) {
        if (page == null || page.getRecords() == null) {
            return new PageDTO<>(page == null ? 0L : page.getTotal(), new ArrayList<>());
        }
        return new PageDTO<>(page.getTotal(), page.getRecords());
    }

    public static <T, R> PageDTO<T> of(Page<R> page, Function<R, T> converter) {
        if (page == null || page.getRecords() == null) {
            return new PageDTO<>(page == null ? 0L : page.getTotal(), new ArrayList<>());
        }
        List<T> list = page.getRecords().stream().map(converter).collect(Collectors.toList());
        return new PageDTO<>(page.getTotal(), list);
    }
}





