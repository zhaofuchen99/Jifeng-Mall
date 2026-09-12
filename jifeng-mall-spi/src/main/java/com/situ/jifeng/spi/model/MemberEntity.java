package com.situ.jifeng.spi.model;


import com.situ.jifeng.common.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class MemberEntity extends AuditEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 用户名 */
    private String account;
    /* 密码 */
    private String password;
    /* 是否启用 */
    private Boolean enabled;
    /* 姓名，全名，也可能是英文姓名 */
    private String name;
    /* 中文姓名的全拼形式 */
    private String pinyin;
    /* 中文姓名的无音调拼音连写形式 */
    private String pinyinUntoned;
    /* 名，仅名。按英文规则写法，名在前，姓在后 */
    private String firstName;
    /* 姓氏，单姓或复姓 */
    private String lastName;
    /* 性别，生物性别 */
    private String sex;
    /* 出生日期 */
    private LocalDate birthday;
    /* 身高，单位cm */
    private Integer height;
    /* 体重，单位kg */
    private BigDecimal weight;
    /* 智商指数 */
    private Integer iq;
    /* qq号 */
    private String qq;
    /* 微信号 */
    private String wechat;
    /* 手机号 */
    private String phone;
    /* 电子邮箱 */
    private String email;
    /* 籍贯所在地编号，3级 */
    private Integer nativePlaceId;
    /* 身份证号 */
    private String cardId;
    /* 婚姻状况 */
    private String wedlock;
    /* 政治面貌 */
    private String politicalOrientation;
    /* 家庭住址，省市区县4级编号 */
    private Integer addressId;
    /* 地址详情（地址细节） */
    private String addressDetail;
    /* 种族，民族 */
    private String race;
    /* 宗教 */
    private String religion;
    /* 国籍 */
    private String nationality;
    /* 肖像，头像，照片 */
    private String portrait;
    /* 备注 */
    private String description;
    /* 最后一次登录时间 */
    private LocalDateTime lastLoginTime;
    /* 最后一次登录IP地址 */
    private String lastLoginIp;
    /* 额外的用户信息，可以进行灵活处理 */
    private String extraInfo;
    /* 此记录修改版本号 */
    private Integer version;
}