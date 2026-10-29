package com.campushub.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 图片上传（阿里云 OSS）配置。
 *
 * <p>默认关闭：campus-common 被全部服务依赖（含网关），没有配置 OSS 的服务
 * 不应该因为少一段配置就起不来。需要上传能力的三方（secondhand / activity / user）
 * 在自己的 application.yml 里显式打开。</p>
 */
@Data
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    /** 开关：为 true 且密钥齐全时才会创建 OSSClient */
    private boolean enabled = false;

    /** 区域，V2 SDK 必填，如 cn-hangzhou */
    private String region = "cn-hangzhou";

    /** 访问域名，写 oss-cn-hangzhou.aliyuncs.com 或带 https:// 都可以 */
    private String endpoint;

    /** 存储空间 */
    private String bucket;

    private String accessKeyId;

    private String accessKeySecret;

    /** 单张图片大小上限（字节），默认 5MB，与前端 el-upload 的限制保持一致 */
    private long maxSize = 5 * 1024 * 1024L;

    /** 允许上传的图片类型 */
    private List<String> allowedTypes = List.of("image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif");
}
