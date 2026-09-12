package com.situ.jifeng.user.mapper;

import com.situ.jifeng.spi.model.UserEntity;
import com.situ.jifeng.spi.model.search.UserSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    List<UserEntity> findAll(UserSearchBean ue);

    UserEntity findById(Long id);

    UserEntity findByUsername(String username);

    int save(UserEntity userEntity);

    int update(UserEntity userEntity);

    int deleteByIds(List<Long> ids);
}