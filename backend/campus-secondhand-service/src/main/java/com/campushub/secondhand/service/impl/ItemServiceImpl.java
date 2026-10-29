package com.campushub.secondhand.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.api.dto.OrderDetailDTO;
import com.campushub.common.BizException;
import com.campushub.common.domain.PageDTO;
import com.campushub.common.domain.PageQuery;
import com.campushub.secondhand.domain.po.Category;
import com.campushub.secondhand.domain.po.Item;
import com.campushub.secondhand.domain.query.ItemPageQuery;
import com.campushub.secondhand.domain.vo.ItemVO;
import com.campushub.secondhand.enums.ItemStatus;
import com.campushub.secondhand.mapper.CategoryMapper;
import com.campushub.secondhand.mapper.ItemMapper;
import com.campushub.secondhand.service.IItemService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {
    private static final long MIN_VALID_CATEGORY_ID = 0L;
    private static final long MIN_VALID_CURSOR_ID = 0L;
    private static final int MIN_VALID_QUANTITY = 0;
    private static final int MAX_OFFSET_PAGE = 5;
    private static final long CURSOR_LOOKAHEAD = 1L;
    private static final int LAST_RECORD_INDEX_OFFSET = 1;

    private static final String DEFAULT_SORT = "";
    private static final String SORT_SALES = "sales";
    private static final String SORT_PRICE_ASC = "priceAsc";
    private static final String SORT_PRICE_DESC = "priceDesc";

    private static final int HOT_ITEM_LIMIT = 8;
    private static final long HOT_CACHE_TTL_MINUTES = 5L;
    private static final String HOT_ITEMS_KEY = "campushub:item:hot:" + HOT_ITEM_LIMIT;

    private static final String CATEGORY_INVALID_MESSAGE = "商品分类不合法";
    private static final String PAGE_REQUIRES_CURSOR_MESSAGE = "第%d页起请使用游标分页";
    private static final String CURSOR_INVALID_MESSAGE = "分页游标不合法";
    private static final String CURSOR_PAGE_CONFLICT_MESSAGE = "游标分页不能同时指定页码";
    private static final String UNSUPPORTED_SORT_MESSAGE = "不支持的商品排序方式";
    private static final String DEFAULT_SORT_CURSOR_MESSAGE = "默认排序不需要排序游标值";
    private static final String SORT_CURSOR_REQUIRED_MESSAGE = "当前排序缺少排序游标值";
    private static final String ITEM_NOT_FOUND_MESSAGE = "商品不存在";
    private static final String STOCK_NOT_ENOUGH_MESSAGE = "商品库存不足";
    private static final String DETAILS_EMPTY_MESSAGE = "商品明细不能为空";
    private static final String ITEM_DETAIL_INVALID_MESSAGE = "商品信息不完整";
    private static final String QUANTITY_INVALID_MESSAGE = "商品数量必须大于0";
    private static final String HOT_CACHE_READ_FAILED_MESSAGE = "读取热卖商品缓存失败，回源数据库";
    private static final String HOT_CACHE_WRITE_FAILED_MESSAGE = "写入热卖商品缓存失败";

    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    @Override
    public PageDTO<ItemVO> queryItemPage(ItemPageQuery query) {
        LambdaQueryWrapper<Item> lq = new QueryWrapper<Item>().lambda();
        lq.eq(Item::getStatus, ItemStatus.ON_SHELF.getValue());
        if (StrUtil.isNotBlank(query.getKeyword())) {
            lq.like(Item::getName, query.getKeyword());
        }
        if (query.getCategoryId() != null && query.getCategoryId() <= MIN_VALID_CATEGORY_ID) {
            throw new BizException(BizException.BAD_REQUEST_CODE, CATEGORY_INVALID_MESSAGE);
        }
        if (query.getCategoryId() != null) {
            lq.eq(Item::getCategoryId, query.getCategoryId());
        }

        boolean cursorMode = query.getCursorId() != null;
        if (query.getPage() != null && query.getPage() > MAX_OFFSET_PAGE && !cursorMode) {
            throw new BizException(BizException.BAD_REQUEST_CODE,
                    String.format(PAGE_REQUIRES_CURSOR_MESSAGE, MAX_OFFSET_PAGE + CURSOR_LOOKAHEAD));
        }
        if (cursorMode && query.getCursorId() <= MIN_VALID_CURSOR_ID) {
            throw new BizException(BizException.BAD_REQUEST_CODE, CURSOR_INVALID_MESSAGE);
        }
        if (cursorMode && query.getPage() != null && query.getPage() != PageQuery.DEFAULT_PAGE) {
            throw new BizException(BizException.BAD_REQUEST_CODE, CURSOR_PAGE_CONFLICT_MESSAGE);
        }

        String sort = StrUtil.blankToDefault(query.getSort(), DEFAULT_SORT);
        switch (sort) {
            case SORT_SALES:
                lq.orderByDesc(Item::getCreateTime).orderByAsc(Item::getId);
                break;
            case SORT_PRICE_ASC:
                lq.orderByAsc(Item::getPrice).orderByAsc(Item::getId);
                break;
            case SORT_PRICE_DESC:
                lq.orderByDesc(Item::getPrice).orderByAsc(Item::getId);
                break;
            case DEFAULT_SORT:
                lq.orderByAsc(Item::getId);
                break;
            default:
                throw new BizException(BizException.BAD_REQUEST_CODE, UNSUPPORTED_SORT_MESSAGE);
        }

        if (cursorMode) {
            if (DEFAULT_SORT.equals(sort)) {
                if (query.getCursorSortValue() != null) {
                    throw new BizException(BizException.BAD_REQUEST_CODE, DEFAULT_SORT_CURSOR_MESSAGE);
                }
            } else if (query.getCursorSortValue() == null) {
                throw new BizException(BizException.BAD_REQUEST_CODE, SORT_CURSOR_REQUIRED_MESSAGE);
            }
            switch (sort) {
                case SORT_PRICE_ASC:
                    lq.and(w -> w.gt(Item::getPrice, query.getCursorSortValue())
                            .or().eq(Item::getPrice, query.getCursorSortValue()).gt(Item::getId, query.getCursorId()));
                    break;
                case SORT_PRICE_DESC:
                    lq.and(w -> w.lt(Item::getPrice, query.getCursorSortValue())
                            .or().eq(Item::getPrice, query.getCursorSortValue()).gt(Item::getId, query.getCursorId()));
                    break;
                default:
                    lq.gt(Item::getId, query.getCursorId());
            }
        }

        Page<Item> page = cursorMode
                ? new Page<>(PageQuery.DEFAULT_PAGE, normalizedPageSize(query.getPageSize()) + CURSOR_LOOKAHEAD, false)
                : query.toMpPage();
        Page<Item> result = this.page(page, lq);
        boolean hasMore = cursorMode && result.getRecords().size() > normalizedPageSize(query.getPageSize());
        if (hasMore) {
            result.getRecords().remove(result.getRecords().size() - LAST_RECORD_INDEX_OFFSET);
        }

        List<Long> categoryIds = result.getRecords().stream()
                .map(Item::getCategoryId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, c -> c));

        List<ItemVO> itemVoList = new ArrayList<>();
        for (Item item : result.getRecords()) {
            ItemVO itemVO = BeanUtil.copyProperties(item, ItemVO.class);
            Category category = categoryMap.get(item.getCategoryId());
            if (category != null) {
                itemVO.setCategoryName(category.getName());
            }
            itemVoList.add(itemVO);
        }
        PageDTO<ItemVO> pageDTO = new PageDTO<>();
        pageDTO.setTotal(cursorMode ? null : result.getTotal());
        pageDTO.setList(itemVoList);
        if (cursorMode) {
            pageDTO.setHasMore(hasMore);
            Item lastItem = result.getRecords().isEmpty() ? null : result.getRecords().get(result.getRecords().size() - LAST_RECORD_INDEX_OFFSET);
            pageDTO.setNextCursor(lastItem == null ? null : lastItem.getId());
            pageDTO.setNextCursorSortValue(lastItem == null || DEFAULT_SORT.equals(sort) ? null
                    : SORT_PRICE_ASC.equals(sort) || SORT_PRICE_DESC.equals(sort) ? lastItem.getPrice() : null);
        }

        return pageDTO;
    }

    @Override
    public List<ItemVO> queryHotItems() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(HOT_ITEMS_KEY);
            if (StrUtil.isNotBlank(cached)) {
                return JSONUtil.toList(cached, ItemVO.class);
            }
        } catch (Exception e) {
            log.warn(HOT_CACHE_READ_FAILED_MESSAGE, e);
        }

        List<Item> items = lambdaQuery()
                .eq(Item::getStatus, ItemStatus.ON_SHELF.getValue())
                .orderByDesc(Item::getCreateTime)
                .orderByAsc(Item::getId)
                .last("LIMIT " + HOT_ITEM_LIMIT)
                .list();
        List<ItemVO> hotItems = toItemVOList(items);
        try {
            stringRedisTemplate.opsForValue().set(HOT_ITEMS_KEY, JSONUtil.toJsonStr(hotItems), HOT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn(HOT_CACHE_WRITE_FAILED_MESSAGE, e);
        }
        return hotItems;
    }

    private List<ItemVO> toItemVOList(List<Item> items) {
        List<Long> categoryIds = items.stream().map(Item::getCategoryId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream().collect(Collectors.toMap(Category::getId, c -> c));
        List<ItemVO> result = new ArrayList<>();
        for (Item item : items) {
            ItemVO vo = BeanUtil.copyProperties(item, ItemVO.class);
            Category category = categoryMap.get(item.getCategoryId());
            if (category != null) vo.setCategoryName(category.getName());
            result.add(vo);
        }
        return result;
    }

    @Override
    public ItemVO queryItemById(Long id) {
        Item item = getById(id);
        if (item == null) {
            throw new BizException(BizException.BAD_REQUEST_CODE, ITEM_NOT_FOUND_MESSAGE);
        }
        ItemVO itemVO = BeanUtil.copyProperties(item, ItemVO.class);
        Long categoryId = item.getCategoryId();
        Category category = categoryMapper.selectById(categoryId);
        if (category != null) {
            itemVO.setCategoryName(category.getName());
        }
        return itemVO;
    }

    @Override
    @Transactional(rollbackFor = BizException.class)
    public void deductStock(List<OrderDetailDTO> details) {
        validateStockDetails(details);
        details.sort(Comparator.comparing(OrderDetailDTO::getItemId));
        for (OrderDetailDTO detail : details) {
            Long itemId = detail.getItemId();
            // 二手交易：在售(1) -> 已售(3)，无库存字段，用状态标记交易占用
            boolean update = lambdaUpdate()
                    .eq(Item::getId, itemId)
                    .eq(Item::getStatus, ItemStatus.ON_SHELF.getValue())
                    .set(Item::getStatus, ItemStatus.SOLD.getValue())
                    .update();
            if (!update) {
                Item item = getById(itemId);
                if (item != null) {
                    throw new BizException(BizException.BAD_REQUEST_CODE, STOCK_NOT_ENOUGH_MESSAGE);
                } else {
                    throw new BizException(BizException.BAD_REQUEST_CODE, ITEM_NOT_FOUND_MESSAGE);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = BizException.class)
    public void restoreStock(List<OrderDetailDTO> details) {
        validateStockDetails(details);
        for (OrderDetailDTO detail : details) {
            Long itemId = detail.getItemId();
            Item item = getById(itemId);
            if (item == null) {
                throw new BizException(BizException.BAD_REQUEST_CODE, ITEM_NOT_FOUND_MESSAGE);
            }
            // 退款恢复：已售(3) -> 在售(1)
            lambdaUpdate()
                    .eq(Item::getId, itemId)
                    .set(Item::getStatus, ItemStatus.ON_SHELF.getValue())
                    .update();
        }
    }

    private void validateStockDetails(List<OrderDetailDTO> details) {
        if (details == null || details.isEmpty()) {
            throw new BizException(BizException.BAD_REQUEST_CODE, DETAILS_EMPTY_MESSAGE);
        }
        for (OrderDetailDTO detail : details) {
            if (detail == null || detail.getItemId() == null) {
                throw new BizException(BizException.BAD_REQUEST_CODE, ITEM_DETAIL_INVALID_MESSAGE);
            }
            if (detail.getNum() == null || detail.getNum() <= MIN_VALID_QUANTITY) {
                throw new BizException(BizException.BAD_REQUEST_CODE, QUANTITY_INVALID_MESSAGE);
            }
        }
    }


    private long normalizedPageSize(Integer pageSize) {
        if (pageSize == null || pageSize < PageQuery.MIN_PAGE_SIZE) {
            return PageQuery.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, PageQuery.MAX_PAGE_SIZE);
    }

}




