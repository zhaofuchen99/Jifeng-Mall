package com.situ.jifeng.brand.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.BrandEntity;
import com.situ.jifeng.spi.model.search.BrandSearchBean;
import com.situ.jifeng.spi.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/brands", produces = MediaType.APPLICATION_JSON_VALUE)
public class BrandApi {
    private BrandService brandService;

    @Autowired
    public void setBrandService(BrandService brandService) {
        this.brandService = brandService;
    }

    /**
     * 查询所有品牌。limit值为-1时，表示查询全部数据
     *
     * @return 所有品牌实体
     */
    @GetMapping
    public JsonResp findAll(BrandSearchBean be, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<BrandEntity> brands = brandService.findAll(be, pi);
        PageInfo<?> pageInfo = new PageInfo<>(brands);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<JsonResp> findById(@PathVariable Long id) {
        BrandEntity be = brandService.findById(id);
        return ResponseEntity.ok(JsonResp.success(be));
    }

    /**
     * 保存品牌实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody BrandEntity be) {
        boolean success = brandService.save(be);
        if (success) {
            return JsonResp.success(be);
        } else {
            return JsonResp.fail(500, "保存品牌失败");
        }
    }

    /**
     * 修改品牌
     *
     * @param be 品牌实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody BrandEntity be) {
        boolean success = brandService.update(be);
        if (success) {
            return JsonResp.success(be);
        } else {
            return JsonResp.fail(500, "修改会员失败");
        }
    }

    /**
     * 批量删除品牌
     *
     * @param ids 品牌主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = brandService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
