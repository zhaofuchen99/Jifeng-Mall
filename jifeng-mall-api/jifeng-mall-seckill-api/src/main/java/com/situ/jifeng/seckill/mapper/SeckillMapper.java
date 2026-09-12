package com.situ.jifeng.seckill.mapper;

import com.situ.jifeng.spi.model.SeckillEntity;
import com.situ.jifeng.spi.model.search.SeckillSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SeckillMapper {
    List<SeckillEntity> findAll(SeckillSearchBean ss);

    SeckillEntity findById(Long id);

    int save(SeckillEntity seckillEntity);

    int update(SeckillEntity seckillEntity);

    int deleteByIds(List<Long> ids);

    List<SeckillEntity> findActive();
}
