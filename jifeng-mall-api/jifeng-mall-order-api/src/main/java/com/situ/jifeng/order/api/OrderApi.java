package com.situ.jifeng.order.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
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
    public JsonResp findById(@PathVariable Long id) {
        OrderEntity oe = orderService.findById(id);
        return JsonResp.success(oe);
    }

    @GetMapping("/member-account/{account}")
    public JsonResp findByMemberAccount(@PathVariable String account) {
        List<OrderEntity> list = orderService.findByMemberAccount(account);
        return JsonResp.success(list);
    }

    /**
     * 按秒杀流水号查订单（服务间调用：秒杀异步下单幂等判断），不存在返回 data=null
     */
    @GetMapping("/seckill-no/{seckillNo}")
    public JsonResp findBySeckillNo(@PathVariable String seckillNo) {
        return JsonResp.success(orderService.findBySeckillNo(seckillNo));
    }

    /**
     * 提交订单（购物车/立即购买）
     */
    @PostMapping("/create")
    public JsonResp create(@RequestBody OrderCreateDTO dto) {
        OrderEntity order = orderService.create(dto);
        return JsonResp.success(order);
    }

    /**
     * 取消订单（仅待付款）
     */
    @PutMapping("/{id}/cancel")
    public JsonResp cancel(@PathVariable Long id) {
        return orderService.cancel(id) ? JsonResp.success(true) : JsonResp.fail(7005, "订单状态不允许该操作");
    }

    /**
     * 确认收货（仅待收货）
     */
    @PutMapping("/{id}/confirm")
    public JsonResp confirm(@PathVariable Long id) {
        return orderService.confirm(id) ? JsonResp.success(true) : JsonResp.fail(7005, "订单状态不允许该操作");
    }

    /**
     * 后台发货（仅已支付）
     */
    @PutMapping("/{id}/ship")
    public JsonResp ship(@PathVariable Long id) {
        return orderService.ship(id) ? JsonResp.success(true) : JsonResp.fail(7005, "订单状态不允许该操作");
    }

    /**
     * 发起模拟支付，返回模拟收银台信息
     */
    @PostMapping("/{id}/pay")
    public JsonResp pay(@PathVariable Long id) {
        return orderService.pay(id) ? JsonResp.success(true) : JsonResp.fail(7005, "订单状态不允许该操作");
    }

    /**
     * 模拟支付确认 → 已支付
     */
    @PostMapping("/{id}/pay/confirm")
    public JsonResp confirmPay(@PathVariable Long id) {
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
}