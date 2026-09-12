package com.situ.jifeng.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 网关内部发起服务间调用用的 WebClient。
 *
 * <p><b>补的什么坑：</b>{@link RbacGatewayFilter} 原先直接写
 * {@code WebClient.builder().baseUrl("http://jifeng-mall-rbac-api").build()} ——
 * baseUrl 用的是<b>服务名</b>，但这个 WebClient 没有负载均衡能力，
 * 解析不了该主机名，调用必然抛异常，然后被 {@code onErrorReturn(true)} 静默放行。
 * 结果是整个 RBAC 过滤器形同虚设：任何 admin 令牌都能访问全部后台接口。</p>
 *
 * <p>加上 {@code @LoadBalanced} 之后，WebClient 会经过 Spring Cloud LoadBalancer
 * 把服务名解析成 Nacos 里注册的真实实例地址。</p>
 *
 * <p>注意：这个 Bean 一旦存在，Spring Boot 自动配置里那个同类型的普通
 * {@code WebClient.Builder} 就会因 {@code @ConditionalOnMissingBean} 而不再创建。
 * 网关里没有别的地方用 WebClient.Builder（路由走的是 reactor-netty 的 HttpClient），
 * 所以没有影响。</p>
 */
@Configuration
public class LoadBalancedWebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
