package com.situ.jifeng.order.mapper;

import com.situ.jifeng.spi.model.OrderEntity;
import com.situ.jifeng.spi.model.search.OrderSearchBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    List<OrderEntity> findAll(OrderSearchBean osb);

    OrderEntity findById(Long id);

    OrderEntity findByOrderNo(String orderNo);

    OrderEntity findBySeckillNo(String seckillNo);

    int save(OrderEntity orderEntity);

    int update(OrderEntity orderEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 查出「超时未支付、需要兜底关闭」的订单主键（超时关单的定时扫描用）。
     *
     * <p>只返回 id 不返回整个实体：兜底扫描可能一次命中几十上百单，
     * 没必要把这些字段全捞进内存；后续逐单调 {@code cancel()} 时它自己会再查一次。</p>
     *
     * @param status   目标状态（传 {@code 待付款}，不写死在 XML 里以免与状态字典脱节）
     * @param deadline 创建时间早于该时刻的订单才算超时
     * @param limit    单次最多取多少条，防止一次扫出过多堆积
     * @return 订单 id 列表，按创建时间升序（先超时的先处理）
     */
    List<Long> findTimeoutOrderIds(@Param("status") String status,
                                   @Param("deadline") LocalDateTime deadline,
                                   @Param("limit") int limit);
}