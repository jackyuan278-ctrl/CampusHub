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

    private String name;

    /** 价格（分） */
    private Integer price;

    private Integer stock;

    private String image;

    private Long categoryId;

    private Long brandId;

    private String spec;

    /** 销量（冗余，下单后异步累加） */
    private Integer sales;

    /** 1上架 0下架 */
    private Integer status;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}




