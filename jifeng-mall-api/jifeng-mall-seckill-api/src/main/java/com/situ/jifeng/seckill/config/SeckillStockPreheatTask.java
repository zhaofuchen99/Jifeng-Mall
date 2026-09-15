package com.situ.jifeng.seckill.config;

import com.situ.jifeng.seckill.mapper.SeckillGoodMapper;
import com.situ.jifeng.seckill.mapper.SeckillMapper;
import com.situ.jifeng.seckill.service.impl.SeckillServiceImpl;
import com.situ.jifeng.spi.model.SeckillEntity;
import com.situ.jifeng.spi.model.SeckillGoodEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 秒杀库存定时预热任务（设计文档 5.6：活动开始前将秒杀库存预热到 Redis）。
 *
 * <p>策略：</p>
 * <ul>
 *   <li>扫描启用且未结束的秒杀活动（{@code findActive}）下的全部秒杀商品；</li>
 *   <li>活动<b>未开始</b>：强制把 {@code seckill:stock:{seckillGoodId}} 刷新为 DB 剩余库存
 *       （此时不可能发生实时扣减，刷新安全）；</li>
 *   <li>活动<b>进行中</b>：仅在 key 缺失（如 Redis 重启/淘汰）时补齐，绝不覆盖实时扣减结果。</li>
 * </ul>
 *
 * <p>DB 侧 {@code seckill_good.stock} 随成交（decreaseStock）与关单回补（increaseStock）同步变动，
 * 因此预热写入的即为当前剩余库存。调度间隔可通过
 * {@code jifeng-mall.seckill.preheat-interval-ms} 配置，默认 60 秒。</p>
 */
@Component
public class SeckillStockPreheatTask {

    private static final Logger log = LoggerFactory.getLogger(SeckillStockPreheatTask.class);

    private SeckillMapper seckillMapper;
    private SeckillGoodMapper seckillGoodMapper;
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 上一次扫描到的「进行中活动」编号集合，用来做<b>状态变化时打日志</b>。
     *
     * <p>初值刻意是 null 而不是空集合：这样服务启动后的第一次扫描<b>必定</b>打印一行，
     * 运维不用猜「现在到底有没有进行中的活动、预热有没有在跑」。</p>
     *
     * <p>为什么是边沿触发而不是每次打：本任务每 60 秒跑一次，若每次无活动都打一行，
     * 一天就是 1440 行噪音，反而把真正要看的那一行淹了。</p>
     */
    private volatile Set<Long> lastActiveIds = null;

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

    @Scheduled(initialDelayString = "${jifeng-mall.seckill.preheat-initial-delay-ms:10000}",
            fixedDelayString = "${jifeng-mall.seckill.preheat-interval-ms:60000}")
    public void preheat() {
        List<SeckillEntity> actives;
        try {
            actives = seckillMapper.findActive();
        } catch (Exception e) {
            log.warn("秒杀库存预热：查询活动失败", e);
            return;
        }
        Set<Long> activeIds = (actives == null || actives.isEmpty())
                ? Set.of()
                : actives.stream().map(SeckillEntity::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        logActiveSetChange(activeIds);

        if (activeIds.isEmpty()) {
            // 没有进行中的活动 —— 不预热是正确行为，但必须让运维知道，
            // 否则「Redis 里没有 seckill:stock:*、抢购全报 7004」会变成无头案
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int total = 0;
        int written = 0;
        for (SeckillEntity seckill : actives) {
            List<SeckillGoodEntity> goods;
            try {
                goods = seckillGoodMapper.findBySeckillId(seckill.getId());
            } catch (Exception e) {
                log.warn("秒杀库存预热：查询活动商品失败，seckillId={}", seckill.getId(), e);
                continue;
            }
            if (goods == null || goods.isEmpty()) {
                continue;
            }
            boolean notStarted = seckill.getStartTime() != null && now.isBefore(seckill.getStartTime());
            for (SeckillGoodEntity sg : goods) {
                total++;
                String key = SeckillServiceImpl.STOCK_KEY_PREFIX + sg.getId();
                int remain = sg.getStock() == null ? 0 : Math.max(sg.getStock(), 0);
                try {
                    if (notStarted) {
                        stringRedisTemplate.opsForValue().set(key, String.valueOf(remain));
                        written++;
                    } else if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
                        stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(remain));
                        written++;
                    }
                } catch (Exception e) {
                    log.warn("预热秒杀库存失败：seckillGoodId={}", sg.getId(), e);
                }
            }
        }
        if (written > 0) {
            log.info("秒杀库存预热完成：活动 {} 个，商品 {} 个，写入 {} 个库存 key", actives.size(), total, written);
        }
    }

    /**
     * 只在「进行中的活动集合发生变化」时打日志（含启动后的第一次）。
     *
     * <p>没有活动时打 WARN 而不是悄悄 return —— 活动过期后
     * {@code findActive()} 返回空集、预热不再执行、Redis 里 {@code seckill:stock:*}
     * 随之消失，而此时抢购会全部返回 7004「已售罄」。
     * <b>原先这条路径一行日志都不留</b>，只能靠"抢购失败"这个间接现象反推，
     * 排查成本极高。现在过期那一刻就会留下明确记录。</p>
     */
    private void logActiveSetChange(Set<Long> activeIds) {
        if (activeIds.equals(lastActiveIds)) {
            return;
        }
        if (activeIds.isEmpty()) {
            log.warn("秒杀库存预热：当前没有进行中的秒杀活动（findActive 为空），不再预热库存。"
                            + "若 Redis 中 seckill:stock:* 缺失，抢购将全部返回 7004。"
                            + "请核对 seckill 表的 enabled / start_time / end_time（上一个活动集合={}）",
                    lastActiveIds);
        } else {
            log.info("秒杀库存预热：进行中的活动集合为 {}", activeIds);
        }
        lastActiveIds = activeIds;
    }
}
