package com.situ.jifeng.member.address.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.MemberAddressEntity;
import com.situ.jifeng.spi.model.search.MemberAddressSearchBean;
import com.situ.jifeng.spi.service.MemberAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/member-addresses", produces = MediaType.APPLICATION_JSON_VALUE)
public class MemberAddressApi {
    private MemberAddressService memberAddressService;

    @Autowired
    public void setMemberAddressService(MemberAddressService memberAddressService) {
        this.memberAddressService = memberAddressService;
    }

    /**
     * 查询所有会员地址。limit值为-1时，表示查询全部数据
     *
     * @return 所有会员地址实体
     */
    @GetMapping
    public JsonResp findAll(MemberAddressSearchBean mae, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<MemberAddressEntity> memberAddresses = memberAddressService.findAll(mae, pi);
        PageInfo<?> pageInfo = new PageInfo<>(memberAddresses);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        MemberAddressEntity mae = memberAddressService.findById(id);
        return JsonResp.success(mae);
    }

    //查询指定会员的所有收货地址
    @GetMapping("/account/{account}")
    public JsonResp findByMemberAccount(@PathVariable String account) {
        List<MemberAddressEntity> maes = memberAddressService.findByMemberAccount(account);
        return JsonResp.success(maes);
    }

    /**
     * 设置默认地址（memberAccount 为空时自动从地址记录获取），保证该会员仅一条默认地址
     *
     * @param id            地址主键
     * @param memberAccount 会员账号（可选）
     * @return 设置默认后的地址实体
     */
    @PutMapping("/default/{id}")
    public JsonResp setDefault(@PathVariable Long id, @RequestParam(required = false) String memberAccount) {
        MemberAddressEntity mae = memberAddressService.setDefault(id, memberAccount);
        return JsonResp.success(mae);
    }

    /**
     * 保存会员地址实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody MemberAddressEntity mae) {
        boolean success = memberAddressService.save(mae);
        if (success) {
            return JsonResp.success(mae);
        } else {
            return JsonResp.fail(500, "保存会员地址失败");
        }
    }

    /**
     * 修改会员地址
     *
     * @param mae 会员地址实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody MemberAddressEntity mae) {
        boolean success = memberAddressService.update(mae);
        if (success) {
            return JsonResp.success(mae);
        } else {
            return JsonResp.fail(500, "修改会员地址失败");
        }
    }

    /**
     * 批量删除会员地址
     *
     * @param ids 会员地址主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = memberAddressService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}