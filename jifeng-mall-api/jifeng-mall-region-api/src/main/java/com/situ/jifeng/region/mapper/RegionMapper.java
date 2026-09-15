package com.situ.jifeng.region.mapper;

import com.situ.jifeng.spi.model.RegionEntity;
import com.situ.jifeng.spi.model.search.RegionSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RegionMapper {
    List<RegionEntity> findAll(RegionSearchBean re);

    RegionEntity findById(Long id);

    /**
     * 直接按父级取下级（不走缓存）。
     * 级联删除与层级修正在同一个请求内要读到<b>最新</b>数据，
     * 不能复用 {@code RegionService.findByParentId} 那层 @Cacheable。
     */
    List<RegionEntity> findByParentId(Long parentId);

    int save(RegionEntity region);

    int update(RegionEntity region);

    int deleteByIds(List<Long> ids);
}