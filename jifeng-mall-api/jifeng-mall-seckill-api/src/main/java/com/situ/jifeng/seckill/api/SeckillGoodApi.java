package com.situ.jifeng.seckill.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.SeckillGoodEntity;
import com.situ.jifeng.spi.model.search.SeckillGoodSearchBean;
import com.situ.jifeng.spi.service.SeckillGoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/seckill-goods", produces = MediaType.APPLICATION_JSON_VALUE)
public class SeckillGoodApi {
    private SeckillGoodService seckillGoodService;

    @Autowired
    public void setSeckillGoodService(SeckillGoodService seckillGoodService) {
        this.seckillGoodService = seckillGoodService;
    }

    /**
     * 查询所有秒杀活动商品。limit值为-1时，表示查询全部数据
     *
     * @return 所有秒杀活动商品实体
     */
    @GetMapping
    public JsonResp findAll(SeckillGoodSearchBean sgs, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<SeckillGoodEntity> seckillGoods = seckillGoodService.findAll(sgs, pi);
        PageInfo<?> pageInfo = new PageInfo<>(seckillGoods);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        SeckillGoodEntity sge = seckillGoodService.findById(id);
        return JsonResp.success(sge);
    }

    /**
     * 查某秒杀活动下的所有商品
     *
     * @return 秒杀活动商品集合
     */
    @GetMapping("/seckill/{seckillId}")
    public JsonResp findBySeckillId(@PathVariable Long seckillId) {
        List<SeckillGoodEntity> seckillGoods = seckillGoodService.findBySeckillId(seckillId);
        return JsonResp.success(seckillGoods);
    }

    /**
     * 保存秒杀活动商品实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody SeckillGoodEntity sge) {
        boolean success = seckillGoodService.save(sge);
        if (success) {
            return JsonResp.success(sge);
        } else {
            return JsonResp.fail(500, "保存秒杀活动商品失败");
        }
    }

    /**
     * 修改秒杀活动商品
     *
     * @param sge 秒杀活动商品实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody SeckillGoodEntity sge) {
        boolean success = seckillGoodService.update(sge);
        if (success) {
            return JsonResp.success(sge);
        } else {
            return JsonResp.fail(500, "修改秒杀活动商品失败");
        }
    }

    /**
     * 批量删除秒杀活动商品
     *
     * @param ids 秒杀活动商品主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = seckillGoodService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
