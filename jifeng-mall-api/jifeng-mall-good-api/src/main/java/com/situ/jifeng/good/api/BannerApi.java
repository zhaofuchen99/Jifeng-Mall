package com.situ.jifeng.good.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.BannerEntity;
import com.situ.jifeng.spi.model.search.BannerSearchBean;
import com.situ.jifeng.spi.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 首页轮播 Banner（需求 5.3 / FR-102：「轮播 Banner（可配置图片与跳转链接）」）。
 *
 * <p><b>为什么挂在商品中心</b>：需求规格说明书要求轮播可配置，但详细设计说明书通篇没有轮播——
 * 2.4 节的微服务清单是 13 个（12 api + 网关），3.3 节没有对应的表，8 节的接口清单里也没有轮播接口。
 * 新增一个 {@code banner-api} 会与设计文档的服务清单冲突，所以并入 good-api：首页的另一块数据
 * （热销商品）本来也由它提供。</p>
 *
 * <p><b>路径放在 {@code /api/goods/banners} 下的好处</b>：直接复用网关已有的两条规则——
 * 「GET /api/goods/** 公开」与商品管理权限资源 {@code /api/goods/**}。
 * 不必改网关白名单（改了要重启网关），也不必往 {@code t_rbac_resource} 补行。</p>
 *
 * <p>⚠️ <b>代价</b>：网关的公开规则是<b>按前缀</b>的，所以 {@code GET /api/goods/banners}
 * 对游客也开放——「列表接口只给后台」这个区分在这里不成立。轮播本身就是公开内容，
 * 被看到一条停用项没有实际影响，因此接受。若将来轮播里要放未发布的活动信息，
 * 就该把它挪到独立路径（如 {@code /api/banners}）并单独注册权限资源。</p>
 */
@RestController
@RequestMapping(value = "/api/goods/banners", produces = MediaType.APPLICATION_JSON_VALUE)
public class BannerApi {
    private BannerService bannerService;

    @Autowired
    public void setBannerService(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    /**
     * 分页查询轮播。
     *
     * <p>前台首页拉取时传 {@code enabled=true&pageSize=0}（0 = 全部），
     * 返回的是启用中的轮播，已按 sort_no 排好序。</p>
     */
    @GetMapping
    public JsonResp findAll(BannerSearchBean bsb, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<BannerEntity> banners = bannerService.findAll(bsb, pi);
        PageInfo<?> pageInfo = new PageInfo<>(banners);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        return JsonResp.success(bannerService.findById(id));
    }

    /**
     * 新增轮播
     */
    @PostMapping
    public JsonResp save(@RequestBody BannerEntity bannerEntity) {
        boolean success = bannerService.save(bannerEntity);
        if (success) {
            return JsonResp.success(bannerEntity);
        } else {
            return JsonResp.fail(500, "保存轮播失败");
        }
    }

    /**
     * 修改轮播
     */
    @PutMapping
    public JsonResp update(@RequestBody BannerEntity bannerEntity) {
        boolean success = bannerService.update(bannerEntity);
        if (success) {
            return JsonResp.success(bannerEntity);
        } else {
            return JsonResp.fail(500, "修改轮播失败");
        }
    }

    /**
     * 批量删除轮播
     *
     * @param ids 轮播主键集合
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = bannerService.deleteByIds(List.of(ids));
        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
