package com.campushub.user.domain.vo;

import lombok.Data;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String avatar;

    /** 账户余额（分） */
    private Integer balance;
}




