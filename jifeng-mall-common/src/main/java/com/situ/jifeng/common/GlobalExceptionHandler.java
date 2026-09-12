package com.situ.jifeng.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把异常统一包装为 {@link JsonResp}，避免堆栈外泄（依据设计文档 2.6.4）。
 *
 * <p>说明：各微服务启动类需以 {@code @SpringBootApplication(scanBasePackages = "com.situ.jifeng")}
 * 开启对 common 包的扫描，方可生效。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：返回对应错误码与提示 */
    @ExceptionHandler(BusinessException.class)
    public JsonResp handleBusiness(BusinessException e) {
        return JsonResp.fail(e.getCode(), e.getMessage());
    }

    /** 参数错误（IllegalArgumentException 等） */
    @ExceptionHandler(IllegalArgumentException.class)
    public JsonResp handleIllegalArgument(IllegalArgumentException e) {
        return JsonResp.fail(400, e.getMessage());
    }

    /** 参数绑定/校验异常 */
    @ExceptionHandler(BindException.class)
    public JsonResp handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数错误");
        return JsonResp.fail(400, msg);
    }

    /** 兜底系统异常 */
    @ExceptionHandler(Exception.class)
    public JsonResp handleException(Exception e) {
        log.error("系统异常", e);
        return JsonResp.fail(500, "系统繁忙，请稍后再试");
    }
}
