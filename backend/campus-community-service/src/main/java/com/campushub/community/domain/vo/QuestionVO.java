package com.campushub.community.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionVO {

    private Long id;

    private Long userId;

    private String authorNickname;

    private String authorAvatar;

    private String title;

    private String content;

    private List<String> tags;

    private Integer viewCount;

    private Integer likeCount;

    private Integer answerCount;

    private Integer commentCount;

    /** 当前用户是否已点赞（未登录为 null） */
    private Boolean liked;

    private LocalDateTime createTime;
}





