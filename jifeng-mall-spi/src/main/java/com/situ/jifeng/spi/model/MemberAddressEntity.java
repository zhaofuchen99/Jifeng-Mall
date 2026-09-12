package com.situ.jifeng.spi.model;


import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class MemberAddressEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 会员账号 */
    private String memberAccount;
    /* 收货人姓名 */
    private String receiver;
    /* 手机号 */
    private String phone;
    /* 街道编号 */
    private Long addrId;
    /* 地址详情 */
    private String addrDetail;
    /* 是否默认地址 */
    private Boolean isDefault;

    //地址
    private RegionEntity address;
}