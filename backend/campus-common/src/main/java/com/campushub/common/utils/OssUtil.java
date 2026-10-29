package com.campushub.common.utils;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.campushub.common.BizException;
import com.campushub.common.config.OssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传工具（阿里云 OSS）。
 *
 * <p>放在 campus-common 里而不是单独建文件服务：上传只是「校验 + 传一个对象 + 拼 URL」，
 * 不需要自己维护元数据，任何服务加一个 controller 就能用。</p>
 *
 * <p>OSSClient 用 {@link ObjectProvider} 拿，而不是构造器直接注入——客户端只在
 * {@code aliyun.oss.enabled=true} 时存在，直接注入会让没配 OSS 的服务启动失败。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssUtil {

    private static final DateTimeFormatter DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final String OBJECT_PREFIX = "campushub/";
    /** 文件名后缀白名单，避免用户传 .jsp / .html 之类的东西上去 */
    private static final Set<String> ALLOWED_SUFFIX = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final OssProperties properties;
    private final ObjectProvider<OSSClient> clientProvider;

    /** 当前服务是否具备上传能力（开关打开、密钥齐全、客户端已创建） */
    public boolean isEnabled() {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getAccessKeyId())
                && StringUtils.hasText(properties.getAccessKeySecret())
                && clientProvider.getIfAvailable() != null;
    }

    /**
     * 上传一张图片。
     *
     * @param file 表单文件
     * @return 可直接给前端 &lt;img src&gt; 用的公网地址
     */
    public String upload(MultipartFile file) {
        if (!isEnabled()) {
            throw new BizException(BizException.BAD_REQUEST_CODE,
                    "图片上传未启用：请在 application-dev.yml 配置 aliyun.oss 密钥，或用 OSS_AK / OSS_SK 环境变量启动");
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(BizException.BAD_REQUEST_CODE, "请选择要上传的图片");
        }
        long maxSize = properties.getMaxSize();
        if (file.getSize() > maxSize) {
            throw new BizException(BizException.BAD_REQUEST_CODE, "图片不能超过 " + (maxSize / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!properties.getAllowedTypes().contains(contentType)) {
            throw new BizException(BizException.BAD_REQUEST_CODE, "只支持 jpg / png / webp / gif 格式的图片");
        }

        String objectName = OBJECT_PREFIX + LocalDate.now().format(DIR_FORMAT) + "/"
                + UUID.randomUUID().toString().replace("-", "") + resolveSuffix(file.getOriginalFilename(), contentType);
        try {
            clientProvider.getObject().putObject(PutObjectRequest.newBuilder()
                    .bucket(properties.getBucket())
                    .key(objectName)
                    .contentType(contentType)
                    .body(BinaryData.fromBytes(file.getBytes()))
                    .build());
        } catch (Exception e) {
            // 不把 SDK 的原始异常抛给前端：里面可能带 endpoint、bucket 甚至请求签名片段
            log.error("图片上传失败：objectName={}", objectName, e);
            throw new BizException(BizException.INTERNAL_SERVER_ERROR_CODE, "图片上传失败，请稍后重试");
        }
        String url = buildUrl(objectName);
        log.info("图片上传成功：{}", url);
        return url;
    }

    /** 优先用文件名的后缀（保留真实格式），拿不到时按 content-type 反推 */
    private String resolveSuffix(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                String suffix = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ALLOWED_SUFFIX.contains(suffix)) {
                    return "." + suffix;
                }
            }
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    /** 访问地址规则：https://{bucket}.{endpoint}/{objectName} */
    private String buildUrl(String objectName) {
        String endpoint = properties.getEndpoint() == null ? "" : properties.getEndpoint().trim();
        if (endpoint.startsWith("https://")) {
            endpoint = endpoint.substring("https://".length());
        } else if (endpoint.startsWith("http://")) {
            endpoint = endpoint.substring("http://".length());
        }
        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return "https://" + properties.getBucket() + "." + endpoint + "/" + objectName;
    }
}
