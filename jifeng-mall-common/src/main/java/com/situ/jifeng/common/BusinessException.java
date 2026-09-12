package com.situ.jifeng.common;

/**
 * 业务异常。业务规则校验失败时抛出，由全局异常处理器统一包装为 JsonResp。
 */
public class BusinessException extends RuntimeException {

    /** 错误码（默认 500，可按业务场景自定义，如库存不足用 7001） */
    private final int code;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
