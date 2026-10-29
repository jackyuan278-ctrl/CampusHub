package com.campushub.common.config;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS 客户端装配。
 *
 * <p>{@code @ConditionalOnProperty} 这里是关键：不加条件时，未配置 OSS 的服务
 * 会因为 OSSClient 依赖的属性为空而启动失败。加了条件后，
 * 「没配就不创建、要用的服务自己打开」。</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssConfig {

    /** OSSClient 线程安全，全局复用一个实例；容器关闭时由 Spring 调 close() 释放连接池 */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "aliyun.oss", name = "enabled", havingValue = "true")
    public OSSClient ossClient(OssProperties properties) {
        log.info("初始化 OSS 客户端：endpoint={} bucket={}", properties.getEndpoint(), properties.getBucket());
        return OSSClient.newBuilder()
                .credentialsProvider(new StaticCredentialsProvider(properties.getAccessKeyId(), properties.getAccessKeySecret()))
                .region(properties.getRegion())
                .endpoint(properties.getEndpoint())
                .build();
    }
}
