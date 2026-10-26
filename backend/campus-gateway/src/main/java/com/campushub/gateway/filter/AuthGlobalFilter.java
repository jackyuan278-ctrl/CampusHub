package com.campushub.gateway.filter;

import com.campushub.common.utils.JwtTool;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTool jwtTool;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        if (isExclude(path, request.getMethod().name())) {
            return chain.filter(exchange);
        }
        String token = null;
        List<String> headers = request.getHeaders().get("authorization");
        if (headers != null && !headers.isEmpty()) {
            token = headers.get(0);
        }
        // EventSource 带不了自定义头：SSE 请求（/api/ai/chat/stream）token 走 query 参数兜底
        if (token == null || token.isEmpty()) {
            token = request.getQueryParams().getFirst("authorization");
        }
        Long userId;
        try {
            userId = jwtTool.parseToken(token);
        } catch (Exception e) {
            return unauthorized(exchange);
        }
        ServerHttpRequest newRequest = request.mutate()
                .header("user-info", userId.toString())
                .build();
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    private boolean isExclude(String path, String method) {
        if (antPathMatcher.match("/api/users/login", path) || antPathMatcher.match("/api/users/register", path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && antPathMatcher.match("/api/items/**", path)) {
            return true;
        }
        return antPathMatcher.match("/api/health/**", path) || antPathMatcher.match("/health/**", path);
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




