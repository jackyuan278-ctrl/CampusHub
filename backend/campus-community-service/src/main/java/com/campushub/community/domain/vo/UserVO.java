package com.campushub.community.domain.vo;

import lombok.Data;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatarUrl;

    /** 0学生 1管理员 */
    private Integer role;
}





