package com.situ.jifeng.cart.api;

import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.spi.model.CartItemEntity;
import com.situ.jifeng.spi.service.CartItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/carts", produces = MediaType.APPLICATION_JSON_VALUE)
public class CartItemApi {
    private CartItemService cartItemService;

    @Autowired
    public void setCartItemService(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }


    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        CartItemEntity ce = cartItemService.findById(id);
        return JsonResp.success(ce);
    }

    //查询指定会员的所有购物车条目
    @GetMapping("/member/{memberId}")
    public JsonResp findByMemberId(@PathVariable Long memberId) {
        List<CartItemEntity> ces = cartItemService.findByMemberId(memberId);
        return JsonResp.success(ces);
    }

    /**
     * 保存购物车条目实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody CartItemEntity ce) {
        boolean success = cartItemService.save(ce);
        if (success) {
            return JsonResp.success(ce);
        } else {
            return JsonResp.fail(500, "保存购物车条目失败");
        }
    }

    /**
     * 加入购物车（合并逻辑）：按会员 + 商品合并累加数量
     *
     * @return 合并后的购物车条目
     */
    @PostMapping("/save-merge")
    public JsonResp saveAndMerge(@RequestBody CartItemEntity ce) {
        CartItemEntity merged = cartItemService.saveAndMerge(ce);
        return JsonResp.success(merged);
    }

    /**
     * 修改购物车条目
     *
     * @param ce 购物车条目实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody CartItemEntity ce) {
        boolean success = cartItemService.update(ce);
        if (success) {
            return JsonResp.success(ce);
        } else {
            return JsonResp.fail(500, "修改购物车条目失败");
        }
    }

    /**
     * 批量删除购物车条目
     *
     * @param ids 购物车主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = cartItemService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }

    /**
     * 下单后批量清空购物车条目（按购物车主键删除）
     *
     * @param ids 购物车主键集合
     * @return 删除条数
     */
    @DeleteMapping("/clear")
    public JsonResp clearByIds(@RequestBody Long[] ids) {
        int rows = cartItemService.clearByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }

    /**
     * 下单后按商品清空某会员的购物车条目
     *
     * @param memberId 会员编号
     * @param goodIds  商品编号集合（逗号分隔）
     * @return 删除条数
     */
    @DeleteMapping("/remove-by-goods")
    public JsonResp removeByGoodIds(@RequestParam Long memberId, @RequestParam List<Long> goodIds) {
        int rows = cartItemService.removeByGoodIds(memberId, goodIds);

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}