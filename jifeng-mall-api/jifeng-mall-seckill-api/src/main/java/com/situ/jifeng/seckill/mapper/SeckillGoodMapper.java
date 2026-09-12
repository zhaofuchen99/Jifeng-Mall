package com.situ.jifeng.seckill.mapper;

import com.situ.jifeng.spi.model.SeckillGoodEntity;
import com.situ.jifeng.spi.model.search.SeckillGoodSearchBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SeckillGoodMapper {
    List<SeckillGoodEntity> findAll(SeckillGoodSearchBean sgs);

    SeckillGoodEntity findById(Long id);

    int save(SeckillGoodEntity seckillGoodEntity);

    int update(SeckillGoodEntity seckillGoodEntity);

    int deleteByIds(List<Long> ids);

    List<SeckillGoodEntity> findBySeckillId(Long seckillId);

    /**
     * 回补秒杀库存：stock + qty、sold - qty（超时关单回补），返回受影响行数。
     */
    int increaseStock(@Param("id") Long id, @Param("qty") Integer qty);

    /**
     * 成交扣减秒杀库存（DB 兜底防超卖）：sold + qty、stock - qty。
     * 仅当剩余库存充足（stock &gt;= qty）时生效，返回受影响行数（0 表示库存不足）。
     */
    int decreaseStock(@Param("id") Long id, @Param("qty") Integer qty);
}
