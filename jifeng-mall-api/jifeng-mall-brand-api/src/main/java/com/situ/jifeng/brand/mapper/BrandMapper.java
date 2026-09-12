package com.situ.jifeng.brand.mapper;

import com.situ.jifeng.spi.model.BrandEntity;
import com.situ.jifeng.spi.model.search.BrandSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BrandMapper {
    List<BrandEntity> findAll(BrandSearchBean be);

    BrandEntity findById(Long id);

    int save(BrandEntity brandEntity);

    int update(BrandEntity brandEntity);

    int deleteByIds(List<Long> ids);
}
