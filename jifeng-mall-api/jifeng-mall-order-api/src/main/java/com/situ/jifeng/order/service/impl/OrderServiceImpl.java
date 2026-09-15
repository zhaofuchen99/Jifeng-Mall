package com.situ.jifeng.order.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.common.SnowFlakeNoGenerator;
import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.order.config.OrderDelayMessageSender;
import com.situ.jifeng.order.mapper.OrderItemMapper;
import com.situ.jifeng.order.mapper.OrderMapper;
import com.situ.jifeng.order.service.AddressFeignService;
import com.situ.jifeng.order.service.CartFeignService;
import com.situ.jifeng.order.service.GoodFeignService;
import com.situ.jifeng.order.service.SeckillFeignService;
import com.situ.jifeng.spi.model.CartItemEntity;
import com.situ.jifeng.spi.model.RestockParam;
import com.situ.jifeng.spi.model.DeductStockDto;
import com.situ.jifeng.spi.model.GoodEntity;
import com.situ.jifeng.spi.model.MemberAddressEntity;
import com.situ.jifeng.spi.model.OrderCreateDTO;
import com.situ.jifeng.spi.model.OrderEntity;
import com.situ.jifeng.spi.model.OrderItemEntity;
import com.situ.jifeng.spi.model.search.OrderSearchBean;
import com.situ.jifeng.spi.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    /**
     * 订单状态字典。本常量是 public 的：超时关单的兜底扫描
     * （{@code OrderTimeoutCloseTask}）需要拿它做查询条件，
     * 与其在调度器里再写一份字面量、日后改字典时漏改，不如共用一处。
     */
    public static final String STATUS_PENDING_PAY = "待付款";
    static final String STATUS_PAID = "已支付";
    static final String STATUS_SHIPPED = "待收货";
    static final String STATUS_CONFIRMED = "已确认";
    static final String STATUS_CANCELED = "已取消";

    /** 退款状态（需求 4.4 / 详细设计 3.5.2）：无退款 → 退款中 → 已退款 */
    static final String REFUND_NONE = "无退款";
    static final String REFUND_APPLYING = "退款中";
    static final String REFUND_DONE = "已退款";

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private GoodFeignService goodFeignService;
    private CartFeignService cartFeignService;
    private AddressFeignService addressFeignService;
    private SeckillFeignService seckillFeignService;
    private OrderDelayMessageSender orderDelayMessageSender;

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Autowired
    public void setOrderItemMapper(OrderItemMapper orderItemMapper) {
        this.orderItemMapper = orderItemMapper;
    }

    @Autowired
    public void setGoodFeignService(GoodFeignService goodFeignService) {
        this.goodFeignService = goodFeignService;
    }

    @Autowired
    public void setCartFeignService(CartFeignService cartFeignService) {
        this.cartFeignService = cartFeignService;
    }

    @Autowired
    public void setAddressFeignService(AddressFeignService addressFeignService) {
        this.addressFeignService = addressFeignService;
    }

    @Autowired
    public void setOrderDelayMessageSender(OrderDelayMessageSender orderDelayMessageSender) {
        this.orderDelayMessageSender = orderDelayMessageSender;
    }

    @Autowired
    public void setSeckillFeignService(SeckillFeignService seckillFeignService) {
        this.seckillFeignService = seckillFeignService;
    }

    @Override
    public List<OrderEntity> findAll(OrderSearchBean osb, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return orderMapper.findAll(osb);
        }
    }

    @Override
    public OrderEntity findById(Long id) {
        return orderMapper.findById(id);
    }

    @Override
    public OrderEntity findByOrderNo(String orderNo) {
        return orderMapper.findByOrderNo(orderNo);
    }

    @Override
    public OrderEntity findBySeckillNo(String seckillNo) {
        if (seckillNo == null || seckillNo.isBlank()) {
            return null;
        }
        return orderMapper.findBySeckillNo(seckillNo);
    }

    @Override
    public List<OrderEntity> findByMemberAccount(String memberAccount) {
        OrderSearchBean osb = new OrderSearchBean();
        osb.setMemberAccount(memberAccount);
        return findAll(osb, PaginateInfo.from(1, 0));
    }

    @Override
    public boolean save(OrderEntity orderEntity) {
        return orderMapper.save(orderEntity) > 0;
    }

    @Override
    public boolean update(OrderEntity orderEntity) {
        return orderMapper.update(orderEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return orderMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderEntity create(OrderCreateDTO dto) {
        // 1. 校验入参与收货地址
        if (dto.getMemberAccount() == null || dto.getMemberAccount().isBlank()) {
            throw new BusinessException(400, "会员账号不能为空");
        }
        MemberAddressEntity addr = fetchAddress(dto.getAddrId());
        // 收货地址必须是下单人自己的。原先只按 addrId 取地址、不比对归属，
        // 会员传一个别人的地址 id 就能把订单寄到他人地址上。
        // 这里不放在 web 层校验：地址已经在服务里取到手了，没必要再查一次。
        if (!dto.getMemberAccount().equals(addr.getMemberAccount())) {
            throw new BusinessException(403, "收货地址不属于当前会员");
        }
        BigDecimal totalPay = BigDecimal.ZERO;
        List<OrderCreateDTO> goodsQueue = new ArrayList<>();

        // 2. 解析下单商品来源：购物车 / 立即购买
        if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
            for (Long cartId : dto.getCartIds()) {
                TypedJsonResp<CartItemEntity> resp = cartFeignService.findById(cartId);
                CartItemEntity item = resp.getData();
                if (item == null) {
                    throw new BusinessException(400, "购物车条目不存在");
                }
                // 购物车条目必须是下单人自己的。购物车主键是自增的，
                // 不校验的话会员传别人的 cartId 就能把他人购物车里的商品下成自己的订单，
                // 顺带还会把对方的购物车条目清掉。
                // dto.memberId 由 OrderApi 从网关注入的 X-User-Id 填入，前端无法伪造。
                if (dto.getMemberId() != null && !dto.getMemberId().equals(item.getMemberId())) {
                    throw new BusinessException(403, "购物车条目不属于当前会员");
                }
                OrderCreateDTO placeholder = new OrderCreateDTO();
                placeholder.setGoodId(item.getGoodId());
                placeholder.setQty(item.getQty());
                goodsQueue.add(placeholder);
            }
        } else if (dto.getGoodId() != null && dto.getQty() != null && dto.getQty() > 0) {
            OrderCreateDTO single = new OrderCreateDTO();
            single.setGoodId(dto.getGoodId());
            single.setQty(dto.getQty());
            goodsQueue.add(single);
        } else {
            throw new BusinessException(400, "下单商品信息为空");
        }

        // 3. 生成订单（雪花 ID，待付款）
        OrderEntity order = buildOrder(dto, addr);
        order.setOrderNo(SnowFlakeNoGenerator.nextOrderNo());
        // order.total_pay 为 NOT NULL，且 insert 语句显式带该字段；但金额要遍历完商品明细
        // 才算得出来，而订单必须先落库拿到 id 才能挂明细。故此处先置 0 占位，
        // 第 5 步按明细汇总后再 update 为真实金额。
        // 不设值会传 NULL，触发 "Column 'total_pay' cannot be null"——普通下单必然失败；
        // 秒杀路径 createSeckillOrder 因金额已知、save 前就赋了值，所以一直没暴露这个问题。
        // （同事务内，外部读者看不到 0 这个中间态）
        order.setTotalPay(BigDecimal.ZERO);
        orderMapper.save(order);

        // 4. 循环处理商品：校验 + 扣库存 + 生成明细
        for (OrderCreateDTO item : goodsQueue) {
            GoodEntity good = fetchGood(item.getGoodId());
            if (Boolean.TRUE.equals(good.getIsDel()) || Boolean.TRUE.equals(good.getIsTakeDown())) {
                throw new BusinessException(7005, "商品已下架或删除: " + good.getName());
            }
            if (good.getQty() == null || good.getQty() < item.getQty()) {
                throw new BusinessException(7001, "库存不足: " + good.getName());
            }
            boolean ok = goodFeignService.deductStock(good.getId(), new DeductStockDto() {{
                setQty(item.getQty());
            }}).isSuccess();
            if (!ok) {
                throw new BusinessException(7001, "库存不足: " + good.getName());
            }
            // 明细（商品快照）
            OrderItemEntity oi = new OrderItemEntity();
            oi.setOrderId(order.getId());
            oi.setGoodId(good.getId());
            oi.setDealPrice(good.getPrice());
            oi.setCount(item.getQty());
            oi.setGoodName(good.getName());
            oi.setGoodPic(good.getPic());
            oi.setGoodDesc(good.getSummary());
            orderItemMapper.save(oi);

            totalPay = totalPay.add(good.getPrice().multiply(BigDecimal.valueOf(item.getQty())));
        }

        // 5. 更新订单合计金额
        order.setTotalPay(totalPay);
        orderMapper.update(order);

        // 6. 下单成功后：清除购物车勾选项 + 发送超时关单消息
        if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
            try {
                cartFeignService.deleteByIds(dto.getCartIds().toArray(new Long[0]));
            } catch (Exception e) {
                log.warn("清空购物车失败（不影响主流程）", e);
            }
        }
        orderDelayMessageSender.sendCloseDelay(order.getId(), OrderDelayMessageSender.DEFAULT_TIMEOUT);

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long orderId) {
        OrderEntity order = requireOrder(orderId);
        if (!STATUS_PENDING_PAY.equals(order.getStatus())) {
            return false;
        }
        order.setStatus(STATUS_CANCELED);
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.update(order);
        restockForOrder(order);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(Long orderId) {
        OrderEntity order = requireOrder(orderId);
        if (!STATUS_SHIPPED.equals(order.getStatus())) {
            return false;
        }
        // 退款在途的订单不该再被确认收货：确认后订单是终态「已确认」，
        // 退款一旦确认又要翻成「已取消」，状态机会自相矛盾。
        if (isRefundInvolved(order)) {
            return false;
        }
        order.setStatus(STATUS_CONFIRMED);
        order.setAcceptTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.update(order);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean ship(Long orderId) {
        OrderEntity order = requireOrder(orderId);
        if (!STATUS_PAID.equals(order.getStatus())) {
            return false;
        }
        // 退款在途的订单不能再发货。发起退款后订单状态仍是「已支付」，
        // 不加这道判断，正在退款的订单会被发出去。
        if (isRefundInvolved(order)) {
            return false;
        }
        order.setStatus(STATUS_SHIPPED);
        order.setShipTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.update(order);
        return true;
    }

    @Override
    public boolean pay(Long orderId) {
        OrderEntity order = requireOrder(orderId);
        // 发起支付：仅待付款可发起
        return STATUS_PENDING_PAY.equals(order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmPay(Long orderId) {
        OrderEntity order = requireOrder(orderId);
        if (!STATUS_PENDING_PAY.equals(order.getStatus())) {
            return false;
        }
        order.setStatus(STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        order.setPayType("模拟支付");
        order.setAlipayTradeNo(SnowFlakeNoGenerator.nextTradeNo());
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.update(order);
        return true;
    }

    /**
     * 发起退款（模拟）：无退款 → 退款中。对应真实场景的「后台提交退款申请，等渠道处理」。
     * 与模拟支付一样分两步（发起 / 确认），退款状态才有「退款中」这个中间态可观察。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(Long orderId, String operator) {
        OrderEntity order = requireOrder(orderId);
        // 可退款的状态：已支付（未发货）与待收货（已发货）。
        // 需求 6.6 只写了「已支付订单发起退款」，这里把待收货一并放开是刻意放宽的：
        // 货已发出、款已收，会员要退，后台仍得能处理。状态字典里没有「已退款」这个
        // 订单状态，两种情形退完都落到「已取消」（终态），是不是退款单由 refund_status 区分。
        if (!STATUS_PAID.equals(order.getStatus()) && !STATUS_SHIPPED.equals(order.getStatus())) {
            return false;
        }
        // 幂等：只有「未退款」才能发起，重复点退款不会把已退款/退款中的订单再退一次
        if (!isRefundable(order)) {
            return false;
        }
        order.setRefundStatus(REFUND_APPLYING);
        order.setUpdatedTime(LocalDateTime.now());
        order.setUpdatedBy(operator);
        orderMapper.update(order);
        return true;
    }

    /**
     * 确认退款（模拟）：退款中 → 已退款，订单转已取消并回补库存。
     * 对应真实场景的「渠道退款成功回调」；模拟实现里由后台点确认触发。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refundConfirm(Long orderId, String operator) {
        OrderEntity order = requireOrder(orderId);
        if (!REFUND_APPLYING.equals(order.getRefundStatus())) {
            return false;
        }
        order.setRefundStatus(REFUND_DONE);
        order.setStatus(STATUS_CANCELED);
        order.setUpdatedTime(LocalDateTime.now());
        order.setUpdatedBy(operator);
        orderMapper.update(order);
        // 与取消订单同一套回补逻辑：退款成功等同于交易撤销，占用的库存要还回去
        restockForOrder(order);
        return true;
    }

    /** 尚未发起过退款（refund_status 为 NULL 的历史数据也视为未退款） */
    private boolean isRefundable(OrderEntity order) {
        return order.getRefundStatus() == null || REFUND_NONE.equals(order.getRefundStatus());
    }

    /** 退款已发起（退款中）或已完成（已退款）——这类订单不再参与履约（发货 / 确认收货） */
    private boolean isRefundInvolved(OrderEntity order) {
        return REFUND_APPLYING.equals(order.getRefundStatus())
                || REFUND_DONE.equals(order.getRefundStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderEntity createSeckillOrder(Long goodId, Integer qty, BigDecimal seckillPrice,
                                          String memberAccount, String seckillNo) {
        // 幂等：同一秒杀流水号仅创建一张订单，MQ 重投时不重复下单
        OrderEntity existed = findBySeckillNo(seckillNo);
        if (existed != null) {
            log.info("秒杀订单已存在，跳过重复创建：seckillNo={}, orderNo={}", seckillNo, existed.getOrderNo());
            return existed;
        }
        GoodEntity good = fetchGood(goodId);
        if (Boolean.TRUE.equals(good.getIsDel()) || Boolean.TRUE.equals(good.getIsTakeDown())) {
            throw new BusinessException(7005, "商品已下架或删除: " + good.getName());
        }
        // 秒杀订单：待付款，关联秒杀流水号，金额为秒杀价，不扣普通库存（秒杀库存已在 Redis 扣减）
        OrderEntity order = new OrderEntity();
        order.setOrderNo(SnowFlakeNoGenerator.nextOrderNo());
        order.setSeckillNo(seckillNo);
        order.setMemberAccount(memberAccount);
        order.setTotalPay(seckillPrice.multiply(BigDecimal.valueOf(qty)));
        order.setStatus(STATUS_PENDING_PAY);
        order.setCheckoutTime(LocalDateTime.now());
        order.setRefundStatus(REFUND_NONE);
        // order.is_del 列为 NOT NULL，且 insert 语句显式带该字段；
        // 不设值会传 NULL，触发 "Column 'is_del' cannot be null"（普通下单 buildOrder 里有设）
        order.setIsDel(false);
        order.setCreatedTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.save(order);

        OrderItemEntity oi = new OrderItemEntity();
        oi.setOrderId(order.getId());
        oi.setGoodId(good.getId());
        oi.setDealPrice(seckillPrice);
        oi.setCount(qty);
        oi.setGoodName(good.getName());
        oi.setGoodPic(good.getPic());
        oi.setGoodDesc(good.getSummary());
        orderItemMapper.save(oi);

        // 秒杀订单同样需要超时关单
        orderDelayMessageSender.sendCloseDelay(order.getId(), OrderDelayMessageSender.DEFAULT_TIMEOUT);
        return order;
    }

    // ---------------------------------------------------------------

    private MemberAddressEntity fetchAddress(Long addrId) {
        if (addrId == null) {
            throw new BusinessException(400, "请选择收货地址");
        }
        TypedJsonResp<MemberAddressEntity> resp = addressFeignService.findById(addrId);
        MemberAddressEntity addr = resp.getData();
        if (addr == null) {
            throw new BusinessException(400, "收货地址不存在");
        }
        return addr;
    }

    private OrderEntity buildOrder(OrderCreateDTO dto, MemberAddressEntity addr) {
        OrderEntity order = new OrderEntity();
        order.setMemberAccount(dto.getMemberAccount());
        order.setStatus(STATUS_PENDING_PAY);
        order.setCheckoutTime(LocalDateTime.now());
        order.setRefundStatus(REFUND_NONE);
        order.setReceiverAddrId(addr.getAddrId());
        order.setReceiverName(addr.getReceiver());
        order.setReceiverPhone(addr.getPhone());
        order.setReceiverAddrDetail(addr.getAddrDetail());
        order.setOrderComment(dto.getComment());
        order.setIsDel(false);
        order.setCreatedTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());
        return order;
    }

    private GoodEntity fetchGood(Long goodId) {
        TypedJsonResp<GoodEntity> resp = goodFeignService.findById(goodId, false);
        GoodEntity good = resp.getData();
        if (good == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return good;
    }

    private OrderEntity requireOrder(Long orderId) {
        OrderEntity order = orderMapper.findById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    /**
     * 归还订单占用的库存。取消订单与退款成功走的是同一条路径。
     *
     * <p>秒杀订单与普通订单的库存来源不同，两条路径互斥：</p>
     * <ul>
     *   <li><b>秒杀订单</b>：下单时未扣减普通库存 {@code good.qty}，只回补秒杀库存
     *       （Redis {@code seckill:stock:*} + {@code seckill_good.stock/sold}），
     *       并释放该会员的抢购名额</li>
     *   <li><b>普通订单</b>：按明细回补 {@code good.qty}</li>
     * </ul>
     */
    private void restockForOrder(OrderEntity order) {
        if (order.getSeckillNo() != null && !order.getSeckillNo().isBlank()) {
            try {
                RestockParam param = new RestockParam();
                param.setSeckillNo(order.getSeckillNo());
                seckillFeignService.restock(param);
            } catch (Exception e) {
                log.warn("回补秒杀库存失败：orderId={}, seckillNo={}", order.getId(), order.getSeckillNo(), e);
            }
        } else {
            refundStock(order.getId());
        }
    }

    private void refundStock(Long orderId) {
        List<OrderItemEntity> items = orderItemMapper.findByOrderId(orderId);
        for (OrderItemEntity oi : items) {
            try {
                goodFeignService.addBackStock(oi.getGoodId(), new DeductStockDto() {{
                    setQty(oi.getCount());
                }});
            } catch (Exception e) {
                log.warn("回补库存失败：order={}, good={}", orderId, oi.getGoodId(), e);
            }
        }
    }
}
