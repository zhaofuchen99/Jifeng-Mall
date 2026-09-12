package com.situ.jifeng.category.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.CategoryEntity;
import com.situ.jifeng.spi.model.search.CategorySearchBean;
import com.situ.jifeng.spi.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/categories", produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoryApi {
    private CategoryService categoryService;

    @Autowired
    public void setCategoryService(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 查询所有分类。limit值为-1时，表示查询全部数据
     *
     * @return 所有分类实体
     */
    @GetMapping
    public JsonResp findAll(CategorySearchBean ce, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<CategoryEntity> categories = categoryService.findAll(ce, pi);
        PageInfo<?> pageInfo = new PageInfo<>(categories);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        CategoryEntity ce = categoryService.findById(id);
        return JsonResp.success(ce);
    }

    @GetMapping("/tree")
    public JsonResp findByTree() {
        List<CategoryEntity> roots = categoryService.findTree();
        return JsonResp.success(roots);
    }

    /**
     * 保存分类实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody CategoryEntity ce) {
        boolean success = categoryService.save(ce);
        if (success) {
            return JsonResp.success(ce);
        } else {
            return JsonResp.fail(500, "保存分类失败");
        }
    }

    /**
     * 修改分类
     *
     * @param ce 分类实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody CategoryEntity ce) {
        boolean success = categoryService.update(ce);
        if (success) {
            return JsonResp.success(ce);
        } else {
            return JsonResp.fail(500, "修改分类失败");
        }
    }

    /**
     * 批量删除分类
     *
     * @param ids 分类主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = categoryService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
