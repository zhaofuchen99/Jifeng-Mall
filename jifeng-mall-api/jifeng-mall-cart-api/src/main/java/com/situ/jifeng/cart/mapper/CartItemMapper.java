package com.situ.jifeng.cart.mapper;

import com.situ.jifeng.spi.model.CartItemEntity;
import com.situ.jifeng.spi.model.search.CartItemSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CartItemMapper {
    List<CartItemEntity> findAll(CartItemSearchBean ce);

    CartItemEntity findById(Long id);

    List<CartItemEntity> findByMemberId(Long memberId);

    /**
     * 依据会员编号 + 商品编号查询购物车记录（唯一键 uk_cart_member_good）。
     */
    CartItemEntity findByMemberIdAndGoodId(Long memberId, Long goodId);

    int save(CartItemEntity cartItemEntity);

    int update(CartItemEntity cartItemEntity);

    int deleteByIds(List<Long> ids);

    int clearByIds(List<Long> ids);

    int removeByGoodIds(Long memberId, List<Long> goodIds);
}