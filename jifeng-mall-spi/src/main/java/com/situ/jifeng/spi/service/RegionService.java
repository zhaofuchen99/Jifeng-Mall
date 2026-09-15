package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RegionEntity;
import com.situ.jifeng.spi.model.search.RegionSearchBean;

import java.util.List;

public interface RegionService {
    List<RegionEntity> findAll(RegionSearchBean re, PaginateInfo pi);

    List<RegionEntity> findByParentId(Long parentId);

    RegionEntity findById(Long id);

    /**
     * 新增区划。层级由父级推导（根为 1），不接受调用方传入的 level。
     *
     * @param region 区划实体；id 可不传（自增），传了就用传的（行政区划编码）
     * @return 是否成功
     */
    boolean save(RegionEntity region);

    /**
     * 修改区划。层级由父级重新推导，并同步修正其所有后代的层级。
     *
     * @param region 区划实体，须带 id
     * @return 是否成功
     */
    boolean update(RegionEntity region);

    /**
     * 删除区划，<b>连同其所有下级一起删除</b>（树形维护语义，与分类管理一致）。
     *
     * @param ids 待删除的区划编号
     * @return 实际删除的行数（含被级联删除的下级）
     */
    int deleteByIds(List<Long> ids);
}