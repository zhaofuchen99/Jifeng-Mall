package com.situ.jifeng.good.mapper;

import com.situ.jifeng.spi.model.GoodEntity;
import com.situ.jifeng.spi.model.search.GoodSearchBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoodMapper {
    List<GoodEntity> findAll(GoodSearchBean ge);

    GoodEntity findById(Long id);

    int save(GoodEntity goodEntity);

    int update(GoodEntity goodEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 扣减库存（防超卖）：仅在剩余库存足够时扣减，返回受影响行数。
     * 对应 SQL：UPDATE good SET qty = qty - #{qty} WHERE id = #{id} AND qty >= #{qty}
     */
    int deductStock(@Param("id") Long id, @Param("qty") Integer qty);

    /**
     * 回补库存：取消订单 / 超时关单时回补库存。
     */
    int addBackStock(@Param("id") Long id, @Param("qty") Integer qty);

    /**
     * 按条件统计商品数（品牌/分类删除前的引用校验用）。
     *
     * <p>只支持引用校验需要的几个条件，故意做成独立语句而不是复用 {@code findAll} 的 where ——
     * 校验场景只要一个 count，没必要把 23 个列的实体查出来再 {@code size()}。</p>
     */
    long count(GoodSearchBean ge);
}
