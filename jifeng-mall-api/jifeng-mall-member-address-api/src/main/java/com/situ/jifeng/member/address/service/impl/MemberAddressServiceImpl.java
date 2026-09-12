package com.situ.jifeng.member.address.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.MemberAddressEntity;
import com.situ.jifeng.spi.model.search.MemberAddressSearchBean;
import com.situ.jifeng.spi.model.RegionEntity;
import com.situ.jifeng.spi.service.MemberAddressService;
import com.situ.jifeng.member.address.mapper.MemberAddressMapper;
import com.situ.jifeng.member.address.service.RegionFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberAddressServiceImpl implements MemberAddressService {
    private MemberAddressMapper memberAddressMapper;
    private RegionFeignService regionFeignService;

    @Autowired
    public void setMemberAddressMapper(MemberAddressMapper memberAddressMapper) {
        this.memberAddressMapper = memberAddressMapper;
    }

    @Autowired
    public void setRegionFeignService(RegionFeignService regionFeignService) {
        this.regionFeignService = regionFeignService;
    }

    @Override
    public List<MemberAddressEntity> findAll(MemberAddressSearchBean mae, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            List<MemberAddressEntity> list = memberAddressMapper.findAll(mae);
            list.forEach(this::makeFull);
            return list;
        }
    }

    @Override
    public List<MemberAddressEntity> findByMemberAccount(String account) {
        MemberAddressSearchBean mae = new MemberAddressSearchBean();
        mae.setMemberAccount(account);
        return findAll(mae, PaginateInfo.from(1, 0));
    }

    @Override
    public MemberAddressEntity findById(Long id) {
        MemberAddressEntity mae = memberAddressMapper.findById(id);
        if (mae != null) {
            makeFull(mae);
        }
        return mae;
    }

    private void makeFull(MemberAddressEntity mae) {
        if (mae.getAddrId() != null && mae.getAddress() == null) {
            TypedJsonResp<RegionEntity> resp = this.regionFeignService.findById(mae.getAddrId());
            if (resp.isSuccess() && resp.getData() != null) {
                mae.setAddress(resp.getData());
            }
        }
    }

    /**
     * 设置默认地址：先清除该会员的其他默认，再将该地址置为默认。
     */
    @Transactional
    @Override
    public MemberAddressEntity setDefault(Long id, String memberAccount) {
        if (id == null) {
            throw new BusinessException("地址不能为空");
        }
        MemberAddressEntity mae = memberAddressMapper.findById(id);
        if (mae == null) {
            throw new BusinessException("地址不存在");
        }
        if (memberAccount == null || memberAccount.isEmpty()) {
            memberAccount = mae.getMemberAccount();
        } else if (!memberAccount.equals(mae.getMemberAccount())) {
            throw new BusinessException("地址不属于当前会员");
        }
        // 清除该会员其他默认地址（排除当前条）
        memberAddressMapper.clearDefault(memberAccount, id);
        int rows = memberAddressMapper.setDefault(id);
        if (rows <= 0) {
            throw new BusinessException("设置默认地址失败");
        }
        MemberAddressEntity updated = memberAddressMapper.findById(id);
        if (updated != null) {
            makeFull(updated);
        }
        return updated;
    }

    @Override
    public boolean save(MemberAddressEntity memberAddressEntity) {
        // 保证会员仅一条默认地址：设为默认前先清除该会员其他默认
        if (Boolean.TRUE.equals(memberAddressEntity.getIsDefault())) {
            if (memberAddressEntity.getMemberAccount() == null || memberAddressEntity.getMemberAccount().isEmpty()) {
                throw new BusinessException("会员账号不能为空");
            }
            memberAddressMapper.clearDefault(memberAddressEntity.getMemberAccount(), null);
        }
        return memberAddressMapper.save(memberAddressEntity) > 0;
    }

    @Override
    public boolean update(MemberAddressEntity memberAddressEntity) {
        if (memberAddressEntity.getId() == null) {
            throw new BusinessException("地址不能为空");
        }
        if (Boolean.TRUE.equals(memberAddressEntity.getIsDefault())) {
            String account = memberAddressEntity.getMemberAccount();
            if (account == null || account.isEmpty()) {
                MemberAddressEntity existing = memberAddressMapper.findById(memberAddressEntity.getId());
                if (existing != null) {
                    account = existing.getMemberAccount();
                }
            }
            // 排除自身，清除该会员其他默认地址
            memberAddressMapper.clearDefault(account, memberAddressEntity.getId());
        }
        return memberAddressMapper.update(memberAddressEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return memberAddressMapper.deleteByIds(ids);
    }
}