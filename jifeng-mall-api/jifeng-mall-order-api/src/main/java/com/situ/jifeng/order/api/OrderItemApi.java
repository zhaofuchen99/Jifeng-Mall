package com.situ.jifeng.order.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.JwtUtil;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.order.service.GoodFeignService;
import com.situ.jifeng.spi.model.GoodEntity;
import com.situ.jifeng.spi.model.OrderEntity;
import com.situ.jifeng.spi.model.OrderItemEntity;
import com.situ.jifeng.spi.model.search.OrderItemSearchBean;
import com.situ.jifeng.spi.service.OrderItemService;
import com.situ.jifeng.spi.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/order-items", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderItemApi {
    private OrderItemService orderItemService;

    private GoodFeignService goodFeignService;

    @Autowired
    public void setOrderItemService(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @Autowired
    public void setGoodFeignService(GoodFeignService goodFeignService) {
        this.goodFeignService = goodFeignService;
    }

    /** 校验订单明细的归属时要用它反查订单的主人 */
    private OrderService orderService;

    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 查询所有订单项。limit值为-1时，表示查询全部数据
     *
     * @return 所有订单项实体
     */
    @GetMapping
    public JsonResp findAll(OrderItemSearchBean oie, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize,
                            @RequestParam(defaultValue = "false") Boolean full) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<OrderItemEntity> orderItems = orderItemService.findAll(oie, pi);
        if (Boolean.TRUE.equals(full)) {
            orderItems.forEach(this::makeFull);
        }
        PageInfo<?> pageInfo = new PageInfo<>(orderItems);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id, @RequestParam(defaultValue = "false") Boolean full) {
        OrderItemEntity oie = orderItemService.findById(id);
        if (oie != null && Boolean.TRUE.equals(full)) {
            makeFull(oie);
        }
        return JsonResp.success(oie);
    }

    private void makeFull(OrderItemEntity orderItem) {
        if (orderItem != null && orderItem.getGoodId() != null && orderItem.getGood() == null) {
            TypedJsonResp<GoodEntity> resp = goodFeignService.findById(orderItem.getGoodId(), true);
            if (resp.isSuccess() && resp.getData() != null) {
                GoodEntity good = resp.getData();
                orderItem.setGood(good);
            }
        }
    }

    //查询指定订单的所有订单项
    @GetMapping("/order/{orderId}")
    public JsonResp findByOrderId(@PathVariable Long orderId,
                                  @RequestHeader(name = "X-User-Name", required = false) String user,
                                  @RequestHeader(name = "X-Audience", required = false) String audience) {
        // 订单明细里带着商品快照和成交价，同样属于订单主人。不校验的话，
        // 会员把 orderId 从 1 往上遍历就能读到所有人的购买记录。
        //
        // 判定规则与 OrderApi.assertOwnership 一致：无 X-Audience 头视为服务间调用放行，
        // admin 放行，member 则要求订单的 memberAccount 等于 X-User-Name。
        if (audience != null && !JwtUtil.AUDIENCE_ADMIN.equals(audience)) {
            OrderEntity order = orderService.findById(orderId);
            if (order != null && (user == null || !user.equals(order.getMemberAccount()))) {
                throw new BusinessException(403, "无权查看他人订单明细");
            }
        }
        List<OrderItemEntity> items = orderItemService.findByOrderId(orderId);
        return JsonResp.success(items);
    }

    /**
     * 保存订单项实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody OrderItemEntity oie) {
        boolean success = orderItemService.save(oie);
        if (success) {
            return JsonResp.success(oie);
        } else {
            return JsonResp.fail(500, "保存订单项失败");
        }
    }

    /**
     * 修改订单项
     *
     * @param oie 订单项实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody OrderItemEntity oie) {
        boolean success = orderItemService.update(oie);
        if (success) {
            return JsonResp.success(oie);
        } else {
            return JsonResp.fail(500, "修改订单项失败");
        }
    }

    /**
     * 批量删除订单项
     *
     * @param ids 订单项主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = orderItemService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}