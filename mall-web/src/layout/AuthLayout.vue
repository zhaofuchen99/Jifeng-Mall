<script setup>
/**
 * 登录 / 注册共用的外壳：左侧品牌区 + 右侧表单插槽。
 */
defineProps({
  slogan: { type: String, default: '好物直达\n秒杀不等待' }
})
</script>

<template>
  <div class="auth">
    <aside class="auth__brand">
      <router-link :to="{ name: 'home' }" class="auth__logo">
        <span class="logo__mark">极</span>
        <span class="logo__text">极锋<em>商城</em></span>
      </router-link>
      <h1 class="auth__slogan">{{ slogan }}</h1>
      <p class="auth__desc">基于 Spring Cloud 的 B2C 微服务秒杀商城</p>
      <ul class="auth__points">
        <li>正品保障 · 品牌直营</li>
        <li>限时秒杀 · 每日更新</li>
        <li>极速配送 · 七天无理由</li>
      </ul>
    </aside>

    <section class="auth__panel">
      <div class="auth__box">
        <slot />
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  min-height: 100vh;
}

.auth__brand {
  background: linear-gradient(150deg, #2b2f36 0%, #3c2226 55%, #6d2029 100%);
  color: #fff;
  padding: 64px 72px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth__logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 48px;
}

.logo__mark {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--accent), var(--brand));
  font-size: 26px;
  font-weight: 700;
  display: grid;
  place-items: center;
}

.logo__text {
  font-size: 26px;
  font-weight: 700;
}

.logo__text em {
  font-style: normal;
  color: var(--brand-light);
}

.auth__slogan {
  font-size: 44px;
  line-height: 1.3;
  letter-spacing: 2px;
  margin-bottom: 16px;
  white-space: pre-line;
}

.auth__desc {
  color: rgba(255, 255, 255, 0.65);
  font-size: 15px;
  margin: 0 0 40px;
}

.auth__points {
  list-style: none;
  padding: 0;
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
}

.auth__points li {
  position: relative;
  padding-left: 20px;
  margin-bottom: 10px;
}

.auth__points li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand-light);
}

.auth__panel {
  display: grid;
  place-items: center;
  padding: 40px;
}

.auth__box {
  width: 100%;
  max-width: 380px;
}

@media (max-width: 900px) {
  .auth {
    grid-template-columns: 1fr;
  }
  .auth__brand {
    display: none;
  }
}
</style>
