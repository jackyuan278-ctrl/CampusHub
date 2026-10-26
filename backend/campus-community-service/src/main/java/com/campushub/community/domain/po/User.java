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
@TableName("tb_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    /** BCrypt 哈希，禁止明文入库 */
    private String password;

    private String nickname;

    private String avatarUrl;

    /** 0学生 1管理员 */
    private Integer role;

    /** 1正常 0封禁 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}





