package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.SeckillEntity;
import com.situ.jifeng.spi.model.SeckillGrabResult;
import com.situ.jifeng.spi.model.search.SeckillSearchBean;

import java.util.List;

public interface SeckillService {
    List<SeckillEntity> findAll(SeckillSearchBean ss, PaginateInfo pi);

    SeckillEntity findById(Long id);

    boolean save(SeckillEntity seckillEntity);

    boolean update(SeckillEntity seckillEntity);

    int deleteByIds(List<Long> ids);

    //查进行中/即将开始的活动（enabled=1 且未彻底结束）
    List<SeckillEntity> findActive();

    /**
     * 秒杀抢购入口（高频请求）。
     * 校验活动时间窗/启用 → Redis + Lua 原子扣减库存（防超卖）→ 记录已抢（防重复）→ 发异步下单消息。
     *
     * @param seckillGoodId 秒杀活动商品 id
     * @param memberId      会员主键
     * @param memberAccount 会员账号
     * @return 抢购结果（orderNo 供前端轮询）
     */
    SeckillGrabResult grab(Long seckillGoodId, Long memberId, String memberAccount);

    /**
     * 秒杀订单超时关单后的库存回补（服务间调用，依据设计文档 5.6）。
     * 回补：Redis {@code seckill:stock:{seckillGoodId}} + {@code seckill_good.stock/sold}。
     *
     * @param seckillNo 秒杀流水号（用于反查 seckillGoodId）
     */
    void restockSeckillOrder(String seckillNo);
}
