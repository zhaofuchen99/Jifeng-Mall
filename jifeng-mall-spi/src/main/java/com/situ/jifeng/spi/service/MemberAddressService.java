package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.MemberAddressEntity;
import com.situ.jifeng.spi.model.search.MemberAddressSearchBean;
import com.situ.jifeng.spi.model.RegionEntity;

import java.util.List;

public interface MemberAddressService {
    List<MemberAddressEntity> findAll(MemberAddressSearchBean mae, PaginateInfo pi);

    List<MemberAddressEntity> findByMemberAccount(String account);

    MemberAddressEntity findById(Long id);

    /**
     * 将指定地址设为当前会员的默认地址（保证该会员仅一条 isDefault=1）：
     * 设置前先清除该会员的其他默认地址，再将该地址置为默认。memberAccount 为空时自动从地址记录中获取。
     *
     * @param id            地址主键
     * @param memberAccount 会员账号（可为空，空时从地址记录获取）
     * @return 设置默认后的地址实体（含关联地区）
     */
    MemberAddressEntity setDefault(Long id, String memberAccount);

    boolean save(MemberAddressEntity memberAddressEntity);

    boolean update(MemberAddressEntity memberAddressEntity);

    int deleteByIds(List<Long> ids);
}