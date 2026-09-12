<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as authApi from '@/api/auth'
import { useUserStore } from '@/stores/user'
import ImageUpload from '@/components/ImageUpload.vue'
import { datetime } from '@/utils/format'

/**
 * 个人中心：当前登录管理员维护自己的头像与备注。
 *
 * 只提交 id / avatarUrl / description 三个字段。
 * PUT /api/users 是 mapper 里每个字段都带 <if> 的动态更新，传多余字段只会引入风险：
 * password 一旦非空就会被 encodePassword 当成新密码重新 BCrypt 加密，
 * 等于把管理员自己的密码改成没人知道的哈希值。
 *
 * ⚠️ 后端 user-api 没有独立的改密接口，所以这个页面不提供改密码。
 */
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const detail = ref({})

async function load() {
  const id = userStore.userId
  if (!id) return

  loading.value = true
  try {
    const data = await authApi.getUserById(id)
    detail.value = { ...data, avatarUrl: data?.avatarUrl || '' }
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    await authApi.updateUser({
      id: detail.value.id,
      avatarUrl: detail.value.avatarUrl,
      description: detail.value.description
    })
    ElMessage.success('资料已保存')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-alert
      title="修改密码请到系统管理里由管理员重置"
      description="后端没有开放独立的改密接口，本页只能维护头像与备注。"
      type="info"
      :closable="false"
      show-icon
      class="mb-16"
    />

    <div class="table-card">
      <div class="table-toolbar">
        <span class="table-toolbar__title">个人资料</span>
        <div class="table-toolbar__actions">
          <el-button @click="load">
            <el-icon><Refresh /></el-icon> 重新加载
          </el-button>
        </div>
      </div>

      <el-form v-loading="loading" label-width="100px" @submit.prevent>
        <el-form-item label="头像">
          <ImageUpload v-model="detail.avatarUrl" type="member" tip="建议正方形，≤ 10MB" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="detail.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="个人备注，如岗位、联系方式"
          />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">账号信息（只读）</el-divider>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ detail.username || '—' }}</el-descriptions-item>
        <el-descriptions-item label="登录次数">{{ detail.loginTimes ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="上次登录时间">{{ datetime(detail.lastLoginTime) }}</el-descriptions-item>
        <el-descriptions-item label="上次登录 IP">{{ detail.lastLoginIp || '—' }}</el-descriptions-item>
      </el-descriptions>

      <div class="form-actions">
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
        <el-button @click="load">放弃修改</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}
</style>
