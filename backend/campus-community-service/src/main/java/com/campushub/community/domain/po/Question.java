package com.campushub.community.domain.po;

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
@TableName("tb_question")
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 提问者ID */
    private Long userId;

    private String title;

    private String content;

    private Integer viewCount;

    private Integer likeCount;

    private Integer answerCount;

    private Integer commentCount;

    /** 1正常 0删除 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}





