package com.situ.jifeng.common;

/**
 * 雪花 ID 生成（订单号、秒杀流水号等全局唯一编号）。
 *
 * <p>依据设计文档：订单号全局唯一，支持高并发生成，采用雪花算法（纯 JDK 实现，无第三方依赖）。
 * 结构：符号位(1) + 时间戳(41) + 工作机器位(10) + 序列位(12)。</p>
 */
public final class SnowFlakeNoGenerator {

    private static final long EPOCH = 1600000000000L; // 起始时间戳
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private static final long WORKER_ID = 1L;
    private static final long DATACENTER_ID = 1L;

    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    private SnowFlakeNoGenerator() {
    }

    private static synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("时钟回拨，拒绝生成 ID");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(timestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (DATACENTER_ID << DATACENTER_ID_SHIFT)
                | (WORKER_ID << WORKER_ID_SHIFT)
                | sequence;
    }

    private static long tilNextMillis(long lastTimestamp) {
        long ts = System.currentTimeMillis();
        while (ts <= lastTimestamp) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }

    /** 生成订单号 */
    public static String nextOrderNo() {
        return String.valueOf(nextId());
    }

    /** 生成秒杀流水号 */
    public static String nextSeckillNo() {
        return String.valueOf(nextId());
    }

    /** 生成模拟支付交易号 */
    public static String nextTradeNo() {
        return "MOCK" + nextId();
    }
}
