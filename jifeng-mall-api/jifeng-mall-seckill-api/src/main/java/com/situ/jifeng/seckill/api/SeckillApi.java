package com.situ.jifeng.seckill.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RestockParam;
import com.situ.jifeng.spi.model.SeckillEntity;
import com.situ.jifeng.spi.model.SeckillGrabParam;
import com.situ.jifeng.spi.model.SeckillGrabResult;
import com.situ.jifeng.spi.model.search.SeckillSearchBean;
import com.situ.jifeng.spi.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/seckills", produces = MediaType.APPLICATION_JSON_VALUE)
public class SeckillApi {
    private SeckillService seckillService;

    @Autowired
    public void setSeckillService(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /**
     * 查询所有秒杀活动。limit值为-1时，表示查询全部数据
     *
     * @return 所有秒杀活动实体
     */
    @GetMapping
    public JsonResp findAll(SeckillSearchBean ss, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<SeckillEntity> seckills = seckillService.findAll(ss, pi);
        PageInfo<?> pageInfo = new PageInfo<>(seckills);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        SeckillEntity se = seckillService.findById(id);
        return JsonResp.success(se);
    }

    /**
     * 查进行中/即将开始的活动（enabled=1 且未彻底结束），供秒杀中心首页展示
     *
     * @return 有效秒杀活动
     */
    @GetMapping("/active")
    public JsonResp findActive() {
        List<SeckillEntity> seckills = seckillService.findActive();
        return JsonResp.success(seckills);
    }

    /**
     * 秒杀抢购（会员）。当前用户由网关注入 X-User-Id / X-User-Name。
     */
    @PostMapping("/grab")
    public JsonResp grab(@RequestBody SeckillGrabParam param,
                         @RequestHeader(name = "X-User-Id", required = false) Long memberId,
                         @RequestHeader(name = "X-User-Name", required = false) String memberAccount) {
        SeckillGrabResult result = seckillService.grab(param.getSeckillGoodId(), memberId, memberAccount);
        if (result.getCode() == 200) {
            return JsonResp.success(result);
        }
        return JsonResp.fail(result.getCode(), result.getMsg());
    }

    /**
     * 秒杀库存回补（服务间调用：order 超时关单时触发）。body 含 seckillNo。
     */
    @PostMapping("/restock")
    public JsonResp restock(@RequestBody RestockParam param) {
        seckillService.restockSeckillOrder(param.getSeckillNo());
        return JsonResp.success(true);
    }

    /**
     * 保存秒杀活动实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody SeckillEntity se) {
        boolean success = seckillService.save(se);
        if (success) {
            return JsonResp.success(se);
        } else {
            return JsonResp.fail(500, "保存秒杀活动失败");
        }
    }

    /**
     * 修改秒杀活动
     *
     * @param se 秒杀活动实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody SeckillEntity se) {
        boolean success = seckillService.update(se);
        if (success) {
            return JsonResp.success(se);
        } else {
            return JsonResp.fail(500, "修改秒杀活动失败");
        }
    }

    /**
     * 批量删除秒杀活动
     *
     * @param ids 秒杀活动主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = seckillService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
