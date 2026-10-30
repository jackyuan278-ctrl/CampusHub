package com.campushub.cart.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.campushub.api.client.ItemClient;
import com.campushub.api.dto.ItemDTO;
import com.campushub.api.dto.ItemStatusDTO;
import com.campushub.cart.domain.dto.AddCartDTO;
import com.campushub.cart.domain.dto.UpdateCartNumDTO;
import com.campushub.cart.domain.vo.CartItemVO;
import com.campushub.cart.service.ICartService;
import com.campushub.common.BizException;
import com.campushub.common.Result;
import com.campushub.common.interceptor.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements ICartService {

    private static final String KEY_PREFIX = "campushub:cart:";

    // 与 secondhand 模块 ItemStatus 保持一致的取值范围（cart 不依赖 secondhand，故本地定义）
    private static final int ITEM_STATUS_ON_SHELF = 1;
    private static final int ITEM_STATUS_SOLD = 3;

    private final StringRedisTemplate stringRedisTemplate;
    private final ItemClient itemClient;

    @Override
    public List<CartItemVO> queryMyCart() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(400,"请先登录");
        }
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(KEY_PREFIX + userId);
        List<CartItemVO> list = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Object valueobj = entry.getValue();
            if (valueobj == null) {
                continue;
            }
            String jsonStr = valueobj.toString();
            try {
                CartItemVO itemVO = JSONUtil.toBean(jsonStr, CartItemVO.class);
                // 历史脏数据兜底：旧版本允许 num>1，读出来一律归一到 1，否则前端合计会按旧数量放大
                itemVO.setNum(1);
                list.add(itemVO);
            } catch (Exception e) {
                log.error("购物车项JSON解析失败，userId:{}, json:{}", userId, jsonStr, e);
            }
        }
        if (!list.isEmpty()) {
            refreshStatus(list);
        }
        return list;
    }

    /**
     * 现查商品状态，让已售/下架的条目在购物车里就能标灰。
     * 商品服务不可用时整体降级：状态留 null，前端不标灰，
     * 结算时后端仍会拦住 —— 宁可少提示，也不要把整页购物车打挂。
     */
    private void refreshStatus(List<CartItemVO> list) {
        try {
            List<Long> ids = list.stream().map(CartItemVO::getItemId).filter(Objects::nonNull).toList();
            if (ids.isEmpty()) {
                return;
            }
            Result<List<ItemStatusDTO>> result = itemClient.queryItemStatus(ids);
            List<ItemStatusDTO> data = result == null ? null : result.getData();
            if (data == null) {
                return;
            }
            Map<Long, Integer> statusMap = data.stream()
                    .filter(dto -> dto.getId() != null)
                    .collect(Collectors.toMap(ItemStatusDTO::getId,
                            dto -> dto.getStatus() == null ? ITEM_STATUS_ON_SHELF : dto.getStatus()));
            for (CartItemVO vo : list) {
                // 查不到记录说明商品已被删除，等同于不可购买
                vo.setStatus(statusMap.getOrDefault(vo.getItemId(), ITEM_STATUS_SOLD));
            }
        } catch (Exception e) {
            log.warn("购物车刷新商品状态失败，本次不标记状态", e);
        }
    }

    @Override
    public void addItem(AddCartDTO addCartDTO) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(400,"请先登录");
        }
        // 二手商品一物一件：数量恒为 1，不采信前端传值（DTO 的 @Max(1) 是第一道，这里不依赖校验也能兜住）
        Integer num = 1;
        Long itemId = addCartDTO.getItemId();
        Object o = stringRedisTemplate.opsForHash().get(KEY_PREFIX + userId, itemId.toString());
        if (o != null) {
            String jsonStr = o.toString();
            try {
                CartItemVO itemVO = JSONUtil.toBean(jsonStr, CartItemVO.class);
                // 重复加购是幂等操作：数量保持 1，不再累加
                itemVO.setItemId(itemId);
                itemVO.setNum(num);
                String newjsonStr = JSONUtil.toJsonStr(itemVO);
                stringRedisTemplate.opsForHash().put(KEY_PREFIX+userId,itemId.toString(),newjsonStr);
                stringRedisTemplate.expire(KEY_PREFIX+userId,30, TimeUnit.DAYS);
            } catch (Exception e) {
                log.error("购物车项JSON解析失败，userId:{}, json:{}", userId, jsonStr, e);
            }
        } else {
            Result<ItemDTO> result = itemClient.queryItemById(itemId);
            ItemDTO itemDTO = result == null ? null : result.getData();
            if (itemDTO == null) {
                throw new BizException(400,"货物不存在" );
            }
            // 只认在售(1)：下架(0)、预订(2)、已售(3) 一律不可加购。
            // 写 != 1 而不是 == 0，避免依赖「状态 2 永不出现」这个假设（详见 ItemStatus 枚举）
            Integer status = itemDTO.getStatus();
            if (status != null && status != ITEM_STATUS_ON_SHELF) {
                throw new BizException(400, status == ITEM_STATUS_SOLD ? "商品已售出" : "商品暂不可购买");
            }
            CartItemVO cartItemVO = BeanUtil.copyProperties(itemDTO, CartItemVO.class);
            cartItemVO.setItemId(itemId);
            cartItemVO.setNum(num);
            stringRedisTemplate.opsForHash().put(KEY_PREFIX+userId,itemId.toString(),JSONUtil.toJsonStr(cartItemVO));
            stringRedisTemplate.expire(KEY_PREFIX+userId,30, TimeUnit.DAYS);
        }

    }

    @Override
    public void updateNum(Long itemId, UpdateCartNumDTO updateCartNumDTO) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(400,"请先登录");
        }
        // 二手商品一物一件：数量恒为 1，不采信前端传值
        Integer num = 1;
        Object o = stringRedisTemplate.opsForHash().get(KEY_PREFIX + userId, itemId.toString());
        if (o != null) {
            String jsonStr = o.toString();
            try {
                CartItemVO itemVO = JSONUtil.toBean(jsonStr, CartItemVO.class);
                itemVO.setNum(num);
                String newjsonStr = JSONUtil.toJsonStr(itemVO);
                stringRedisTemplate.opsForHash().put(KEY_PREFIX+userId,itemId.toString(),newjsonStr);
                stringRedisTemplate.expire(KEY_PREFIX+userId,30, TimeUnit.DAYS);
            } catch (Exception e) {
                log.error("购物车项JSON解析失败，userId:{}, json:{}", userId, jsonStr, e);
            }
        }else {
            throw new BizException(400, "购物车条目不存在");
        }

    }

    @Override
    public void removeItem(Long itemId) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(400,"请先登录");
        }
        stringRedisTemplate.opsForHash().delete(KEY_PREFIX + userId, itemId.toString());
    }

    @Override
    public void clearCart() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(400,"请先登录");
        }
        stringRedisTemplate.delete(KEY_PREFIX + userId);
    }
}




