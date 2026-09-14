package com.situ.jifeng.order.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.JwtUtil;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.OrderCreateDTO;
import com.situ.jifeng.spi.model.OrderEntity;
import com.situ.jifeng.spi.model.SeckillOrderDTO;
import com.situ.jifeng.spi.model.search.OrderSearchBean;
import com.situ.jifeng.spi.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderApi {
    private OrderService orderService;

    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 查询所有订单。limit值为-1时，表示查询全部数据
     *
     * @return 所有订单实体
     */
    @GetMapping
    public JsonResp findAll(OrderSearchBean osb, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<OrderEntity> orders = orderService.findAll(osb, pi);
        PageInfo<?> pageInfo = new PageInfo<>(orders);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id,
                             @RequestHeader(name = "X-User-Name", required = false) String user,
                             @RequestHeader(name = "X-Audience", required = false) String audience) {
        OrderEntity oe = orderService.findById(id);
        assertOwnership(oe, user, audience);
        return JsonResp.success(oe);
    }

    @GetMapping("/member-account/{account}")
    public JsonResp findByMemberAccount(@PathVariable String account,
                                        @RequestHeader(name = "X-User-Name", required = false) String user,
                                        @RequestHeader(name = "X-Audience", required = false) String audience) {
        // 会员只能查自己的订单列表，不能拿别人的账号来查
        assertAccountVisible(account, user, audience);
        List<OrderEntity> list = orderService.findByMemberAccount(account);
        return JsonResp.success(list);
    }

    /**
     * 按秒杀流水号查订单（秒杀异步下单幂等判断 / 会员轮询抢购结果），不存在返回 data=null
     */
    @GetMapping("/seckill-no/{seckillNo}")
    public JsonResp findBySeckillNo(@PathVariable String seckillNo,
                                    @RequestHeader(name = "X-User-Name", required = false) String user,
                                    @RequestHeader(name = "X-Audience", required = false) String audience) {
        OrderEntity order = orderService.findBySeckillNo(seckillNo);
        assertOwnership(order, user, audience);
        return JsonResp.success(order);
    }

    /**
     * 提交订单（购物车/立即购买）
     */
    @PostMapping("/create")
    public JsonResp create(@RequestBody OrderCreateDTO dto,
                           @RequestHeader(name = "X-User-Id", required = false) String userId,
                           @RequestHeader(name = "X-User-Name", required = false) String user,
                           @RequestHeader(name = "X-Audience", required = false) String audience) {
        // 会员下单只能给自己下：忽略请求体里的 memberAccount/memberId，强制用令牌里的身份。
        // 否则会员可以传别人的账号，把订单挂到他人名下。
        if (JwtUtil.AUDIENCE_MEMBER.equals(audience)) {
            dto.setMemberAccount(user);
            dto.setMemberId(parseId(userId));
        }
        OrderEntity order = orderService.create(dto);
        return JsonResp.success(order);
    }

    private Long parseId(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 取消订单（仅待付款）
     */
    @PutMapping("/{id}/cancel")
    public JsonResp cancel(@PathVariable Long id,
                           @RequestHeader(name = "X-User-Name", required = false) String user,
                           @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertOwnership(orderService.findById(id), user, audience);
        return orderService.cancel(id) ? JsonResp.success(true) : JsonResp.fail(7005, "订单状态不允许该操作");
    }

    /**
     * 确认收货（仅待收货）
     */
    @PutMapping("/{id}/confirm")
    public JsonResp confirm(@PathVariable Long id,
                            @RequestHeader(name = "X-User-Name", required = false) String user,
                            @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertOwnership(orderService.findById(id), user, audience);
        return orderService.confirm(id) ? JsonResp.success(true) : JsonResp.fail(7005, "订单状态不允许该操作");
    }

    /**
     * 后台发货（仅已支付）
     */
    @PutMapping("/{id}/ship")
    public JsonResp ship(@PathVariable Long id,
                         @RequestHeader(name = "X-Audience", required = false) String audience) {
        // 发货是后台动作，会员即使拥有该订单也不该能发货。
        // 网关已按路径拦了会员，这里再兜一道，避免只依赖网关一层。
        if (audience != null && !JwtUtil.AUDIENCE_ADMIN.equals(audience)) {
            throw new BusinessException(403, "无权限执行发货操作");
        }
        return orderService.ship(id) ? JsonResp.success(true) : JsonResp.fail(7005, "订单状态不允许该操作");
    }

    /**
     * 发起模拟支付，返回模拟收银台信息
     */
    @PostMapping("/{id}/pay")
    public JsonResp pay(@PathVariable Long id,
                        @RequestHeader(name = "X-User-Name", required = false) String user,
                        @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertOwnership(orderService.findById(id), user, audience);
        return orderService.pay(id) ? JsonResp.success(true) : JsonResp.fail(7005, "订单状态不允许该操作");
    }

    /**
     * 模拟支付确认 → 已支付
     */
    @PostMapping("/{id}/pay/confirm")
    public JsonResp confirmPay(@PathVariable Long id,
                               @RequestHeader(name = "X-User-Name", required = false) String user,
                               @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertOwnership(orderService.findById(id), user, audience);
        return orderService.confirmPay(id) ? JsonResp.success(true) : JsonResp.fail(7005, "支付确认失败");
    }

    /**
     * 秒杀下单（服务间调用：秒杀服务 → order 服务），按秒杀价创建秒杀订单
     */
    @PostMapping("/seckill")
    public JsonResp createSeckillOrder(@RequestBody SeckillOrderDTO dto) {
        OrderEntity order = orderService.createSeckillOrder(
                dto.getGoodId(), dto.getQty(), dto.getSeckillPrice(),
                dto.getMemberAccount(), dto.getSeckillNo());
        return JsonResp.success(order);
    }

    /**
     * 保存订单实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody OrderEntity oe) {
        boolean success = orderService.save(oe);
        if (success) {
            return JsonResp.success(oe);
        } else {
            return JsonResp.fail(500, "保存订单失败");
        }
    }

    /**
     * 修改订单
     *
     * @param oe 订单实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody OrderEntity oe) {
        boolean success = orderService.update(oe);
        if (success) {
            return JsonResp.success(oe);
        } else {
            return JsonResp.fail(500, "修改订单失败");
        }
    }

    /**
     * 批量删除订单
     *
     * @param ids 订单主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = orderService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }

    // ==================================================================
    // 订单归属校验（需求 7.2-3：会员只能操作本人订单）
    // ==================================================================

    /**
     * 判断一个请求是不是经网关过来的。
     *
     * <p>网关的 AuthGlobalFilter 会**无条件**注入 {@code X-Audience}（值可能是空串），
     * 而服务间的 Feign 调用完全没有这个头。所以用它来区分：</p>
     * <ul>
     *   <li>{@code audience == null} → 服务间调用（如 seckill-api 用 Feign 查
     *       {@code /seckill-no/{sno}} 做幂等判断），不做归属校验</li>
     *   <li>{@code audience == "admin"} → 后台，不受限（管理端要看/操作任意订单）</li>
     *   <li>{@code audience == "member"} → 比对订单的 memberAccount 与 X-User-Name</li>
     * </ul>
     *
     * <p>注意这里放行"无头的直连请求"。这是有意的：服务端口本就不该对外暴露，
     * 唯一的入口是网关。如果将来要收紧，应该给服务间调用加一个内部令牌，
     * 而不是靠头的有无来判断。</p>
     */
    private void assertOwnership(OrderEntity order, String user, String audience) {
        if (audience == null || JwtUtil.AUDIENCE_ADMIN.equals(audience)) {
            return;
        }
        // order 为 null（订单不存在）时不拦，交给各自的业务逻辑返回 null / 状态码，
        // 否则会员抢购轮询 seckill-no 时订单尚未生成会拿到 403 而不是 null。
        if (order == null) {
            return;
        }
        if (user == null || !user.equals(order.getMemberAccount())) {
            throw new BusinessException(403, "无权访问他人订单");
        }
    }

    /** 会员只能查自己账号的订单；后台不受限 */
    private void assertAccountVisible(String account, String user, String audience) {
        if (audience == null || JwtUtil.AUDIENCE_ADMIN.equals(audience)) {
            return;
        }
        if (user == null || !user.equals(account)) {
            throw new BusinessException(403, "无权查看他人订单");
        }
    }
}
