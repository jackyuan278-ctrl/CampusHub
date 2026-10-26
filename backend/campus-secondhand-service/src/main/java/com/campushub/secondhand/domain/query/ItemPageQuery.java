package com.campushub.secondhand.domain.query;

import com.campushub.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分页查询参数，字段名与前端契约一致
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ItemPageQuery extends PageQuery {

    private String keyword;

    private Long categoryId;

    /** '' 综合 / sales 销量 / priceAsc 价格升序 / priceDesc 价格降序 */
    private String sort;

    /** 按 id 游标继续取数；仅支持默认排序。 */
    private Long cursorId;

    /** 当前排序字段的游标值（销量或价格）；默认排序不需要。 */
    private Integer cursorSortValue;
}




