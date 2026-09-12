package com.situ.jifeng.member.mapper;

import com.situ.jifeng.spi.model.MemberEntity;
import com.situ.jifeng.spi.model.search.MemberSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MemberMapper {
    List<MemberEntity> findAll(MemberSearchBean me);

    MemberEntity findById(Long id);

    MemberEntity findByAccount(String account);

    int save(MemberEntity memberEntity);

    int update(MemberEntity memberEntity);

    int deleteByIds(List<Long> ids);
}