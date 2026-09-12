package com.situ.jifeng.cart.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.cart.service.GoodFeignService;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.CartItemEntity;
import com.situ.jifeng.spi.model.GoodEntity;
import com.situ.jifeng.spi.model.search.CartItemSearchBean;
import com.situ.jifeng.spi.service.CartItemService;
import com.situ.jifeng.cart.mapper.CartItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartItemServiceImpl implements CartItemService {
    private CartItemMapper cartItemMapper;
    private GoodFeignService goodFeignService;

    @Autowired
    public void setCartItemMapper(CartItemMapper cartItemMapper) {
        this.cartItemMapper = cartItemMapper;
    }

    @Autowired
    public void setGoodFeignService(GoodFeignService goodFeignService) {
        this.goodFeignService = goodFeignService;
    }

    @Override
    public List<CartItemEntity> findAll(CartItemSearchBean ce, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            List<CartItemEntity> items = cartItemMapper.findAll(ce);
            items.forEach(this::makeFull);
            return items;
        }
    }

    @Override
    public CartItemEntity findById(Long id) {
        CartItemEntity cartItem = cartItemMapper.findById(id);
        if (cartItem != null) {
            makeFull(cartItem);
        }
        return cartItem;
    }

    private void makeFull(CartItemEntity cartItem) {
        if (cartItem != null && cartItem.getGoodId() != null && cartItem.getGood() == null) {
            TypedJsonResp<GoodEntity> resp = goodFeignService.findById(cartItem.getGoodId());
            if (resp.isSuccess() && resp.getData() != null) {
                cartItem.setGood(resp.getData());
            }
        }
    }

    @Override
    public List<CartItemEntity> findByMemberId(Long memberId) {
        List<CartItemEntity> items = cartItemMapper.findByMemberId(memberId);
        items.forEach(this::makeFull);
        return items;
    }

    /**
     * 加入购物车（合并逻辑）：按会员 + 商品走唯一键合并。
     */
    @Transactional
    @Override
    public CartItemEntity saveAndMerge(CartItemEntity cartItemEntity) {
        if (cartItemEntity.getMemberId() == null) {
            throw new BusinessException("会员未登录");
        }
        if (cartItemEntity.getGoodId() == null) {
            throw new BusinessException("商品不能为空");
        }
        int qty = cartItemEntity.getQty() == null ? 1 : cartItemEntity.getQty();
        if (qty < 1) {
            throw new BusinessException("数量不能小于1");
        }
        cartItemEntity.setQty(qty);

        CartItemEntity exist = cartItemMapper.findByMemberIdAndGoodId(cartItemEntity.getMemberId(), cartItemEntity.getGoodId());
        if (exist != null) {
            exist.setQty((exist.getQty() == null ? 0 : exist.getQty()) + qty);
            cartItemMapper.update(exist);
            makeFull(exist);
            return exist;
        }
        cartItemMapper.save(cartItemEntity);
        makeFull(cartItemEntity);
        return cartItemEntity;
    }

    @Override
    public boolean save(CartItemEntity cartItemEntity) {
        return cartItemMapper.save(cartItemEntity) > 0;
    }

    @Override
    public boolean update(CartItemEntity cartItemEntity) {
        if (cartItemEntity.getQty() != null && cartItemEntity.getQty() < 1) {
            throw new BusinessException("数量不能小于1");
        }
        return cartItemMapper.update(cartItemEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return cartItemMapper.deleteByIds(ids);
    }

    @Override
    public int clearByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return cartItemMapper.clearByIds(ids);
    }

    @Override
    public int removeByGoodIds(Long memberId, List<Long> goodIds) {
        if (memberId == null) {
            throw new BusinessException("会员未登录");
        }
        if (goodIds == null || goodIds.isEmpty()) {
            return 0;
        }
        return cartItemMapper.removeByGoodIds(memberId, goodIds);
    }
}