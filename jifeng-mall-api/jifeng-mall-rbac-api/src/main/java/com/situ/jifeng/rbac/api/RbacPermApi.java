package com.situ.jifeng.rbac.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacPermEntity;
import com.situ.jifeng.spi.model.search.RbacPermSearchBean;
import com.situ.jifeng.spi.service.RbacPermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/perms", produces = MediaType.APPLICATION_JSON_VALUE)
public class RbacPermApi {
    private RbacPermService rbacPermService;

    @Autowired
    public void setRbacPermService(RbacPermService rbacPermService) {
        this.rbacPermService = rbacPermService;
    }

    /**
     * 查询所有权限。limit值为-1时，表示查询全部数据
     *
     * @return 所有权限实体
     */
    @GetMapping
    public JsonResp findAll(RbacPermSearchBean pe, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<RbacPermEntity> perms = rbacPermService.findAll(pe, pi);
        PageInfo<?> pageInfo = new PageInfo<>(perms);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        RbacPermEntity pe = rbacPermService.findById(id);
        return JsonResp.success(pe);
    }

    /**
     * 保存权限实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody RbacPermEntity pe) {
        boolean success = rbacPermService.save(pe);
        if (success) {
            return JsonResp.success(pe);
        } else {
            return JsonResp.fail(500, "保存权限失败");
        }
    }

    /**
     * 修改权限
     *
     * @param pe 权限实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody RbacPermEntity pe) {
        boolean success = rbacPermService.update(pe);
        if (success) {
            return JsonResp.success(pe);
        } else {
            return JsonResp.fail(500, "修改权限失败");
        }
    }

    /**
     * 批量删除权限
     *
     * @param ids 权限主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = rbacPermService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
