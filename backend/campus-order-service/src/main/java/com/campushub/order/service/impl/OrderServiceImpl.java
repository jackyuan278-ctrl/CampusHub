package com.campushub.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.api.client.ItemClient;
import com.campushub.api.constants.MqConstants;
import com.campushub.api.dto.ItemDTO;
import com.campushub.api.dto.OrderDetailDTO;
import com.campushub.common.BizException;
import com.campushub.common.Result;
import com.campushub.common.interceptor.UserContext;
import com.campushub.common.utils.BizNoGenerator;
import com.campushub.order.domain.dto.OrderFormDTO;
import com.campushub.order.domain.dto.OrderGoodsDTO;
import com.campushub.order.domain.po.Order;
import com.campushub.order.domain.po.OrderDetail;
import com.campushub.order.domain.vo.OrderGoodsVO;
import com.campushub.order.domain.vo.OrderVO;
import com.campushub.order.mapper.OrderMapper;
import com.campushub.order.service.IOrderDetailService;
import com.campushub.order.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    /** 订单来源类型，与 tb_order.order_type 默认值保持一致 */
    private static final String ORDER_TYPE_SECONDHAND = "SECONDHAND";
    /** 待支付 */
    private static final int ORDER_STATUS_UNPAID = 1;
    /** 下单时尚未选择支付方式，与 tb_order.payment_type 默认值保持一致 */
    private static final int PAYMENT_TYPE_UNSET = 0;
    /** 商品快照缺少卖家时的兜底值，避免 NOT NULL 约束直接失败 */
    private static final Long NO_SELLER_ID = 0L;
    /**
     * 单笔订单金额上限（单位：分）= 10 万元。
     * 远低于 total_fee INT 上限（约 2147 万元），既拦住异常大额订单，也保证 (int) 强转永不截断。
     */
    private static final long MAX_ORDER_FEE = 10_000_000L;

    private final ItemClient  itemClient;
    private final IOrderDetailService orderDetailService;
    private final RabbitTemplate rabbitTemplate;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderFormDTO orderFormDTO) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(400,"请先登录");
        }
        //      2. 逐件调 ItemClient.queryItemById 校验商品存在且上架，以服务端实时价重算 totalFee
        //         （不采信前端 price，防改包篡改价格）
        List<OrderGoodsDTO> goods = orderFormDTO.getGoods();
        if (goods == null || goods.isEmpty()) {
            throw new BizException(400,"商品信息为空");
        }
        //      3. 调 ItemClient.deductStock 预扣库存
        // 用 long 累加：int 相乘会静默溢出成负数，让订单金额变成一折甚至负数
        long totalFee = 0;
        List<OrderDetailDTO> orderDetailDTOList = new ArrayList<>();
        // itemId -> 商品快照，写订单明细时复用，避免为了拿卖家 ID 再查一次商品服务
        Map<Long, ItemDTO> itemSnapshots = new HashMap<>();
        for (OrderGoodsDTO good : goods) {
            if (good == null || good.getItemId() == null) {
                throw new BizException(400, "商品信息不完整");
            }
            Result<ItemDTO> itemDTOResult = itemClient.queryItemById(good.getItemId());
            if (itemDTOResult==null){
                throw new BizException(400,"商品不存在");
            }
            ItemDTO data = itemDTOResult.getData();
            if (data==null){
                throw new BizException(400,"商品不存在");
            }
            itemSnapshots.put(good.getItemId(), data);
            Integer num = good.getNum();
            if (num == null || num <= 0) {
                throw new BizException(400, "商品数量必须大于0");
            }
            // 二手商品一物一件：即使绕过 DTO 的 @Max(1) 传了更大的数，服务端也一律按 1 计
            num = 1;
            OrderDetailDTO  orderDetailDTO = new OrderDetailDTO();
            orderDetailDTO.setItemId(good.getItemId());
            orderDetailDTO.setNum(num);
            orderDetailDTOList.add(orderDetailDTO);
            if (data.getStatus()!= 1) throw new BizException(400,"选中的商品有暂未出售的");
            // 商品快照一律以服务端为准覆盖前端传值，否则改包会让明细单价与 totalFee 对不上
            good.setName(data.getName());
            good.setPrice(data.getPrice());
            good.setImage(data.getImage());
            good.setSpec(data.getSpec());
            Integer unitPrice = data.getPrice();
            if (unitPrice == null || unitPrice <= 0) {
                // 价格为 0 或负数等于白送，必须在算钱之前拦住
                throw new BizException(400, "商品价格异常");
            }
            try {
                totalFee = Math.addExact(totalFee, Math.multiplyExact((long) unitPrice, (long) num));
            } catch (ArithmeticException overflow) {
                // multiplyExact / addExact 溢出即抛，不会像 * 和 += 那样静默变成负数
                throw new BizException(400, "订单金额超限");
            }
        }
        // 上限校验放在扣库存之前：超限直接拒单，不必再走库存补偿
        if (totalFee <= 0 || totalFee > MAX_ORDER_FEE) {
            throw new BizException(400, "订单金额超限");
        }

        Result<Void> voidResult = itemClient.deductStock(orderDetailDTOList);
        if (voidResult == null || voidResult.getCode() == null || voidResult.getCode() != 200) {
            throw new BizException(400, voidResult == null ? "库存扣减失败" : voidResult.getMsg());
        }
        //      4. 本地事务写 tb_order（地址快照，status=1）+ tb_order_detail（商品快照）
        //      写库失败时补偿回滚库存（扣库存成功但本地事务失败）
        try {
            Order order = new Order();
            BeanUtils.copyProperties(orderFormDTO,order);
            order.setOrderNo(BizNoGenerator.order());
            order.setOrderType(ORDER_TYPE_SECONDHAND);
            order.setUserId(userId);
            // 上方已校验 totalFee <= MAX_ORDER_FEE(1000 万分)，此处强转 int 不会截断，DB 列类型无需改动
            order.setTotalFee((int) totalFee);
            // 显式置为待支付，不依赖数据库默认值：markPaySuccess / closeOrder 都按 status=1 做条件更新
            order.setStatus(ORDER_STATUS_UNPAID);
            order.setPaymentType(PAYMENT_TYPE_UNSET);
            order.setCreateTime(LocalDateTime.now());
            this.save(order);
            List<OrderDetail> orderDetailList = new ArrayList<>();
            List<OrderGoodsVO> orderGoodsVOS = new ArrayList<>();
            for (OrderGoodsDTO good : goods) {
                OrderDetail orderDetail = new OrderDetail();
                BeanUtils.copyProperties(good,orderDetail);
                orderDetail.setItemId(good.getItemId());
                // 卖家 ID 是 tb_order_detail 的非空快照字段，必须在明细里固化，避免回查商品库
                ItemDTO snapshot = itemSnapshots.get(good.getItemId());
                orderDetail.setSellerId(snapshot == null || snapshot.getSellerId() == null
                        ? NO_SELLER_ID : snapshot.getSellerId());
                orderDetail.setCreateTime(LocalDateTime.now());
                orderDetail.setOrderId(order.getId());
                OrderGoodsVO orderGoodsVO = new OrderGoodsVO();
                BeanUtils.copyProperties(good,orderGoodsVO);
                orderGoodsVOS.add(orderGoodsVO);
                orderDetailList.add(orderDetail);
            }
            orderDetailService.saveBatch(orderDetailList);
            //      5. 发送延迟消息（trade.delay.direct / order.delay，TTL 队列超时关单）
            rabbitTemplate.convertAndSend(MqConstants.TRADE_DELAY_EXCHANGE, MqConstants.TRADE_ORDER_DELAY_KEY, order.getId());
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order,orderVO);
            orderVO.setGoods(orderGoodsVOS);
            return orderVO;
        } catch (Exception e) {
            itemClient.restoreStock(orderDetailDTOList);
            throw e;
        }
    }

    @Override
    public List<OrderVO> queryMyOrders() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(400,"请先登录");
        }
        List<Order> list = lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .list();
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = list.stream().map(Order::getId).collect(Collectors.toList());
        Map<Long, List<OrderDetail>> detailMap = orderDetailService.lambdaQuery()
                .in(OrderDetail::getOrderId, orderIds)
                .list()
                .stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Order order : list) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            List<OrderGoodsVO> orderGoodsVOList = new ArrayList<>();
            for (OrderDetail orderDetail : detailMap.getOrDefault(order.getId(), Collections.emptyList())) {
                OrderGoodsVO orderGoodsVO = new OrderGoodsVO();
                BeanUtils.copyProperties(orderDetail, orderGoodsVO);
                orderGoodsVOList.add(orderGoodsVO);
            }
            orderVO.setGoods(orderGoodsVOList);
            orderVOList.add(orderVO);
        }
        return orderVOList;
    }

    @Override
    public OrderVO queryOrderById(Long id) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(400,"请先登录");
        }
        Order order = lambdaQuery().eq(Order::getId, id).eq(Order::getUserId, userId).one();
        if (order == null) {
            throw new BizException(400,"订单不存在");
        }
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order,orderVO);
        List<OrderGoodsVO> goodsVOS = orderDetailService.lambdaQuery().eq(OrderDetail::getOrderId, id).list().stream().map(orderDetail -> {
            OrderGoodsVO orderGoodsVO = new OrderGoodsVO();
            BeanUtils.copyProperties(orderDetail, orderGoodsVO);
            orderGoodsVO.setItemId(orderDetail.getItemId());
            return orderGoodsVO;
        }).collect(Collectors.toList());

        orderVO.setGoods(goodsVOS);

        return orderVO;

    }

    @Override
    public void markPaySuccess(Long orderId) {
        // Pay 已将支付单确认成功后才发送消息；Trade 只负责幂等地更新自己的订单状态。
        // TODO 销量累加待拍板（建议 item 服务监听支付成功消息异步加 sold）
        boolean success = lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, 1)
                .set(Order::getStatus, 2)
                .set(Order::getPayTime, LocalDateTime.now())
                .update();
        if (success) {
            return;
        }
        // 更新失败=重复消息（幂等静默）或订单已被超时关闭（关单赢了支付的竞态，需退款补偿）
        Order order = getById(orderId);
        if (order != null && Integer.valueOf(5).equals(order.getStatus())) {
            log.warn("订单{}已关闭但收到支付成功消息，待退款补偿", orderId);
        }
    }

    @Override
    public void closeOrder(Long orderId) {
        boolean success = lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, 1)
                .set(Order::getStatus, 5)
                .set(Order::getCloseTime, LocalDateTime.now())
                .update();
        if (!success) {
            // 已支付或已关闭（重复消息）：静默幂等，此时回滚库存会虚增
            return;
        }
        List<OrderDetailDTO> restoreList = orderDetailService.lambdaQuery()
                .eq(OrderDetail::getOrderId, orderId)
                .list()
                .stream()
                .map(detail -> {
                    OrderDetailDTO dto = new OrderDetailDTO();
                    dto.setItemId(detail.getItemId());
                    dto.setNum(detail.getNum());
                    return dto;
                })
                .collect(Collectors.toList());
        Result<Void> result = itemClient.restoreStock(restoreList);
        // 订单已关，补偿失败不能抛异常（重试时 update=false 会静默跳过，库存永远回不来），只能记日志人工介入
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            log.error("订单{}关单回滚库存失败，需人工介入", orderId);
        }
    }
}




