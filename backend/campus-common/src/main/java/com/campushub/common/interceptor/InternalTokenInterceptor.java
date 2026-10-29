package com.campushub.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 内部接口门禁。
 * <p>
 * 服务间 Feign 调用不走网关，公网也可能直连服务端口，因此内部接口必须自行校验调用方。
 * 只有携带正确 {@code X-Internal-Token} 的请求才放行，其余一律 403。
 * <p>
 * 两处细节：
 * <ul>
 *   <li>密钥未配置时 fail-closed（一律拒绝），避免"忘了配就敞开"；</li>
 *   <li>比对用 {@link MessageDigest#isEqual} 而非 equals —— 恒时比较，不留计时侧信道。</li>
 * </ul>
 */
@Slf4j
public class InternalTokenInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Internal-Token";

    private final byte[] expected;

    public InternalTokenInterceptor(String token) {
        this.expected = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String actual = request.getHeader(HEADER);
        boolean matched = expected.length > 0
                && actual != null
                && MessageDigest.isEqual(expected, actual.getBytes(StandardCharsets.UTF_8));

        if (!matched) {
            log.warn("拒绝非服务间调用：path={} ip={} 是否携带密钥={}",
                    request.getRequestURI(), request.getRemoteAddr(), actual != null);
            // 回"接口不存在"而非"无权限"：不向外部确认该接口的存在
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"接口不存在\"}");
            return false;
        }
        return true;
    }
}
