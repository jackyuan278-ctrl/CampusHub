package com.campushub.api.dto;

import lombok.Data;

/**
 * 跨服务的用户最小可展示信息。
 * <p>
 * 只放"别的服务拼列表时要展示"的字段：password / phone / balance 这类字段一旦
 * 进入对方进程，就再没有边界可言，所以这里一个都不给，DTO 本身就是脱敏边界。
 * <p>
 * 也不放 role：权威库 campushub_user.tb_user 没有该列，硬加只会永远返回 null，
 * 让调用方误以为拿到了职级信息。
 */
@Data
public class UserBriefDTO {

    private Long id;

    private String username;

    private String nickname;

    /** 头像地址（用户服务的 User.avatar 映射的就是 avatar_url 列） */
    private String avatarUrl;
}
