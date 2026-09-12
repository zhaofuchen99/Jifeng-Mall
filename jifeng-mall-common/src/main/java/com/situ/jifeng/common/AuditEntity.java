package com.situ.jifeng.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public abstract class AuditEntity implements Identity<Long> {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /* 创建时间 */
    private LocalDateTime createdTime;
    /* 创建人 */
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /* 最后修改时间 */
    private LocalDateTime updatedTime;
    /* 最后修改人 */
    private String updatedBy;
}
