package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.BannerEntity;
import com.situ.jifeng.spi.model.search.BannerSearchBean;

import java.util.List;

/**
 * 首页轮播 Banner（需求 5.3 / FR-102）。
 *
 * <p>前台首页取数据用 {@code findAll(bsb, pi)} 并传 {@code enabled=true}，
 * 结果按 {@code sort_no, id} 排序。</p>
 */
public interface BannerService {
    List<BannerEntity> findAll(BannerSearchBean bsb, PaginateInfo pi);

    BannerEntity findById(Long id);

    boolean save(BannerEntity bannerEntity);

    boolean update(BannerEntity bannerEntity);

    int deleteByIds(List<Long> ids);
}
