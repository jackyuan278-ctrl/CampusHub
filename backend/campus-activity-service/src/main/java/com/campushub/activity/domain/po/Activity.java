package com.campushub.activity.domain.po;

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
@TableName("tb_activity")
public class Activity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String title;

    private String description;

    private String coverUrl;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime signupStartTime;

    private LocalDateTime signupEndTime;

    /** 总名额：发布时写入一次，此后只读 */
    private Integer quota;

    /** 剩余名额：报名 -1、取消 +1，是"能不能报上"的唯一裁决依据 */
    private Integer remainQuota;

    /** 1报名中 2进行中 3已结束 4已取消 */
    private Integer status;

    private Long publisherId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}





