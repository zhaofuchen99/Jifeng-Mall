package com.situ.jifeng.good.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.good.service.BrandFeignService;
import com.situ.jifeng.good.service.CategoryFeignService;
import com.situ.jifeng.spi.model.BrandEntity;
import com.situ.jifeng.spi.model.CategoryEntity;
import com.situ.jifeng.spi.model.DeductStockDto;
import com.situ.jifeng.spi.model.GoodEntity;
import com.situ.jifeng.spi.model.search.GoodSearchBean;
import com.situ.jifeng.spi.service.GoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/goods", produces = MediaType.APPLICATION_JSON_VALUE)
public class GoodApi {
    private GoodService goodService;
    private CategoryFeignService categoryFeignService;
    private BrandFeignService brandFeignService;

    @Autowired
    public void setGoodService(GoodService goodService) {
        this.goodService = goodService;
    }

    @Autowired
    public void setCategoryFeignService(CategoryFeignService categoryFeignService) {
        this.categoryFeignService = categoryFeignService;
    }

    @Autowired
    public void setBrandFeignService(BrandFeignService brandFeignService) {
        this.brandFeignService = brandFeignService;
    }

    /**
     * 查询所有商品。limit值为-1时，表示查询全部数据
     *
     * @return 所有商品实体
     */
    @GetMapping
    public JsonResp findAll(GoodSearchBean ge, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize, @RequestParam(defaultValue = "false") Boolean full) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<GoodEntity> goods = goodService.findAll(ge, pi);
        if (Boolean.TRUE.equals(full)) {
            goods.forEach(this::makeFull);
        }
        PageInfo<?> pageInfo = new PageInfo<>(goods);
        return JsonResp.success(pageInfo);
    }

    /**
     * 按条件统计商品数。供 brand-api / category-api 在删除前做**引用校验**用
     * （设计文档 5.3「删除校验商品引用」），避免删掉品牌/分类后商品变孤儿。
     *
     * <p>⚠️ 调用方必须显式传 {@code isDel=false}：{@code GoodSearchBean} 不传 isDel 时
     * **不过滤**逻辑删除，会把已删除的商品也算成引用，导致永远删不掉。</p>
     *
     * <p>只返回一个数字，不是分页对象 —— 校验场景没必要把商品实体查出来。</p>
     */
    @GetMapping("/count")
    public JsonResp count(GoodSearchBean ge) {
        return JsonResp.success(goodService.count(ge));
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id, @RequestParam(defaultValue = "false") Boolean full) {
        GoodEntity good = goodService.findById(id);
        if (Boolean.TRUE.equals(full)) {
            makeFull(good);
        }
        return JsonResp.success(good);
    }

    //注入关联对象
    private void makeFull(GoodEntity good) {
        if (good.getBrandId() != null) {
            TypedJsonResp<BrandEntity> resp = this.brandFeignService.findById(good.getBrandId());
            BrandEntity brand = resp.getData();
            if (resp.isSuccess() && brand != null) {
                good.setBrand(brand);
            }
        }

        if (good.getCategoryId() != null) {
            TypedJsonResp<CategoryEntity> resp = this.categoryFeignService.findById(good.getCategoryId());
            CategoryEntity category = resp.getData();
            if (resp.isSuccess() && category != null) {
                good.setCategory(category);
            }
        }
    }

    /**
     * 扣减库存（服务间调用，供下单防超卖）
     */
    @PostMapping("/{id}/deduct")
    public JsonResp deductStock(@PathVariable Long id, @RequestBody DeductStockDto dto) {
        boolean ok = goodService.deductStock(id, dto.getQty());
        return ok ? JsonResp.success(true) : JsonResp.fail(7001, "库存不足");
    }

    /**
     * 回补库存（取消订单/超时关单，服务间调用）
     */
    @PostMapping("/{id}/add-back")
    public JsonResp addBackStock(@PathVariable Long id, @RequestBody DeductStockDto dto) {
        boolean ok = goodService.addBackStock(id, dto.getQty());
        return ok ? JsonResp.success(true) : JsonResp.fail(500, "回补库存失败");
    }

    /**
     * 保存商品实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody GoodEntity ge) {
        boolean success = goodService.save(ge);
        if (success) {
            return JsonResp.success(ge);
        } else {
            return JsonResp.fail(500, "保存商品失败");
        }
    }

    /**
     * 修改商品
     *
     * @param ge 商品实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody GoodEntity ge) {
        boolean success = goodService.update(ge);
        if (success) {
            return JsonResp.success(ge);
        } else {
            return JsonResp.fail(500, "修改商品失败");
        }
    }

    /**
     * 批量删除商品
     *
     * @param ids 商品主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = goodService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
