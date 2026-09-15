package com.situ.jifeng.good.mapper;

import com.situ.jifeng.spi.model.BannerEntity;
import com.situ.jifeng.spi.model.search.BannerSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BannerMapper {
    List<BannerEntity> findAll(BannerSearchBean bsb);

    BannerEntity findById(Long id);

    int save(BannerEntity bannerEntity);

    int update(BannerEntity bannerEntity);

    int deleteByIds(List<Long> ids);
}
