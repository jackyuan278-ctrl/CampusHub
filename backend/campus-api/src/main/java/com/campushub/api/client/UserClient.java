package com.campushub.api.client;

import com.campushub.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户服务客户端（支付服务扣余额用，Feign 直连不经网关）
 */
@FeignClient(value = "campus-user-service", path = "/users")
public interface UserClient {

    @PutMapping("/{id}/balance/deduct")
    Result<Void> deductBalance(@PathVariable("id") Long userId,
                               @RequestParam("orderId") Long orderId,
                               @RequestParam("amount") Integer amount);
}




