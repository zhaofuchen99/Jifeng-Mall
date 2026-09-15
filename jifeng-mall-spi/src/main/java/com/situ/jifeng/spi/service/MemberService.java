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

    /** 修改会员。传了 password 就是改密码（后台「重置密码」走的也是这里），不传则不动密码列 */
    boolean update(MemberEntity memberEntity);

    /** 会员自助改密码：服务端校验旧密码后写入新密码（BCrypt），并把该会员的登录失败锁定一并解除 */
    boolean changePassword(Long memberId, String oldPassword, String newPassword);

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