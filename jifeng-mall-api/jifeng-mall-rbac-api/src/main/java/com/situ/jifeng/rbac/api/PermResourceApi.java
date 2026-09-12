package com.situ.jifeng.rbac.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.PermResourceEntity;
import com.situ.jifeng.spi.model.search.PermResourceSearchBean;
import com.situ.jifeng.spi.service.PermResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/perm-resources", produces = MediaType.APPLICATION_JSON_VALUE)
public class PermResourceApi {
    private PermResourceService permResourceService;

    @Autowired
    public void setPermResourceService(PermResourceService permResourceService) {
        this.permResourceService = permResourceService;
    }

    /**
     * 查询所有权限资源关联。limit值为-1时，表示查询全部数据
     *
     * @return 所有权限资源关联实体
     */
    @GetMapping
    public JsonResp findAll(PermResourceSearchBean pre, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<PermResourceEntity> permResources = permResourceService.findAll(pre, pi);
        PageInfo<?> pageInfo = new PageInfo<>(permResources);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        PermResourceEntity pre = permResourceService.findById(id);
        return JsonResp.success(pre);
    }

    //查询指定权限的所有资源关联
    @GetMapping("/perm/{permId}")
    public JsonResp findByPermId(@PathVariable Long permId) {
        List<PermResourceEntity> pres = permResourceService.findByPermId(permId);
        return JsonResp.success(pres);
    }

    //查询指定资源的所有权限关联
    @GetMapping("/resource/{resourceId}")
    public JsonResp findByResourceId(@PathVariable Long resourceId) {
        List<PermResourceEntity> pres = permResourceService.findByResourceId(resourceId);
        return JsonResp.success(pres);
    }

    /**
     * 保存权限资源关联实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody PermResourceEntity pre) {
        boolean success = permResourceService.save(pre);
        if (success) {
            return JsonResp.success(pre);
        } else {
            return JsonResp.fail(500, "保存权限资源关联失败");
        }
    }

    /**
     * 修改权限资源关联
     *
     * @param pre 权限资源关联实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody PermResourceEntity pre) {
        boolean success = permResourceService.update(pre);
        if (success) {
            return JsonResp.success(pre);
        } else {
            return JsonResp.fail(500, "修改权限资源关联失败");
        }
    }

    /**
     * 批量删除权限资源关联
     *
     * @param ids 权限资源关联主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = permResourceService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
