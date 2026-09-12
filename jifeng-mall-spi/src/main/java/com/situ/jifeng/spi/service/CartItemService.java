package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.CartItemEntity;
import com.situ.jifeng.spi.model.search.CartItemSearchBean;

import java.util.List;

public interface CartItemService {
    List<CartItemEntity> findAll(CartItemSearchBean ce, PaginateInfo pi);

    CartItemEntity findById(Long id);

    List<CartItemEntity> findByMemberId(Long memberId);

    /**
     * 加入购物车（合并逻辑）：若指定 memberId + goodId 已存在（唯一键 uk_cart_member_good），则累计 qty 并返回现有记录；
     * 否则新增一条购物车记录。会员未登录（memberId 为空）时抛 {@link com.situ.jifeng.common.BusinessException}。
     *
     * @param cartItemEntity 购物车条目（必须带 memberId、goodId）
     * @return 合并后的购物车条目（含关联商品）
     */
    CartItemEntity saveAndMerge(CartItemEntity cartItemEntity);

    boolean save(CartItemEntity cartItemEntity);

    boolean update(CartItemEntity cartItemEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 下单后批量清空购物车条目（按购物车主键删除）。
     *
     * @param ids 购物车主键集合
     * @return 删除条数
     */
    int clearByIds(List<Long> ids);

    /**
     * 下单后按商品清空某会员的购物车条目。
     *
     * @param memberId 会员编号
     * @param goodIds  商品编号集合
     * @return 删除条数
     */
    int removeByGoodIds(Long memberId, List<Long> goodIds);
}