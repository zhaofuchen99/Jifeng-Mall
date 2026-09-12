package com.situ.jifeng.region.mapper;

import com.situ.jifeng.spi.model.RegionEntity;
import com.situ.jifeng.spi.model.search.RegionSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RegionMapper {
    List<RegionEntity> findAll(RegionSearchBean re);

    RegionEntity findById(Long id);
}