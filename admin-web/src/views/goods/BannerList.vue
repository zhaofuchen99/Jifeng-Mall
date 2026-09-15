<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import bannerApi from '@/api/banner'
import { useCrud, useDialog } from '@/composables/useCrud'
import { datetime, onImgError } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'

/**
 * 首页轮播管理（需求 5.3 / FR-102）。
 *
 * 运营在这里配「图片 + 跳转链接 + 排序」，前台首页（mall-web/Home.vue）读
 * `GET /api/goods/banners?enabled=true` 渲染。
 *
 * 两点注意：
 * 1. 图片可以**不配**——前台会退回渐变底色，不会开天窗。所以图片不是必填。
 * 2. `sortNo` 越小越靠前；前台按 `sort_no, id` 排序。
 */
const {
  rows, total, loading, selection, query, pageNo, size,
  load, search, reset, remove, onPageChange, onSizeChange
} = useCrud({
  api: bannerApi,
  label: '轮播',
  pageSize: 10,
  query: { title: '', enabled: null }
})

const dlg = useDialog(() => ({
  title: '',
  imageUrl: '',
  linkUrl: '',
  sortNo: 0,
  enabled: true,
  description: ''
}))
const { visible, isEdit, submitting, form } = dlg
const formRef = ref()

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 100, message: '不超过 100 个字符', trigger: 'blur' }
  ],
  linkUrl: [{ max: 255, message: '不超过 255 个字符', trigger: 'blur' }]
}

const onSelectionChange = (v) => (selection.value = v)

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await bannerApi.update(form.value)
      ElMessage.success('轮播已更新')
    } else {
      await bannerApi.save(form.value)
      ElMessage.success('轮播已新增')
    }
    visible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

function onAdd() {
  dlg.open(null)
  formRef.value?.clearValidate()
}

function onEdit(row) {
  dlg.open(row)
  formRef.value?.clearValidate()
}

/** 直接切换启用状态：停用后前台首页立刻不再展示这一条 */
async function onToggleEnabled(row, val) {
  await bannerApi.update({ id: row.id, enabled: val })
  ElMessage.success(val ? '已启用' : '已停用')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 查询 -->
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="标题">
          <el-input
            v-model="query.title"
            placeholder="支持模糊匹配"
            clearable
            style="width: 200px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.enabled" clearable placeholder="全部" style="width: 120px">
            <el-option label="已启用" :value="true" />
            <el-option label="已停用" :value="false" />
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

    <!-- 表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <span class="table-toolbar__title">首页轮播</span>
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
            <el-icon><Plus /></el-icon> 新增轮播
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="图片" width="150" align="center">
          <template #default="{ row }">
            <img v-if="row.imageUrl" class="cell-img" :src="row.imageUrl" @error="onImgError" />
            <span v-else class="text-muted">未配置</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="170" show-overflow-tooltip />
        <el-table-column label="跳转地址" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.linkUrl">{{ row.linkUrl }}</span>
            <span v-else class="text-muted">不跳转</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="80" align="center" />
        <el-table-column label="启用" width="90" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="(v) => onToggleEnabled(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">{{ datetime(row.updatedTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="还没有配置轮播，前台会显示内置的兜底样式" :image-size="80" />
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

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="visible"
      :title="isEdit ? '编辑轮播' : '新增轮播'"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="显示在轮播上的文字" />
        </el-form-item>
        <el-form-item label="图片">
          <ImageUpload
            v-model="form.imageUrl"
            type="banner"
            tip="建议宽幅比例约 1200×420；不传也可以，前台会用渐变兜底"
          />
        </el-form-item>
        <el-form-item label="跳转地址" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="站内路由如 /goods、/seckill，或 https:// 外链" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortNo" :min="0" :max="9999" />
          <span class="text-muted" style="margin-left: 10px">数字小的排在前面</span>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
          <span class="text-muted" style="margin-left: 10px">停用后前台首页不展示</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
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
