<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { seckillApi } from '@/api/seckill'
import { useCrud, useDialog } from '@/composables/useCrud'
import { datetime, toDateTimeString } from '@/utils/format'
import { SECKILL_STATUS_TAG, seckillStatus } from '@/utils/dict'

/**
 * 秒杀活动。
 *
 * ⚠️ 活动的「状态」不是数据库字段，而是由 enabled + 起止时间推导出来的
 * （需求 4.4）：未开始 / 进行中 / 已结束 / 已禁用。列表里显示的是推导值。
 */
const {
  rows, total, loading, selection, query, pageNo, size,
  load, search, reset, remove, onPageChange, onSizeChange
} = useCrud({
  api: seckillApi,
  label: '秒杀活动',
  pageSize: 10,
  query: { name: '', enabled: null }
})

const dlg = useDialog(() => ({
  name: '',
  enabled: true,
  startTime: '',
  endTime: '',
  description: ''
}))
const { visible, isEdit, submitting, form } = dlg
const formRef = ref()

const rules = {
  name: [
    { required: true, message: '请输入活动名称', trigger: 'blur' },
    { max: 100, message: '不超过 100 个字符', trigger: 'blur' }
  ],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    {
      validator: (_r, v, cb) => {
        if (!v || !form.value.startTime) return cb()
        v > form.value.startTime ? cb() : cb(new Error('结束时间必须晚于开始时间'))
      },
      trigger: 'change'
    }
  ]
}

const onSelectionChange = (v) => (selection.value = v)

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await seckillApi.update(form.value)
      ElMessage.success('活动已更新')
    } else {
      await seckillApi.save(form.value)
      ElMessage.success('活动已新增')
    }
    visible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

function onAdd() {
  dlg.open(null)
  // 默认给一个从现在开始、持续 24 小时的窗口，省得每次手工填
  const now = new Date()
  form.value.startTime = toDateTimeString(now)
  form.value.endTime = toDateTimeString(new Date(now.getTime() + 24 * 3600 * 1000))
  formRef.value?.clearValidate()
}

function onEdit(row) {
  dlg.open(row)
  formRef.value?.clearValidate()
}

/** 快捷延长：把结束时间推到 24 小时后（活动过期后常用） */
async function onExtend(row) {
  const now = new Date()
  const end = new Date(now.getTime() + 24 * 3600 * 1000)
  await seckillApi.update({
    id: row.id,
    startTime: toDateTimeString(now),
    endTime: toDateTimeString(end)
  })
  ElMessage.success('活动时间已延长至 24 小时后')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="活动名称">
          <el-input
            v-model="query.name"
            placeholder="支持模糊匹配"
            clearable
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-select v-model="query.enabled" clearable placeholder="全部" style="width: 110px">
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <span class="table-toolbar__title">秒杀活动</span>
        <div class="table-toolbar__actions">
          <el-button
            type="danger"
            plain
            :disabled="selection.length === 0"
            @click="remove(selection.map((r) => r.id))"
          >
            批量删除
          </el-button>
          <el-button type="primary" @click="onAdd">
            <el-icon><Plus /></el-icon> 新增活动
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column prop="name" label="活动名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="SECKILL_STATUS_TAG[seckillStatus(row)]" size="small">
              {{ seckillStatus(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small" effect="plain">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ datetime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="{ row }">{{ datetime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="onExtend(row)">延长24h</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无秒杀活动" :image-size="80" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog
      v-model="visible"
      :title="isEdit ? '编辑秒杀活动' : '新增秒杀活动'"
      width="580px"
      destroy-on-close
    >
      <el-alert
        class="mb-16"
        type="info"
        :closable="false"
        show-icon
        title="活动状态由「启用 + 起止时间」推导"
        description="只有时间窗内且启用的活动才能被抢购。活动结束后预热任务会静默退出，记得及时延长或新建。"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" placeholder="如：每周三秒杀" />
        </el-form-item>

        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>

        <el-form-item label="活动说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
