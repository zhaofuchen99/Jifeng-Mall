<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { money } from '@/utils/format'

const props = defineProps({
  good: { type: Object, required: true }
})

const router = useRouter()

/** 下架或已删除都不能购买（需求 7.1-3），卡片上也要给出视觉提示 */
const offShelf = computed(() => props.good.isTakeDown === true || props.good.isDel === true)

function toDetail() {
  router.push({ name: 'goodDetail', params: { id: props.good.id } })
}

/**
 * 种子数据里的商品图（/upload/good/iphone.png、mate.png）项目里并不存在，
 * 直接展示会是一片破图，这里统一退化成占位图。
 */
function onImgError(e) {
  e.target.src = '/img-placeholder.svg'
}
</script>

<template>
  <div class="good-card" @click="toDetail">
    <div class="good-card__pic">
      <img :src="good.pic || '/img-placeholder.svg'" :alt="good.name" @error="onImgError" />
      <span v-if="offShelf" class="tag-corner tag-takedown">已下架</span>
      <span v-else-if="good.isSeckill" class="tag-corner">秒杀</span>
    </div>
    <div class="good-card__body">
      <div class="good-card__name" :title="good.name">{{ good.name }}</div>
      <div>
        <span class="price good-card__price">
          <span class="symbol">¥</span>{{ money(good.price) }}
        </span>
        <span v-if="good.markPrice && Number(good.markPrice) > Number(good.price)" class="price-origin">
          ¥{{ money(good.markPrice) }}
        </span>
      </div>
      <div class="good-card__meta">
        <span>{{ good.brand?.name || good.alias || '' }}</span>
        <span :class="{ 'text-muted': (good.qty ?? 0) > 0 }">
          {{ (good.qty ?? 0) > 0 ? `库存 ${good.qty}` : '已售罄' }}
        </span>
      </div>
    </div>
  </div>
</template>
