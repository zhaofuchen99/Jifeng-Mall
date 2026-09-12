package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.OrderCreateDTO;
import com.situ.jifeng.spi.model.OrderEntity;
import com.situ.jifeng.spi.model.search.OrderSearchBean;

import java.util.List;

public interface OrderService {
    List<OrderEntity> findAll(OrderSearchBean osb, PaginateInfo pi);

    OrderEntity findById(Long id);

    OrderEntity findByOrderNo(String orderNo);

    /** 按秒杀流水号查订单（秒杀异步下单幂等判断用），不存在返回 null */
    OrderEntity findBySeckillNo(String seckillNo);

    List<OrderEntity> findByMemberAccount(String memberAccount);

    boolean save(OrderEntity orderEntity);

    boolean update(OrderEntity orderEntity);

    int deleteByIds(List<Long> ids);

    /** 下单：购物车/立即购买 → 创建订单 + 明细 + 扣库存 + 清购物车（事务） */
    OrderEntity create(OrderCreateDTO dto);

    /** 取消订单（仅待付款），恢复库存 */
    boolean cancel(Long orderId);

    /** 确认收货（仅待收货） */
    boolean confirm(Long orderId);

    /** 发货（仅已支付，后台操作） */
    boolean ship(Long orderId);

    /** 发起模拟支付 */
    boolean pay(Long orderId);

    /** 模拟支付确认 → 已支付 */
    boolean confirmPay(Long orderId);

    /** 秒杀下单（内部服务调用）：按秒杀价创建秒杀订单并关联秒杀流水号 */
    OrderEntity createSeckillOrder(Long goodId, Integer qty, java.math.BigDecimal seckillPrice,
                                   String memberAccount, String seckillNo);
}