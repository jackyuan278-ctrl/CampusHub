package com.campushub.secondhand.controller;

import com.campushub.api.dto.ItemStatusDTO;
import com.campushub.api.dto.OrderDetailDTO;
import com.campushub.common.Result;
import com.campushub.common.domain.PageDTO;
import com.campushub.common.utils.OssUtil;
import com.campushub.secondhand.domain.dto.ItemPublishDTO;
import com.campushub.secondhand.domain.query.ItemPageQuery;
import com.campushub.secondhand.domain.vo.ItemVO;
import com.campushub.secondhand.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final IItemService itemService;
    private final OssUtil ossUtil;

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

    /**
     * 发布商品：需登录（网关白名单只放行 GET /api/items/**，POST 会走鉴权）。
     * sellerId 取自网关注入的 user-info，请求体里没有这个字段。
     */
    @PostMapping
    public Result<Long> publish(@RequestBody @Validated ItemPublishDTO dto) {
        return Result.success(itemService.publish(dto));
    }

    /**
     * 上传商品图：需登录（白名单只放行 GET /api/items/**）。
     * 返回可直接放进 item.image 的地址，不落库。
     */
    @PostMapping("/images")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(ossUtil.upload(file));
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

    /** 内部接口：批量查状态与实时价（购物车标出已售商品，网关已列入黑名单） */
    @PostMapping("/batch/status")
    public Result<List<ItemStatusDTO>> queryStatusBatch(@RequestBody List<Long> ids) {
        return Result.success(itemService.queryStatusBatch(ids));
    }
}




