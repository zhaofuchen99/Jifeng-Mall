package com.situ.jifeng.gateway.config;

import com.situ.jifeng.common.JwtUtil;
import io.jsonwebtoken.Claims;
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

/**
 * 网关 JWT 鉴权过滤器（依据设计文档 5.8 / 6.1）。
 *
 * <p>职责：
 * <ul>
 *   <li>公开接口（白名单）放行</li>
 *   <li>其余请求校验 {@code Authorization: Bearer {token}}，非法/过期返回 401</li>
 *   <li>解析身份注入请求头 {@code X-User-Id / X-User-Name / X-Audience}</li>
 * </ul>
 * </p>
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /** 公开白名单（Ant 风格），依据设计文档 4.1 */
    private static final List<String> WHITE_LIST = List.of(
            // 登录 / 注册
            "/api/members/login",
            "/api/members/register",
            "/api/users/login",
            // 商品 / 分类查询
            "/api/goods",
            "/api/goods/**",
            "/api/categories/**",
            "/api/brands/**",
            // 地区查询
            "/api/regions/**",
            // 秒杀活动查询（GET）
            "/api/seckills",
            "/api/seckills/**",
            // 秒杀商品查询（GET）——原先漏了这一条，导致游客打开首页/秒杀会场时
            // 取不到秒杀商品列表（直接被判 401），与接口文档 2 节「seckill-goods 查询公开」不符。
            "/api/seckill-goods",
            "/api/seckill-goods/**",
            // 文件读取
            "/upload/**"
    );

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 公开接口放行
        String method = request.getMethod() == null ? "" : request.getMethod().name();
        if (isWhitelist(path, method)) {
            return chain.filter(exchange);
        }

        // 获取令牌
        String auth = request.getHeaders().getFirst(AUTH_HEADER);
        String token = null;
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            token = auth.substring(BEARER_PREFIX.length());
        }
        if (token == null || token.isBlank()) {
            return unauthorized(exchange, "未登录或令牌缺失");
        }

        Claims claims;
        try {
            claims = JwtUtil.parse(token);
        } catch (Exception e) {
            return unauthorized(exchange, "令牌无效或已过期");
        }

        // 解析身份并注入请求头
        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();
        String audience = claims.get("audience", String.class);
        ServerHttpRequest mutated = request.mutate()
                .header("X-User-Id", userId == null ? "" : String.valueOf(userId))
                .header("X-User-Name", username == null ? "" : username)
                .header("X-Audience", audience == null ? "" : audience)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isWhitelist(String path, String method) {
        boolean pathMatch = WHITE_LIST.stream().anyMatch(p -> matcher.match(p, path));
        // 商品/分类/秒杀查询在 GET 下公开，写操作仍需鉴权（由 RBAC 或归属校验处理）
        if (pathMatch && ("GET".equalsIgnoreCase(method) || path.startsWith("/upload/"))) {
            return true;
        }
        // 精确 POST 登录/注册白名单
        return pathMatch && isLoginOrRegister(path);
    }

    private boolean isLoginOrRegister(String path) {
        return path.endsWith("/login") || path.endsWith("/register");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"success\":false,\"msg\":\"" + msg + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
