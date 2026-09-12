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
 * 网关权限判定（依据设计文档 6.1）。
 *
 * <p>请求按身份分成四类处理：</p>
 * <ol>
 *   <li><b>公开路径</b>：游客即可访问，直接放行。注意公开只对 <b>GET</b> 生效
 *       （另加登录/注册/上传）——否则 {@code POST /api/goods} 也会被当成公开，
 *       会员就能建商品了</li>
 *   <li><b>会员路径</b>：member 令牌可访问。这些接口由各自服务按
 *       {@code X-User-Name}（网关注入）限定本人数据</li>
 *   <li><b>后台自身路径</b>：{@code /api/menus/mine}，任何 admin 令牌都可访问</li>
 *   <li><b>其余后台接口</b>：调 rbac-api 的 {@code /api/rbac/check} 判定，未授权 403</li>
 * </ol>
 *
 * <p><b>这里修过三个问题：</b></p>
 * <ol>
 *   <li><b>RBAC 调用根本发不出去。</b>原先用
 *       {@code WebClient.builder().baseUrl("http://jifeng-mall-rbac-api")} 直接构建，
 *       baseUrl 是服务名却没有负载均衡能力 → 主机解析失败 → 异常 →
 *       被 {@code onErrorReturn(true)} 静默放行。等于 RBAC 完全没生效：
 *       {@code operator} 这种零授权账号照样能读全部后台数据（实测 14 个接口全泄漏）。
 *       现改为注入 {@link LoadBalancedWebClientConfig} 里那个 {@code @LoadBalanced} 的 Builder，
 *       并把 WebClient 构建一次缓存（原先每次请求都新建一个）。</li>
 *   <li><b>失败放行。</b>鉴权查询失败时放行，等于把权限校验整个关掉——而"服务没起来"
 *       恰恰是最常见的故障场景。现改为 <b>fail-closed</b>，但用 <b>503</b> 而不是 403，
 *       把"权限服务不可用"与"你没有权限"区分开。</li>
 *   <li><b>会员令牌畅通无阻。</b>原先的判断是「非 admin 就放行」，RBAC 只对 admin 生效，
 *       于是任何在前台注册的会员拿着合法 member 令牌就能调后台全部接口。
 *       现改为：会员只能走 {@link #MEMBER_PATHS} 里列出的会员业务接口，其余一律拒绝。
 *       <b>注意不能简单地"让会员也走 RBAC 判定"</b>：member 表与 user 表的 id 是两套
 *       独立空间，拿会员 userId 去查 t_rbac_user_group 会错位——id=1 的会员会撞上
 *       id=1 的管理员，反而被授成超管。</li>
 * </ol>
 */
@Component
public class RbacGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RbacGatewayFilter.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String RBAC_SERVICE_ID = "jifeng-mall-rbac-api";
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 不分方法一律公开：登录、注册、文件读取 */
    private static final List<String> PUBLIC_ANY_METHOD = List.of(
            "/api/members/login",
            "/api/members/register",
            "/api/users/login",
            "/upload/**"
    );

    /**
     * 仅 GET 公开：前台商城游客要能浏览。
     * 写操作（POST/PUT/DELETE）不在此列，仍要过权限判定。
     */
    private static final List<String> PUBLIC_GET_ONLY = List.of(
            "/api/goods", "/api/goods/**",
            "/api/categories", "/api/categories/**",
            "/api/brands", "/api/brands/**",
            "/api/regions", "/api/regions/**",
            "/api/seckills", "/api/seckills/**",
            "/api/seckill-goods", "/api/seckill-goods/**"
    );

    /**
     * 会员业务接口：member 令牌可用。
     *
     * <p>这些接口自身按 {@code X-User-Name} 限定本人数据。列得很细是故意的——
     * 订单只放开会员真正用得到的那几个，<b>不放开裸的 {@code /api/orders}</b>，
     * 因为那是后台的订单列表接口。</p>
     */
    private static final List<String> MEMBER_PATHS = List.of(
            "/api/carts/**",
            "/api/member-addresses/**",
            "/api/members/id/**",
            "/api/members/account/**",
            "/api/orders/create",
            "/api/orders/member-account/**",
            "/api/orders/id/**",
            "/api/orders/seckill-no/**",
            "/api/orders/*/pay",
            "/api/orders/*/pay/confirm",
            "/api/orders/*/cancel",
            "/api/orders/*/confirm",
            "/api/order-items/order/**",
            "/api/seckills/grab"
    );

    /**
     * 后台自身接口：任何 admin 令牌都能访问，不需要资源授权。
     * {@code /api/menus/mine} 只返回调用者自己的菜单树，不涉及他人数据。
     */
    private static final List<String> ADMIN_SELF_PATHS = List.of("/api/menus/mine");

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

        // 1) 不分方法的公开路径
        if (matches(PUBLIC_ANY_METHOD, path)) {
            return chain.filter(exchange);
        }
        // 2) 只读的公开路径
        if ("GET".equalsIgnoreCase(method) && matches(PUBLIC_GET_ONLY, path)) {
            return chain.filter(exchange);
        }

        // 取令牌身份（JWT 已在 AuthGlobalFilter(-100) 校验过）
        String auth = request.getHeaders().getFirst(AUTH_HEADER);
        Claims claims = null;
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            try {
                claims = JwtUtil.parse(auth.substring(BEARER_PREFIX.length()));
            } catch (Exception ignored) {
                // 解析失败按无身份处理，走下面的拒绝分支
            }
        }
        String audience = claims == null ? null : claims.get("audience", String.class);
        Long userId = claims == null ? null : claims.get("userId", Long.class);

        // 3) 非管理员：只能走会员业务接口
        if (!JwtUtil.AUDIENCE_ADMIN.equals(audience)) {
            if (userId != null && matches(MEMBER_PATHS, path)) {
                return chain.filter(exchange);
            }
            log.warn("非管理员令牌访问后台接口被拒绝：audience={}, path={}, method={}",
                    audience, path, method);
            return forbidden(exchange);
        }

        // 4) 管理员的后台自身接口
        if (matches(ADMIN_SELF_PATHS, path)) {
            return chain.filter(exchange);
        }

        // 5) 其余后台接口 → 调 rbac-api 判定
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

    private boolean matches(List<String> patterns, String path) {
        return patterns.stream().anyMatch(p -> MATCHER.match(p, path));
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
