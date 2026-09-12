package com.situ.jifeng.seckill.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.common.SnowFlakeNoGenerator;
import com.situ.jifeng.seckill.config.SeckillGrabMessage;
import com.situ.jifeng.seckill.config.SeckillMqConfig;
import com.situ.jifeng.seckill.mapper.SeckillGoodMapper;
import com.situ.jifeng.seckill.mapper.SeckillMapper;
import com.situ.jifeng.spi.model.SeckillEntity;
import com.situ.jifeng.spi.model.SeckillGoodEntity;
import com.situ.jifeng.spi.model.SeckillGrabResult;
import com.situ.jifeng.spi.model.search.SeckillSearchBean;
import com.situ.jifeng.spi.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class SeckillServiceImpl implements SeckillService, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SeckillServiceImpl.class);

    /** 秒杀库存 key 前缀：seckill:stock:{seckillGoodId} */
    public static final String STOCK_KEY_PREFIX = "seckill:stock:";
    /** 用户已抢 key 前缀：seckill:user:{memberId}:{seckillGoodId} */
    public static final String USER_KEY_PREFIX = "seckill:user:";
    /**
     * 订单-秒杀商品映射 key：seckill:order:{seckillNo} = "seckillGoodId:memberId"。
     * 带上 memberId 是为了回补时能一并释放该会员的"已抢"记录（见 restockSeckillOrder）。
     * 兼容早期只写 seckillGoodId 的老键 —— 解析时按 ":" 分段，取不到 memberId 就跳过释放。
     */
    public static final String ORDER_KEY_PREFIX = "seckill:order:";

    private SeckillMapper seckillMapper;
    private SeckillGoodMapper seckillGoodMapper;
    private StringRedisTemplate stringRedisTemplate;
    private RabbitTemplate rabbitTemplate;

    private RedisScript<Long> grabScript;

    @Autowired
    public void setSeckillMapper(SeckillMapper seckillMapper) {
        this.seckillMapper = seckillMapper;
    }

    @Autowired
    public void setSeckillGoodMapper(SeckillGoodMapper seckillGoodMapper) {
        this.seckillGoodMapper = seckillGoodMapper;
    }

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String lua = StreamUtils.copyToString(
                new ClassPathResource("seckill.lua").getInputStream(), StandardCharsets.UTF_8);
        this.grabScript = new DefaultRedisScript<>(lua, Long.class);
    }

    @Override
    public List<SeckillEntity> findAll(SeckillSearchBean ss, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return seckillMapper.findAll(ss);
        }
    }

    @Override
    public SeckillEntity findById(Long id) {
        return seckillMapper.findById(id);
    }

    @Override
    public boolean save(SeckillEntity seckillEntity) {
        return seckillMapper.save(seckillEntity) > 0;
    }

    @Override
    public boolean update(SeckillEntity seckillEntity) {
        return seckillMapper.update(seckillEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return seckillMapper.deleteByIds(ids);
    }

    @Override
    public List<SeckillEntity> findActive() {
        return seckillMapper.findActive();
    }

    @Override
    public SeckillGrabResult grab(Long seckillGoodId, Long memberId, String memberAccount) {
        if (memberId == null || memberAccount == null || memberAccount.isBlank()) {
            return result(401, "请先登录", null, false);
        }
        SeckillGoodEntity sg = seckillGoodMapper.findById(seckillGoodId);
        if (sg == null) {
            return result(7002, "秒杀商品不存在", null, false);
        }
        SeckillEntity seckill = seckillMapper.findById(sg.getSeckillId());
        if (seckill == null || !Boolean.TRUE.equals(seckill.getEnabled())) {
            return result(7002, "秒杀活动未启用或已结束", null, false);
        }
        // 时间窗校验
        LocalDateTime now = LocalDateTime.now();
        if (seckill.getStartTime() != null && now.isBefore(seckill.getStartTime())) {
            return result(7002, "秒杀活动尚未开始", null, false);
        }
        if (seckill.getEndTime() != null && now.isAfter(seckill.getEndTime())) {
            return result(7002, "秒杀活动已结束", null, false);
        }

        // 预热库存到 Redis（首次或库存未初始化时）
        String stockKey = STOCK_KEY_PREFIX + seckillGoodId;
        preloadStock(stockKey, sg);

        // Redis + Lua 原子扣减
        String userKey = USER_KEY_PREFIX + memberId + ":" + seckillGoodId;
        Long resultCode;
        try {
            resultCode = stringRedisTemplate.execute(grabScript,
                    List.of(stockKey, userKey),
                    String.valueOf(1), String.valueOf(sg.getLimitPerUser() == null ? 1 : sg.getLimitPerUser()));
        } catch (Exception e) {
            log.error("秒杀 Lua 执行异常", e);
            return result(500, "抢购系统繁忙", null, false);
        }

        if (resultCode == null) {
            return result(500, "抢购系统繁忙", null, false);
        }
        switch (resultCode.intValue()) {
            case 1 -> {
                // 扣减成功：登记订单映射（供超时回补反查）→ 发异步下单消息
                String seckillNo = SnowFlakeNoGenerator.nextSeckillNo();
                try {
                    // 值里带上 memberId，回补时才知道该释放谁的"已抢"记录
                    stringRedisTemplate.opsForValue()
                            .set(ORDER_KEY_PREFIX + seckillNo, seckillGoodId + ":" + memberId);
                } catch (Exception e) {
                    log.warn("登记秒杀订单映射失败：seckillNo={}", seckillNo, e);
                }
                SeckillGrabMessage msg = SeckillGrabMessage.builder()
                        .seckillNo(seckillNo)
                        .seckillGoodId(seckillGoodId)
                        .goodId(sg.getGoodId())
                        .seckillPrice(sg.getSeckillPrice())
                        .qty(1)
                        .memberId(memberId)
                        .memberAccount(memberAccount)
                        .build();
                rabbitTemplate.convertAndSend(SeckillMqConfig.SECKILL_EXCHANGE, SeckillMqConfig.SECKILL_ROUTING_KEY, msg);
                return result(200, "抢购提交成功，请等待结果", seckillNo, true);
            }
            case 2 -> {
                return result(7003, "已参与过该秒杀或超出限购", null, false);
            }
            case 3 -> {
                return result(7004, "秒杀已售罄", null, false);
            }
            default -> {
                return result(500, "抢购系统繁忙", null, false);
            }
        }
    }

    @Override
    public void restockSeckillOrder(String seckillNo) {
        if (seckillNo == null || seckillNo.isBlank()) {
            return;
        }
        // 反查（由抢购时登记）。值格式 "seckillGoodId:memberId"；
        // 老键可能只有 seckillGoodId，此时 memberId 取不到，跳过释放名额那一步即可。
        String mapping = stringRedisTemplate.opsForValue().get(ORDER_KEY_PREFIX + seckillNo);
        if (mapping == null) {
            log.warn("秒杀回补：未找到订单{}对应的秒杀商品映射，可能已回补或映射丢失", seckillNo);
            return;
        }
        String[] parts = mapping.split(":");
        Long seckillGoodId = Long.valueOf(parts[0]);
        Long memberId = parts.length > 1 ? Long.valueOf(parts[1]) : null;
        // 回补 Redis 秒杀库存
        try {
            String stockKey = STOCK_KEY_PREFIX + seckillGoodId;
            Long cur = stringRedisTemplate.opsForValue().increment(stockKey, 1);
            log.info("秒杀回补：seckillNo={}, seckillGoodId={}, redis剩余={}", seckillNo, seckillGoodId, cur);
        } catch (Exception e) {
            log.warn("回补Redis秒杀库存失败：seckillGoodId={}", seckillGoodId, e);
        }
        // 回补 DB seckill_good.stock/sold
        seckillGoodMapper.increaseStock(seckillGoodId, 1);
        // 释放该会员在本场秒杀的"已抢"记录 —— 名额要随库存一起回到可抢状态。
        // 需求 7.3-5「超时关闭后名额/库存回补」、5.9「超时未支付释放名额」都要求如此。
        // 原先这里只回补库存、不释放名额，结果是：订单取消了、库存也回来了，
        // 但该会员永远不能再抢同一商品（一直返回 7003），与需求冲突。
        if (memberId != null) {
            try {
                stringRedisTemplate.delete(USER_KEY_PREFIX + memberId + ":" + seckillGoodId);
                log.info("秒杀回补：已释放名额 memberId={}, seckillGoodId={}", memberId, seckillGoodId);
            } catch (Exception e) {
                log.warn("释放秒杀名额失败：memberId={}, seckillGoodId={}", memberId, seckillGoodId, e);
            }
        }
        // 清理订单映射（幂等，避免重复回补）。必须放在释放名额之后
        stringRedisTemplate.delete(ORDER_KEY_PREFIX + seckillNo);
    }

    private void preloadStock(String stockKey, SeckillGoodEntity sg) {
        try {
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(stockKey))) {
                stringRedisTemplate.opsForValue().set(stockKey,
                        String.valueOf(sg.getStock() == null ? 0 : sg.getStock()));
            }
        } catch (Exception e) {
            log.warn("预热秒杀库存失败", e);
        }
    }

    private SeckillGrabResult result(int code, String msg, String orderNo, boolean success) {
        return SeckillGrabResult.builder()
                .code(code).msg(msg).orderNo(orderNo).success(success).build();
    }
}
