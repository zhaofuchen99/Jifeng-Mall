<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/**
 * 帮助中心。页脚的每一栏都指向这里的一个锚点。
 * 内容按演示项目的实际情况写——比如「模拟支付」如实说明不接真实支付渠道，
 * 别写成真的能收货、能退款的电商条款。
 */
const SECTIONS = [
  { id: 'flow', title: '购物流程', icon: 'ShoppingCart' },
  { id: 'faq', title: '常见问题', icon: 'QuestionFilled' },
  { id: 'delivery-area', title: '配送范围', icon: 'Location' },
  { id: 'delivery-time', title: '配送时效', icon: 'Van' },
  { id: 'delivery-fee', title: '运费说明', icon: 'Money' },
  { id: 'pay', title: '支付说明', icon: 'CreditCard' },
  { id: 'refund', title: '退换货政策', icon: 'RefreshLeft' },
  { id: 'cancel', title: '取消订单', icon: 'CircleClose' }
]

const activeId = ref(SECTIONS[0].id)

const FAQS = [
  {
    q: '忘记密码了怎么办？',
    a: '当前版本没有找回密码入口。可以用新账号重新注册；若已登录，到「个人中心 → 修改密码」用旧密码更换。'
  },
  {
    q: '为什么下单后订单一直停在「待付款」？',
    a: '提交订单只是锁定库存，还需要到收银台完成支付。待付款订单 30 分钟未支付会被系统自动关闭，库存随即释放。'
  },
  {
    q: '「已支付」之后为什么一直没发货？',
    a: '发货是后台管理端的操作。演示项目里可以在后台「订单管理」里对已支付订单点发货，状态才会变成「待收货」。'
  },
  {
    q: '秒杀提示「已参与过该秒杀」是怎么回事？',
    a: '同一会员对同一秒杀商品限购一次。如果订单被取消或超时关闭，名额会随库存一起释放，之后可以重新抢购。'
  },
  {
    q: '商品图片显示不出来？',
    a: '演示商品的图片来自厂商官方素材，需要手工放到上传目录。若未放置，页面会显示灰色占位图，不影响浏览与下单。'
  }
]

onMounted(() => {
  // 从页脚带 #锚点 进来时，高亮对应的目录项
  if (route.hash) {
    const id = route.hash.replace('#', '')
    if (SECTIONS.some((s) => s.id === id)) activeId.value = id
  }
  // 滚动时同步高亮（简单实现：取最靠近顶部的小节）
  window.addEventListener('scroll', onScroll, { passive: true })
})

function onScroll() {
  for (const s of SECTIONS) {
    const el = document.getElementById(s.id)
    if (el && el.getBoundingClientRect().top <= 140) activeId.value = s.id
  }
}

