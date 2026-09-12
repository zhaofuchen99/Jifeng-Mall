package com.situ.jifeng.rbac.api;

import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.spi.model.RbacCheckParam;
import com.situ.jifeng.spi.model.RbacGrant;
import com.situ.jifeng.spi.service.RbacResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限判定接口（供网关内部调用，依据设计文档 4.3 / 4.6.5）。
 */
@RestController
@RequestMapping(value = "/api/rbac", produces = MediaType.APPLICATION_JSON_VALUE)
public class RbacCheckApi {
    private RbacResourceService rbacResourceService;

    @Autowired
    public void setRbacResourceService(RbacResourceService rbacResourceService) {
        this.rbacResourceService = rbacResourceService;
    }

    /**
     * 权限判定：userId + path + method → 是否放行。
     */
    @PostMapping("/check")
    public JsonResp check(@RequestBody RbacCheckParam param) {
        RbacGrant grant = rbacResourceService.check(param);
        return JsonResp.success(grant);
    }
}
