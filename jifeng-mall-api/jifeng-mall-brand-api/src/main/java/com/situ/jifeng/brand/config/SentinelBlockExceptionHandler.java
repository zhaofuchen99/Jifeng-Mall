package com.situ.jifeng.brand.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.situ.jifeng.common.JsonResp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.PrintWriter;

@Slf4j
@Component
public class SentinelBlockExceptionHandler implements BlockExceptionHandler {
    private JsonMapper jsonMapper;

    @Autowired
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp,
                       String resourceName, BlockException e) throws Exception {
        log.error("{}:{}:{}", resourceName, e.getClass().getName(), e.getMessage());

        //429 状态码，Too many requests

        resp.setCharacterEncoding("UTF-8");//防止中文乱码
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);

        JsonResp jr = null;
        if (e instanceof FlowException) {
            resp.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            jr = JsonResp.fail(429, "请求过快，请稍候再试");
        } else if (e instanceof DegradeException) {
            resp.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            jr = JsonResp.fail(503, "服务不可用");
        }

        PrintWriter writer = resp.getWriter();
        writer.write(jsonMapper.writeValueAsString(jr));
        writer.flush();
    }
}
