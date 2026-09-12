package com.situ.jifeng.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypedJsonResp<T> {
    private int code;//状态码
    private boolean success;//操作是否成功
    private String msg;//异步信息
    private T data;//数据

    public static <T> TypedJsonResp<T> success(T data) {
        return new TypedJsonResp<>(200, true, "success", data);
    }

    public static <T> TypedJsonResp<T> fail(int code, String msg) {
        return new TypedJsonResp<>(code, false, msg, null);
    }
}
