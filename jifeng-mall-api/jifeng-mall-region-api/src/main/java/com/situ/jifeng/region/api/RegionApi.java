package com.situ.jifeng.region.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RegionEntity;
import com.situ.jifeng.spi.model.search.RegionSearchBean;
import com.situ.jifeng.spi.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/regions", produces = MediaType.APPLICATION_JSON_VALUE)
public class RegionApi {
    private RegionService regionService;

    @Autowired
    public void setRegionService(RegionService regionService) {
        this.regionService = regionService;
    }

    /**
     * 查询所有地区。limit值为-1时，表示查询全部数据
     *
     * @return 所有地区实体
     */
    @GetMapping
    public JsonResp findAll(RegionSearchBean re, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<RegionEntity> provinces = regionService.findAll(re, pi);
        PageInfo<?> pageInfo = new PageInfo<>(provinces);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        RegionEntity re = regionService.findById(id);
        return JsonResp.success(re);
    }

    @GetMapping("/parent-id/{id}")
    public JsonResp findByParentId(@PathVariable Long id) {
        List<RegionEntity> res = regionService.findByParentId(id);
        return JsonResp.success(res);
    }
}