function toSection(id) {
  activeId.value = id
  router.replace({ name: 'help', hash: `#${id}` })
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function toOrders() {
  if (userStore.isLoggedIn) router.push({ name: 'userOrders' })
  else router.push({ name: 'login', query: { redirect: '/user/orders' } })
}
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="section-head">
        <h3 class="section-title">帮助中心</h3>
        <span class="text-muted">演示项目，以下说明按本项目的实际行为编写</span>
      </div>

      <div class="help">
        <!-- 目录 -->
        <aside class="toc">
          <a
            v-for="s in SECTIONS"
            :key="s.id"
            class="toc__item"
            :class="{ 'is-active': activeId === s.id }"
            @click.prevent="toSection(s.id)"
          >
            <el-icon><component :is="s.icon" /></el-icon>
            {{ s.title }}
          </a>
        </aside>

        <!-- 正文 -->
        <section class="doc panel">
          <!-- 购物流程 -->
          <article id="flow" class="block">
            <h4>购物流程</h4>
            <ol class="steps">
              <li>
                <b>注册 / 登录</b>
                <p>点击右上角「免费注册」创建账号，或直接用 <code>member / 123456</code> 登录演示账号。</p>
              </li>
              <li>
                <b>挑选商品</b>
                <p>首页或「全部商品」按分类、品牌、价格区间筛选，也可以直接搜索商品名。</p>
              </li>
              <li>
                <b>加入购物车 / 立即购买</b>
                <p>商品详情页可以调整数量（上限为当前库存），加入购物车后支持多件一起结算。</p>
              </li>
              <li>
                <b>确认订单</b>
                <p>选择收货地址（可新增，省市区三级联动）、填写备注，提交后生成待付款订单并锁定库存。</p>
              </li>
              <li>
                <b>支付</b>
                <p>进入模拟收银台确认支付，订单变为「已支付」，并生成一个 MOCK 开头的模拟交易号。</p>
              </li>
              <li>
                <b>发货与收货</b>
                <p>后台发货后订单变为「待收货」，你在「我的订单」点「确认收货」即完成。</p>
              </li>
            </ol>
            <el-alert type="info" :closable="false" show-icon title="秒杀商品走的是另一条链路">
              抢购是异步处理的：提交后会返回一个流水号，订单由后台消费者生成，页面会自动轮询结果。
            </el-alert>
          </article>

          <!-- 常见问题 -->
          <article id="faq" class="block">
            <h4>常见问题</h4>
            <el-collapse>
              <el-collapse-item v-for="(f, i) in FAQS" :key="i" :title="f.q">
                <p class="faq-a">{{ f.a }}</p>
              </el-collapse-item>
            </el-collapse>
          </article>

          <!-- 配送范围 -->
          <article id="delivery-area" class="block">
            <h4>配送范围</h4>
            <p>
              演示项目<b>没有真实的物流环节</b>。收货地址里的省市区来自
              <code>t_china_region</code> 表，目前只内置了少量示例数据（广东省 → 广州市 → 天河区、
              北京市 → 北京市市辖区）。
            </p>
            <p>
              因此地址选择器只有这几个选项可以选通。要覆盖更多地区，需要往该表里补数据，
              省市区三级通过 <code>parent_id</code> 关联，区划 ID 用的是真实国标编码
              （如 440106 = 广州市天河区）。
            </p>
          </article>

          <!-- 配送时效 -->
          <article id="delivery-time" class="block">
            <h4>配送时效</h4>
            <p>
              无真实配送。订单状态里的「发货」「待收货」「确认收货」都只是状态机上的流转，
              用来演示完整的交易链路；后台点发货后状态立刻变化，不涉及任何实际运输。
            </p>
            <p class="text-muted">
              唯一带真实「时间」语义的是<b>超时关单</b>：待付款订单 30 分钟内未支付，
              由 RabbitMQ 延迟消息触发自动关闭并回补库存。
            </p>
          </article>

          <!-- 运费说明 -->
          <article id="delivery-fee" class="block">
            <h4>运费说明</h4>
            <p>
              全场包邮，订单金额即商品小计，不额外计算运费。这也是简化实现——
              订单表里没有独立的运费字段，<code>total_pay</code> 直接等于各明细的
              成交价 × 数量之和。
            </p>
          </article>

          <!-- 支付说明 -->
          <article id="pay" class="block">
            <h4>支付说明</h4>
            <el-alert
              type="warning"
              :closable="false"
              show-icon
              title="这是模拟支付，不接入任何真实支付渠道"
              description="不会产生真实扣款，也不需要绑定任何支付账号。"
            />
            <ul class="bullets mt-16">
              <li>下单后进入的是<b>模拟收银台</b>，只有「模拟支付」一种方式。</li>
              <li>点「确认支付」后订单立即变为「已支付」，服务端会生成一个 <code>MOCK</code> 开头的交易流水号。</li>
              <li>支付接口分两步：<code>POST /api/orders/&#123;id&#125;/pay</code> 只做状态校验，
                <code>.../pay/confirm</code> 才真正落状态——这是接口设计上的划分，前端会依次调用。</li>
              <li>待付款订单有 <b>30 分钟</b>支付时限，收银台页面会显示倒计时。</li>
            </ul>
          </article>

          <!-- 退换货政策 -->
          <article id="refund" class="block">
            <h4>退换货政策</h4>
            <p>
              演示项目<b>没有实现退货退款流程</b>。订单表里有 <code>refund_status</code> 字段
              （无退款 / 退款中 / 已退款），但没有任何接口会去修改它，前端也没有退货入口。
            </p>
            <p>
              想「反悔」的话，实际可用的操作是<b>取消订单</b>——但仅限「待付款」状态，
              取消后库存会回补。已支付的订单目前无法取消或退款。
            </p>
          </article>

          <!-- 取消订单 -->
          <article id="cancel" class="block">
            <h4>取消订单</h4>
            <p>订单在以下情况下会被关闭，两种途径都会<b>回补库存</b>：</p>
            <ul class="bullets">
              <li>
                <b>手动取消</b> —— 在「我的订单」里对待付款订单点「取消」，或从订单详情页取消。
              </li>
              <li>
                <b>超时自动关闭</b> —— 提交订单后 30 分钟内未支付，服务端通过 RabbitMQ
                延迟消息自动关闭，无需你操作。
              </li>
            </ul>
            <p class="mt-16">
              秒杀订单被取消时，除了库存，<b>抢购名额也会一并释放</b>，之后可以重新抢同一件商品。
            </p>
            <el-button type="primary" class="mt-16" @click="toOrders">
              去我的订单
            </el-button>
          </article>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.help {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 16px;
  align-items: start;
}

/* 目录 */
.toc {
  position: sticky;
  top: 160px;
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  padding: 8px 0;
}

.toc__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  color: var(--text-2);
  font-size: 14px;
  cursor: pointer;
  border-left: 3px solid transparent;
}

.toc__item:hover {
  background: var(--bg-page);
  color: var(--brand);
}

.toc__item.is-active {
  background: var(--brand-bg);
  color: var(--brand);
  border-left-color: var(--brand);
  font-weight: 600;
}

/* 正文 */
.doc {
  padding: 8px 28px 28px;
}

.block {
  padding: 24px 0;
  border-bottom: 1px solid var(--line);
  /* 锚点跳转时预留顶部粘性导航的高度，否则标题会被盖住 */
  scroll-margin-top: 150px;
}

.block:last-child {
  border-bottom: none;
}

.block h4 {
  font-size: 18px;
  margin-bottom: 14px;
  padding-left: 10px;
  border-left: 4px solid var(--brand);
  line-height: 1.2;
}

.block p {
  margin: 0 0 10px;
  color: var(--text-2);
  line-height: 1.9;
}

.steps {
  margin: 0;
  padding-left: 20px;
}

.steps li {
  margin-bottom: 14px;
  color: var(--text-2);
  line-height: 1.8;
}

.steps li b {
  color: var(--text-1);
}

.steps li p {
  margin: 2px 0 0;
}

.bullets {
  margin: 0;
  padding-left: 20px;
  color: var(--text-2);
  line-height: 2;
}

.faq-a {
  margin: 0;
  color: var(--text-2);
  line-height: 1.9;
}

code {
  background: var(--bg-page);
  padding: 1px 6px;
  border-radius: 4px;
  color: var(--brand);
  font-size: 13px;
}

@media (max-width: 900px) {
  .help {
    grid-template-columns: 1fr;
  }
  .toc {
    position: static;
  }
}
</style>
