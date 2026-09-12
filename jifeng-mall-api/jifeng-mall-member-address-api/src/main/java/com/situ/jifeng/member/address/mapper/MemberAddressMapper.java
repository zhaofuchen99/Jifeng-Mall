package com.situ.jifeng.member.address.mapper;

import com.situ.jifeng.spi.model.MemberAddressEntity;
import com.situ.jifeng.spi.model.search.MemberAddressSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MemberAddressMapper {
    List<MemberAddressEntity> findAll(MemberAddressSearchBean mae);

    MemberAddressEntity findById(Long id);

    int save(MemberAddressEntity memberAddressEntity);

    int update(MemberAddressEntity memberAddressEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 清除某会员的默认地址（excludeId 不为空时排除该条，用于更新自身默认场景），保证同一会员最多一条默认。
     */
    int clearDefault(String memberAccount, Long excludeId);

    /**
     * 将指定地址置为默认地址。
     */
    int setDefault(Long id);
}