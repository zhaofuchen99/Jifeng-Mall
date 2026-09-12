package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.LoginParam;
import com.situ.jifeng.spi.model.LoginUserInfo;
import com.situ.jifeng.spi.model.MemberEntity;
import com.situ.jifeng.spi.model.search.MemberSearchBean;

import java.util.List;

public interface MemberService {
    List<MemberEntity> findAll(MemberSearchBean me, PaginateInfo pi);

    MemberEntity findById(Long id);

    MemberEntity findByAccount(String account);

    boolean save(MemberEntity memberEntity);

    boolean update(MemberEntity memberEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 会员注册。
     *
     * @param member 注册信息（含账号、明文密码）
     * @return 登录信息（含签发令牌）
     */
    LoginUserInfo register(MemberEntity member);

    /**
     * 会员登录。
     *
     * @param param 账号 + 密码
     * @return 登录信息（含签发令牌）
     */
    LoginUserInfo login(LoginParam param);
}