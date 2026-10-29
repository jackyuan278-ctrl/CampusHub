package com.campushub.secondhand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.api.dto.OrderDetailDTO;
import com.campushub.common.domain.PageDTO;
import com.campushub.secondhand.domain.dto.ItemPublishDTO;
import com.campushub.secondhand.domain.po.Item;
import com.campushub.secondhand.domain.query.ItemPageQuery;
import com.campushub.secondhand.domain.vo.ItemVO;

import java.util.List;

public interface IItemService extends IService<Item> {

    PageDTO<ItemVO> queryItemPage(ItemPageQuery query);

    List<ItemVO> queryHotItems();

    ItemVO queryItemById(Long id);

    /** 发布商品，返回新商品 id */
    Long publish(ItemPublishDTO dto);

    /** 下单预扣库存（Feign 直连），条件更新防超卖 */
    void deductStock(List<OrderDetailDTO> details);

    /** 超时关单回滚库存（Feign 直连） */
    void restoreStock(List<OrderDetailDTO> details);
}




