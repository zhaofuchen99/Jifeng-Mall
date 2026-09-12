package com.situ.jifeng.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JsonResp {
    private int code;//状态码
    private boolean success;//操作是否成功
    private String msg;//异步信息
    private Object data;//数据

    public static JsonResp success(Object data) {
        return JsonResp.builder()
                .code(200)
                .success(true)
                .data(data)
                .build();
    }

    public static JsonResp fail(int code, String msg) {
        return JsonResp.builder()
                .code(code)
                .success(false)
                .msg(msg)
                .build();
    }
}
