package com.campushub.user.domain.vo;

import lombok.Data;

@Data
public class AddressVO {

    private Long id;

    private String receiver;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String detail;

    /** 1默认 0非默认 */
    private Integer isDefault;
}




