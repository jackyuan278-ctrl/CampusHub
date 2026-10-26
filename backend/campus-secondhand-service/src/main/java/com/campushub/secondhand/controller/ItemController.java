package com.campushub.secondhand.controller;

import com.campushub.api.dto.OrderDetailDTO;
import com.campushub.common.Result;
import com.campushub.common.domain.PageDTO;
import com.campushub.secondhand.domain.query.ItemPageQuery;
import com.campushub.secondhand.domain.vo.ItemVO;
import com.campushub.secondhand.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final IItemService itemService;

    /** 免鉴权（网关白名单：GET /api/items/**） */
    @GetMapping
    public Result<PageDTO<ItemVO>> queryItemPage(ItemPageQuery query) {
        return Result.success(itemService.queryItemPage(query));
    }


    @GetMapping("/hot")
    public Result<List<ItemVO>> queryHotItems() {
        return Result.success(itemService.queryHotItems());
    }

    /** 免鉴权（网关白名单） */
    @GetMapping("/{id}")
    public Result<ItemVO> queryItemById(@PathVariable("id") Long id) {
        return Result.success(itemService.queryItemById(id));
    }

    /** 内部接口：交易服务下单预扣（Feign 直连 item-service，不经网关） */
    @PutMapping("/stock/deduct")
    public Result<Void> deductStock(@RequestBody List<OrderDetailDTO> details) {
        itemService.deductStock(details);
        return Result.success();
    }

    /** 内部接口：交易服务超时关单回滚（Feign 直连，不经网关） */
    @PutMapping("/stock/restore")
    public Result<Void> restoreStock(@RequestBody List<OrderDetailDTO> details) {
        itemService.restoreStock(details);
        return Result.success();
    }
}




