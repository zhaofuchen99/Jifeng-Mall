package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.GoodEntity;
import com.situ.jifeng.spi.model.search.GoodSearchBean;

import java.util.List;

public interface GoodService {
    List<GoodEntity> findAll(GoodSearchBean ge, PaginateInfo pi);

    GoodEntity findById(Long id);

    boolean save(GoodEntity goodEntity);

    boolean update(GoodEntity goodEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 扣减库存（防超卖，仅库存充足时成功）。
     *
     * @return true 扣减成功
     */
    boolean deductStock(Long id, Integer qty);

    /**
     * 回补库存（取消订单/超时关单时）。
     */
    boolean addBackStock(Long id, Integer qty);

    /**
     * 按条件统计商品数量（品牌/分类<b>删除前的引用校验</b>用，设计文档 5.3）。
     *
     * <p>调用方（brand-api / category-api）传 {@code brandId} 或 {@code categoryId}，
     * 并应显式传 {@code isDel=false} —— {@code GoodMapper.findAll} 默认<b>不</b>过滤逻辑删除，
     * 不传会把已逻辑删除的商品也算成引用。</p>
     *
     * @return 命中条数
     */
    long count(GoodSearchBean ge);
}
