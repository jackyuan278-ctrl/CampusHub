package com.campushub.community.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnswerVO {

    private Long id;

    private Long questionId;

    private Long userId;

    private String authorNickname;

    private String authorAvatar;

    private String content;

    private Integer likeCount;

    private Integer commentCount;

    /** 当前用户是否已点赞（未登录为 null） */
    private Boolean liked;

    private LocalDateTime createTime;
}





