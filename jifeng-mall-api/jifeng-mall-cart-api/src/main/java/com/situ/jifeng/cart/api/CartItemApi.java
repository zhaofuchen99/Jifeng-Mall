package com.situ.jifeng.cart.api;

import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.JwtUtil;
import com.situ.jifeng.spi.model.CartItemEntity;
import com.situ.jifeng.spi.service.CartItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车接口。
 *
 * <p><b>归属校验</b>：会员只能操作自己的购物车。原先所有接口都不校验，
 * 而购物车主键是自增的，会员把 id 从 1 往上遍历就能读改删别人的购物车。</p>
 *
 * <p>判定规则（与 order-api / member-api 一致，三选一放行）：</p>
 * <ol>
 *   <li><b>无 {@code X-Audience} 头</b> → 服务间 Feign 调用，放行。
 *       order-api 下单时会用 Feign 调 {@code /api/carts/id/{id}}（校验条目）
 *       和 {@code DELETE /api/carts}（下单后清空），不能误伤</li>
 *   <li>{@code X-Audience=admin} → 后台，放行</li>
 *   <li>{@code X-Audience=member} → 比对 {@code X-User-Id} 与条目的 memberId</li>
 * </ol>
 */
@RestController
@RequestMapping(value = "/api/carts", produces = MediaType.APPLICATION_JSON_VALUE)
public class CartItemApi {
    private CartItemService cartItemService;

    @Autowired
    public void setCartItemService(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id,
                             @RequestHeader(name = "X-User-Id", required = false) String userId,
                             @RequestHeader(name = "X-Audience", required = false) String audience) {
        CartItemEntity ce = cartItemService.findById(id);
        assertOwned(ce, userId, audience);
        return JsonResp.success(ce);
    }

    //查询指定会员的所有购物车条目
    @GetMapping("/member/{memberId}")
    public JsonResp findByMemberId(@PathVariable Long memberId,
                                   @RequestHeader(name = "X-User-Id", required = false) String userId,
                                   @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertSameMember(memberId, userId, audience);
        List<CartItemEntity> ces = cartItemService.findByMemberId(memberId);
        return JsonResp.success(ces);
    }

    /**
     * 保存购物车条目实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody CartItemEntity ce,
                         @RequestHeader(name = "X-User-Id", required = false) String userId,
                         @RequestHeader(name = "X-Audience", required = false) String audience) {
        forceCaller(ce, userId, audience);
        boolean success = cartItemService.save(ce);
        if (success) {
            return JsonResp.success(ce);
        } else {
            return JsonResp.fail(500, "保存购物车条目失败");
        }
    }

    /**
     * 加入购物车（合并逻辑）：按会员 + 商品合并累加数量
     *
     * @return 合并后的购物车条目
     */
    @PostMapping("/save-merge")
    public JsonResp saveAndMerge(@RequestBody CartItemEntity ce,
                                 @RequestHeader(name = "X-User-Id", required = false) String userId,
                                 @RequestHeader(name = "X-Audience", required = false) String audience) {
        forceCaller(ce, userId, audience);
        CartItemEntity merged = cartItemService.saveAndMerge(ce);
        return JsonResp.success(merged);
    }

    /**
     * 修改购物车条目
     *
     * @param ce 购物车条目实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody CartItemEntity ce,
                           @RequestHeader(name = "X-User-Id", required = false) String userId,
                           @RequestHeader(name = "X-Audience", required = false) String audience) {
        if (isMember(audience)) {
            // 先确认这一行是自己的，再强制归属，防止借 update 把条目改挂到别人名下
            assertOwned(cartItemService.findById(ce.getId()), userId, audience);
            forceCaller(ce, userId, audience);
        }
        boolean success = cartItemService.update(ce);
        if (success) {
            return JsonResp.success(ce);
        } else {
            return JsonResp.fail(500, "修改购物车条目失败");
        }
    }

    /**
     * 批量删除购物车条目
     *
     * @param ids 购物车主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids,
                                @RequestHeader(name = "X-User-Id", required = false) String userId,
                                @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertAllOwned(List.of(ids), userId, audience);
        int rows = cartItemService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }

    /**
     * 下单后批量清空购物车条目（按购物车主键删除）
     *
     * @param ids 购物车主键集合
     * @return 删除条数
     */
    @DeleteMapping("/clear")
    public JsonResp clearByIds(@RequestBody Long[] ids,
                               @RequestHeader(name = "X-User-Id", required = false) String userId,
                               @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertAllOwned(List.of(ids), userId, audience);
        int rows = cartItemService.clearByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }

    /**
     * 下单后按商品清空某会员的购物车条目
     *
     * @param memberId 会员编号
     * @param goodIds  商品编号集合（逗号分隔）
     * @return 删除条数
     */
    @DeleteMapping("/remove-by-goods")
    public JsonResp removeByGoodIds(@RequestParam Long memberId, @RequestParam List<Long> goodIds,
                                    @RequestHeader(name = "X-User-Id", required = false) String userId,
                                    @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertSameMember(memberId, userId, audience);
        int rows = cartItemService.removeByGoodIds(memberId, goodIds);

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }

    // ==================================================================
    // 归属校验
    // ==================================================================

    /** 会员请求才校验；无 X-Audience 头 = 服务间调用，admin 也不受限 */
    private boolean isMember(String audience) {
        return JwtUtil.AUDIENCE_MEMBER.equals(audience);
    }

    private Long callerId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 单个条目：会员只能碰自己的 */
    private void assertOwned(CartItemEntity row, String userId, String audience) {
        if (!isMember(audience)) {
            return;
        }
        Long me = callerId(userId);
        if (row != null && (me == null || !me.equals(row.getMemberId()))) {
            throw new BusinessException(403, "无权操作他人购物车");
        }
    }

    /** 批量：列表里的每一个都必须属于调用者 */
    private void assertAllOwned(List<Long> ids, String userId, String audience) {
        if (!isMember(audience)) {
            return;
        }
        Long me = callerId(userId);
        if (me == null) {
            throw new BusinessException(403, "无权操作他人购物车");
        }
        List<CartItemEntity> mine = cartItemService.findByMemberId(me);
        List<Long> mineIds = mine == null ? List.of() : mine.stream().map(CartItemEntity::getId).toList();
        for (Long id : ids) {
            if (!mineIds.contains(id)) {
                throw new BusinessException(403, "无权操作他人购物车");
            }
        }
    }

    /** 路径/参数里直接给了 memberId 的接口：必须是调用者本人 */
    private void assertSameMember(Long memberId, String userId, String audience) {
        if (!isMember(audience)) {
            return;
        }
        Long me = callerId(userId);
        if (me == null || !me.equals(memberId)) {
            throw new BusinessException(403, "无权访问他人购物车");
        }
    }

    /**
     * 新增/合并时把归属强制成调用者本人。
     * 不做的话，会员只要在请求体里填别人的 memberId，就能往他人购物车里塞商品。
     */
    private void forceCaller(CartItemEntity ce, String userId, String audience) {
        if (!isMember(audience)) {
            return;
        }
        Long me = callerId(userId);
        if (me == null) {
            throw new BusinessException(403, "无权操作他人购物车");
        }
        ce.setMemberId(me);
    }
}
