package com.campushub.community.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {

    private Long id;

    private Long answerId;

    private Long userId;

    private String authorNickname;

    private String authorAvatar;

    private String content;

    private LocalDateTime createTime;
}





