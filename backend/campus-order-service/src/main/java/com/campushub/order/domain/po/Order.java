package com.campushub.order.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_order")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单业务号（跨服务唯一标识，tb_order.order_no 非空且唯一，不可为空） */
    private String orderNo;

    private Long userId;

    /** 订单来源类型，目前固定 SECONDHAND */
    private String orderType;

    /** 订单总金额（分） */
    private Integer totalFee;

    /** 1余额 2模拟支付；0 表示下单时尚未选择支付方式 */
    private Integer paymentType;

    /** 支付服务业务号，支付成功后回写；未支付时为 null */
    private String paymentNo;

    /** 1未付款 2已付款 3已发货 4已完成 5已关闭 */
    private Integer status;

    /** 以下三个字段为下单时地址快照 */
    private String receiver;

    private String phone;

    private String address;

    /** 买家备注 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private LocalDateTime consignTime;

    private LocalDateTime endTime;

    private LocalDateTime closeTime;

    private LocalDateTime updateTime;
}




