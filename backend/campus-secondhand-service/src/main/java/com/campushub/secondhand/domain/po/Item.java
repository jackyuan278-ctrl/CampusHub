package com.campushub.secondhand.domain.po;

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
@TableName("tb_item")
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 卖家业务ID */
    private Long sellerId;

    private String name;

    /** 价格（分） */
    private Integer price;

    private String image;

    private Long categoryId;

    /** 成色描述 */
    private String conditionDesc;

    private String description;

    /** 交易地点 */
    private String tradeLocation;

    /** 0下架 1在售 2预订 3已售 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}




