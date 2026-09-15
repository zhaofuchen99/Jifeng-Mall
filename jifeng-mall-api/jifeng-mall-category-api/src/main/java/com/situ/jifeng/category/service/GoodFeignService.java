package com.situ.jifeng.category.service;

import com.situ.jifeng.common.TypedJsonResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 调 good-api 统计「引用了某个分类的商品数」（设计文档 5.3「删除校验商品引用」）。
 *
 * <p><b>为什么由 category-api 反向调 good-api</b>：正常依赖方向是 good-api → category-api
 * （商品组装分类信息）。但"删除前校验引用"必须由被引用方问引用方，方向天然是反的。
 * 选择 Feign 而不是让 category-api 直接查 {@code good} 表 —— 后者虽然能跑
 * （本项目所有服务共用一个库），但跨服务直接读别人的表会破坏分层。</p>
 *
 * <p><b>降级即拒绝</b>：good-api 不可用时 fallback 返回失败，
 * {@code CategoryServiceImpl.deleteByIds} 据此拒绝删除（fail-closed）。</p>
 */
@FeignClient(value = "jifeng-mall-good-api", contextId = "category-good",
        fallback = GoodFeignServiceFallback.class)
public interface GoodFeignService {

    /**
     * @param categoryId 分类编号
     * @param isDel      必须显式传 false —— good-api 的 count 不传 isDel 时不过滤逻辑删除，
     *                   会把已删除的商品也算成引用，导致分类永远删不掉
     */
    @GetMapping("/api/goods/count")
    TypedJsonResp<Long> countGoodsByCategory(@RequestParam("categoryId") Long categoryId,
                                             @RequestParam("isDel") Boolean isDel);
}

/** 降级：返回失败，让调用方走「校验不了就不删」的分支 */
@Component
class GoodFeignServiceFallback implements GoodFeignService {
    @Override
    public TypedJsonResp<Long> countGoodsByCategory(Long categoryId, Boolean isDel) {
        return TypedJsonResp.fail(503, "商品服务不可用");
    }
}
