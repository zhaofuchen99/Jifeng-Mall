package com.situ.jifeng.common;

public record PaginateInfo(int pageNo, int pageSize) {
    public static PaginateInfo from(int pageNo, int pageSize) {
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize < 0) {
            pageSize = -1;
        }
        return new PaginateInfo(pageNo, pageSize);
    }
}
