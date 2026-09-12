<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { parseTime } from '@/utils/format'

const props = defineProps({
  /** 目标时间，"yyyy-MM-dd HH:mm:ss" */
  endTime: { type: String, required: true },
  /** 倒计时结束前的提示文案前缀 */
  prefix: { type: String, default: '距结束' },
  /** 结束后是否隐藏整块 */
  hideOnFinish: { type: Boolean, default: false }
})

const emit = defineEmits(['finish'])

const remain = ref(0)
let timer = null

function tick() {
  remain.value = Math.max(0, parseTime(props.endTime) - Date.now())
  if (remain.value === 0) {
    clearInterval(timer)
    timer = null
    emit('finish')
  }
}

onMounted(() => {
  tick()
  if (remain.value > 0) timer = setInterval(tick, 1000)
})

onBeforeUnmount(() => timer && clearInterval(timer))

const parts = computed(() => {
  const total = Math.floor(remain.value / 1000)
  return {
    d: Math.floor(total / 86400),
    h: String(Math.floor((total % 86400) / 3600)).padStart(2, '0'),
    m: String(Math.floor((total % 3600) / 60)).padStart(2, '0'),
    s: String(total % 60).padStart(2, '0')
  }
})
</script>

<template>
  <span v-if="!(hideOnFinish && remain === 0)" class="countdown">
    <span class="countdown__prefix">{{ prefix }}</span>
    <span v-if="parts.d > 0" class="countdown__num">{{ parts.d }}</span>
    <span v-if="parts.d > 0" class="countdown__unit">天</span>
    <span class="countdown__num">{{ parts.h }}</span>
    <span class="countdown__colon">:</span>
    <span class="countdown__num">{{ parts.m }}</span>
    <span class="countdown__colon">:</span>
    <span class="countdown__num">{{ parts.s }}</span>
  </span>
</template>

<style scoped>
.countdown {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.countdown__prefix {
  margin-right: 6px;
}

.countdown__num {
  display: inline-block;
  min-width: 22px;
  padding: 0 4px;
  border-radius: 4px;
  background: #2b2f36;
  color: #fff;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.countdown__colon {
  color: #2b2f36;
  font-weight: 700;
}

.countdown__unit {
  margin: 0 2px;
}
</style>
