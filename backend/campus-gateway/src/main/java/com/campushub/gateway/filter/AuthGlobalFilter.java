package com.campushub.gateway.filter;

import com.campushub.common.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 下游服务据此识别当前用户，只能由网关写入 */
    private static final String USER_INFO_HEADER = "user-info";
    private static final String BEARER_PREFIX = "Bearer ";

    /** 服务间内部密钥，只应由服务自己生成，客户端带来的一律丢弃 */
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    /**
     * 内部接口黑名单：这些路径只允许服务间 Feign 直连，公网请求即使带合法 JWT 也拒绝。
     * 用 {@code *} 精确匹配单段，不开 {@code **}，避免误伤后续正常接口。
     */
    private static final List<String> INTERNAL_DENY = List.of(
            "/api/users/*/balance/deduct",
            "/api/users/*/balance/refund",
            // 批量查用户只给服务间用：公网放行等于开放用户枚举
            "/api/users/batch",
            "/api/items/stock/deduct",
            "/api/items/stock/restore",
            // 批量查商品状态只给购物车用，公网放行等于开放批量抓商品
            "/api/items/batch/status"
    );

    private final JwtTool jwtTool;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        // 内部接口先于一切鉴权判断：这类路径本就不给公网用
        if (isInternalDenied(path)) {
            log.warn("拦截公网访问内部接口 path={} remote={}", path, request.getRemoteAddress());
            return forbidden(exchange);
        }

        String token = resolveToken(request);

        if (isExclude(path, request.getMethod().name())) {
            // 公开接口不强制登录，但身份头必须由网关决定：
            // 先移除客户端可能自带的 user-info（否则可以伪造他人身份读个性化字段），
            // 再在 token 合法时按 token 注入，保证「带 token 才会有 mySignupStatus / liked」的语义。
            ServerHttpRequest.Builder builder = request.mutate()
                    .headers(headers -> {
                        headers.remove(USER_INFO_HEADER);
                        // 客户端可能自带内部密钥头，一律丢弃，绝不让它流到下游
                        headers.remove(INTERNAL_TOKEN_HEADER);
                    });
            Long optionalUserId = parseUserIdQuietly(token);
            if (optionalUserId != null) {
                builder.header(USER_INFO_HEADER, optionalUserId.toString());
            }
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        Long userId = parseUserIdQuietly(token);
        if (userId == null) {
            return unauthorized(exchange);
        }
        ServerHttpRequest newRequest = request.mutate()
                .headers(headers -> {
                    headers.remove(USER_INFO_HEADER);
                    headers.remove(INTERNAL_TOKEN_HEADER);
                })
                .header(USER_INFO_HEADER, userId.toString())
                .build();
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    /**
     * 取出 JWT。前端按 HTTP 标准带的是 {@code Authorization: Bearer <token>}，
     * 需要先剥掉 "Bearer " 前缀再交给解析器，否则签名校验必然失败并返回 401。
     * SSE 场景浏览器带不了自定义头，token 允许走 query 参数兜底。
     */
    private String resolveToken(ServerHttpRequest request) {
        String raw = request.getHeaders().getFirst("authorization");
        if (raw == null || raw.isBlank()) {
            raw = request.getQueryParams().getFirst("authorization");
        }
        if (raw == null) {
            return null;
        }
        String token = raw.trim();
        if (token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            token = token.substring(BEARER_PREFIX.length()).trim();
        }
        return token.isEmpty() ? null : token;
    }

    private Long parseUserIdQuietly(String token) {
        try {
            return token == null ? null : jwtTool.parseToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isExclude(String path, String method) {
        if (antPathMatcher.match("/api/users/login", path) || antPathMatcher.match("/api/users/register", path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method)
                && (antPathMatcher.match("/api/items/**", path)
                || antPathMatcher.match("/api/activities/**", path)
                || antPathMatcher.match("/api/community/**", path))) {
            return true;
        }
        return antPathMatcher.match("/api/health/**", path) || antPathMatcher.match("/health/**", path);
    }

    private boolean isInternalDenied(String path) {
        return INTERNAL_DENY.stream().anyMatch(pattern -> antPathMatcher.match(pattern, path));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 与下游门禁保持同一话术：不向外部确认该接口存在
        byte[] bytes = "{\"code\":403,\"msg\":\"接口不存在\"}".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = "{\"code\":401,\"msg\":\"未登录\"}".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
