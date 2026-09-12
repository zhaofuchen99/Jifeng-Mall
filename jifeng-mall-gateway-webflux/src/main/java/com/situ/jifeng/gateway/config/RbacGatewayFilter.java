package com.situ.jifeng.gateway.config;

import com.situ.jifeng.common.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * 网关 RBAC 权限判定（依据设计文档 6.1）。
 *
 * <p>对后台接口（admin 令牌、非公开路径），调用 rbac-api 的 {@code /api/rbac/check}
 * 按 userId + path + method 判定，未授权返回 403。</p>
 *
 * <p><b>修过的两个问题：</b></p>
 * <ol>
 *   <li><b>调用根本发不出去。</b>原先用 {@code WebClient.builder().baseUrl("http://jifeng-mall-rbac-api")}
 *       直接构建，baseUrl 是服务名却没有负载均衡能力 → 主机解析失败 → 异常 →
 *       被 {@code onErrorReturn(true)} 静默放行。等于 RBAC 完全没生效：
 *       {@code operator} 这种零授权账号照样能读全部后台数据。
 *       现改为注入 {@link LoadBalancedWebClientConfig} 里那个 {@code @LoadBalanced} 的 Builder，
 *       并把 WebClient 构建一次缓存起来（原先每次请求都新建一个）。</li>
 *   <li><b>失败放行。</b>鉴权查询失败时放行，等于把权限校验整个关掉——而"服务没起来"
 *       恰恰是最常见的故障场景。现改为 <b>fail-closed</b>：判定不出来就拒绝。
 *       但用 <b>503</b> 而不是 403，把"权限服务不可用"和"你没有权限"区分开，
 *       免得运维排查时把故障误读成越权。</li>
 * </ol>
 */
@Component
public class RbacGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RbacGatewayFilter.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String RBAC_SERVICE_ID = "jifeng-mall-rbac-api";
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 不参与 RBAC 判定的公开路径前缀 */
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/goods", "/api/categories", "/api/brands", "/api/regions",
            "/api/seckills", "/upload", "/api/orders/id",
            "/api/members/login", "/api/members/register", "/api/users/login",
            "/api/menus/mine"
    );

    private WebClient rbacClient;

    @Autowired
    public void setWebClientBuilder(@Qualifier("loadBalancedWebClientBuilder") WebClient.Builder builder) {
        // 构建一次即可，WebClient 是线程安全的
        this.rbacClient = builder.baseUrl("http://" + RBAC_SERVICE_ID).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() == null ? "" : request.getMethod().name();

        // admin 之外（member / 公开）不做后台 RBAC 判定
        String auth = request.getHeaders().getFirst(AUTH_HEADER);
        Claims claims = null;
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            try {
                claims = JwtUtil.parse(auth.substring(BEARER_PREFIX.length()));
            } catch (Exception ignored) {
                // JWT 已在 AuthGlobalFilter 校验；此处解析失败按无权限处理
            }
        }
        String audience = claims == null ? null : claims.get("audience", String.class);
        Long userId = claims == null ? null : claims.get("userId", Long.class);

        // 非 admin 或不涉及后台资源，直接放行
        if (!JwtUtil.AUDIENCE_ADMIN.equals(audience) || userId == null || isPublic(path)) {
            return chain.filter(exchange);
        }

        String checkBody = "{\"userId\":" + userId + ",\"path\":\"" + path + "\",\"method\":\"" + method + "\"}";

        // empty = 判定不出来（服务不可用）；of(true/false) = 明确的判定结果
        Mono<Optional<Boolean>> decision = rbacClient.post()
                .uri("/api/rbac/check")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(checkBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(resp -> Optional.of(resp != null && resp.contains("\"allowed\":true")))
                .onErrorResume(e -> {
                    log.error("RBAC 判定调用失败，按拒绝处理：userId={}, path={}, cause={}",
                            userId, path, e.toString());
                    return Mono.just(Optional.empty());
                });

        return decision.flatMap(opt -> {
            if (opt.isEmpty()) {
                return rbacUnavailable(exchange);
            }
            if (Boolean.TRUE.equals(opt.get())) {
                return chain.filter(exchange);
            }
            log.warn("越权访问被拒绝：userId={}, path={}, method={}", userId, path, method);
            return forbidden(exchange);
        });
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(p -> MATCHER.match(p, path));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        return write(exchange, HttpStatus.FORBIDDEN,
                "{\"code\":403,\"success\":false,\"msg\":\"无权限访问\",\"data\":null}");
    }

    /**
     * 权限服务不可用。用 503 而不是 403，是为了让「权限服务挂了」和「你确实没权限」
     * 在监控与排查时能区分开。
     */
    private Mono<Void> rbacUnavailable(ServerWebExchange exchange) {
        return write(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                "{\"code\":503,\"success\":false,\"msg\":\"权限服务暂时不可用，请稍后重试\",\"data\":null}");
    }

    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String body) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -90; // 在 AuthGlobalFilter(-100) 之后执行
    }
}
