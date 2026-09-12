package com.situ.jifeng.rbac.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacMenuEntity;
import com.situ.jifeng.spi.model.search.RbacMenuSearchBean;
import com.situ.jifeng.spi.service.RbacMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/menus", produces = MediaType.APPLICATION_JSON_VALUE)
public class RbacMenuApi {
    private RbacMenuService rbacMenuService;

    @Autowired
    public void setRbacMenuService(RbacMenuService rbacMenuService) {
        this.rbacMenuService = rbacMenuService;
    }

    /**
     * 查询所有菜单。limit值为-1时，表示查询全部数据
     *
     * @return 所有菜单实体
     */
    @GetMapping
    public JsonResp findAll(RbacMenuSearchBean me, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<RbacMenuEntity> menus = rbacMenuService.findAll(me, pi);
        PageInfo<?> pageInfo = new PageInfo<>(menus);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        RbacMenuEntity me = rbacMenuService.findById(id);
        return JsonResp.success(me);
    }

    /**
     * 当前用户的动态菜单树（网关已注入 X-User-Id）
     */
    @GetMapping("/mine")
    public JsonResp myMenus(@RequestHeader(name = "X-User-Id", required = false) Long userId) {
        List<RbacMenuEntity> tree = rbacMenuService.findMenuTreeByUserId(userId);
        return JsonResp.success(tree);
    }

    /**
     * 保存菜单实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody RbacMenuEntity me) {
        boolean success = rbacMenuService.save(me);
        if (success) {
            return JsonResp.success(me);
        } else {
            return JsonResp.fail(500, "保存菜单失败");
        }
    }

    /**
     * 修改菜单
     *
     * @param me 菜单实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody RbacMenuEntity me) {
        boolean success = rbacMenuService.update(me);
        if (success) {
            return JsonResp.success(me);
        } else {
            return JsonResp.fail(500, "修改菜单失败");
        }
    }

    /**
     * 批量删除菜单
     *
     * @param ids 菜单主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = rbacMenuService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
