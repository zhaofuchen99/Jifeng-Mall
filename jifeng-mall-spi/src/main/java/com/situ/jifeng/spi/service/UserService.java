package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.LoginParam;
import com.situ.jifeng.spi.model.LoginUserInfo;
import com.situ.jifeng.spi.model.UserEntity;
import com.situ.jifeng.spi.model.search.UserSearchBean;

import java.util.List;

public interface UserService {
    List<UserEntity> findAll(UserSearchBean ue, PaginateInfo pi);

    UserEntity findById(Long id);

    UserEntity findByUsername(String username);

    boolean save(UserEntity userEntity);

    boolean update(UserEntity userEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 后台用户登录。
     *
     * @param param 用户名 + 密码
     * @return 登录信息（含签发 admin 令牌）
     */
    LoginUserInfo login(LoginParam param);
}