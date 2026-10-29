package com.campushub.api.client;

import com.campushub.api.dto.UserBriefDTO;
import com.campushub.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户服务客户端（Feign 直连不经网关）
 * <ul>
 *   <li>支付服务扣余额；</li>
 *   <li>社区服务批量补作者昵称/头像——社区本地那份 tb_user 是历史重复数据，不再读写。</li>
 * </ul>
 */
@FeignClient(value = "campus-user-service", path = "/users")
public interface UserClient {

    @PutMapping("/{id}/balance/deduct")
    Result<Void> deductBalance(@PathVariable("id") Long userId,
                               @RequestParam("orderId") Long orderId,
                               @RequestParam("amount") Integer amount);

    /**
     * 批量查作者信息。
     * 用 POST 而非 GET：id 列表拼进 query 会受 URL 长度限制，几十个 id 就可能被中间件截断。
     * 调用方必须自己兜住"返回 null / code!=200 / data 为 null"这三种情况，服务端不做部分成功。
     */
    @PostMapping("/batch")
    Result<List<UserBriefDTO>> queryUsersByIds(@RequestBody List<Long> ids);
}




