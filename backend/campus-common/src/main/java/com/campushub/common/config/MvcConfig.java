package com.campushub.common.config;

import com.campushub.common.interceptor.InternalTokenInterceptor;
import com.campushub.common.interceptor.UserInfoInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    /** 服务间共享密钥，由各服务 yml 的 campus.internal.token 注入 */
    @Value("${campus.internal.token:}")
    private String internalToken;

    /**
     * 需要强制校验内部密钥的路径，逗号分隔。
     * 各服务只声明自己对外提供的那几个内部接口，避免误伤正常业务路径。
     */
    @Value("${campus.internal.protected-paths:}")
    private String[] protectedPaths;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserInfoInterceptor()).addPathPatterns("/**");

        if (protectedPaths != null && protectedPaths.length > 0) {
            registry.addInterceptor(new InternalTokenInterceptor(internalToken))
                    .addPathPatterns(protectedPaths);
        }
    }
}
