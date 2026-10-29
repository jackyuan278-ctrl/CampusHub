package com.campushub.common.config;

import com.campushub.common.interceptor.InternalTokenInterceptor;
import com.campushub.common.interceptor.UserContext;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Feign 请求拦截：出站请求在发出前统一加工。
 * <p>
 * 1. 身份接力：把当前线程 user-info 头透传给下游服务；<br>
 * 2. 内部密钥：给所有 Feign 请求带上 X-Internal-Token，供下游的
 *    {@link InternalTokenInterceptor} 校验调用方身份。<br>
 * 依赖 openfeign，未引入时整段跳过。
 */
@Configuration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignRelayConfig {

    @Bean
    public RequestInterceptor userInfoFeignInterceptor() {
        return template -> {
            Long userId = UserContext.getUser();
            if (userId != null) {
                template.header("user-info", userId.toString());
            }
        };
    }

    /**
     * 给服务间调用盖章。密钥留空时不发头，下游会 fail-closed 拒绝——
     * 配置缺失会以"调用报 403"的形式立刻暴露，而不是静默放行。
     */
    @Bean
    public RequestInterceptor internalTokenFeignInterceptor(
            @Value("${campus.internal.token:}") String internalToken) {
        return template -> {
            if (StringUtils.hasText(internalToken)) {
                template.header(InternalTokenInterceptor.HEADER, internalToken);
            }
        };
    }
}
