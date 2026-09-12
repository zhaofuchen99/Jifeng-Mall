<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { seckillApi, seckillGoodApi } from '@/api/seckill'
import goodApi from '@/api/good'
import { useCrud, useDialog } from '@/composables/useCrud'
import { datetime, money, onImgError } from '@/utils/format'
import { seckillStatus, SECKILL_STATUS_TAG } from '@/utils/dict'

/**
 * 秒杀商品：把某个商品挂到某场活动下，并给出秒杀价、参与库存和限购数。
 *
 * 库存语义：`stock` 是分配给本场秒杀的库存（不是商品自身库存），
 * `sold` 是已成交数，**剩余 = stock - sold**。预热任务就是按这个差值
 * 灌到 Redis 的 seckill:stock:{id} 里的。
 */
const {
  rows, total, loading, selection, query, pageNo, size,
  load, search, reset, remove, onPageChange, onSizeChange
} = useCrud({
  api: seckillGoodApi,
  label: '秒杀商品',
  pageSize: 10,
  query: { seckillId: null, goodId: null }
})

const dlg = useDialog(() => ({
  seckillId: null,
  goodId: null,
  seckillPrice: 0,
  stock: 0,
  limitPerUser: 1,
  description: ''
}))
const { visible, isEdit, submitting, form } = dlg
const formRef = ref()

const activities = ref([])
const goods = ref([])

const rules = {
  seckillId: [{ required: true, message: '请选择秒杀活动', trigger: 'change' }],
  goodId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  seckillPrice: [
    { required: true, message: '请输入秒杀价', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (Number(v) > 0 ? cb() : cb(new Error('秒杀价必须大于 0'))),
      trigger: 'blur'
    }
  ],
  stock: [
    { required: true, message: '请输入秒杀库存', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (Number(v) > 0 ? cb() : cb(new Error('库存必须大于 0'))),
      trigger: 'blur'
    }
  ]
}

const onSelectionChange = (v) => (selection.value = v)

const activityName = (id) => activities.value.find((a) => a.id === id)?.name || `#${id}`
const goodOf = (id) => goods.value.find((g) => g.id === id)

const remainOf = (row) => Math.max(0, (row.stock ?? 0) - (row.sold ?? 0))

async function loadOptions() {
  const [actPage, goodPage] = await Promise.all([
    seckillApi.list({ pageNo: 1, pageSize: 0 }),
    goodApi.list({ pageNo: 1, pageSize: 0 })
  ])
  activities.value = actPage?.list || []
  goods.value = (goodPage?.list || []).filter((g) => g.isDel !== true)
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  // 同一场活动下同一个商品只能挂一次，先在前端拦一道，省得白白报错
  const dup = rows.value.find(
    (r) => r.seckillId === form.value.seckillId && r.goodId === form.value.goodId && r.id !== form.value.id
  )
  if (dup) {
    ElMessage.warning('该活动下已经挂过这件商品了')
    return
  }

  submitting.value = true
  try {
    const payload = { ...form.value }
    // sold 由系统累加，编辑时不要回写
    delete payload.sold

    if (isEdit.value) {
      await seckillGoodApi.update(payload)
      ElMessage.success('秒杀商品已更新')
    } else {
      await seckillGoodApi.save(payload)
      ElMessage.success('秒杀商品已新增')
    }
    visible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

function onAdd() {
  dlg.open(null)
  // 默认挂到第一场启用中的活动上
  const running = activities.value.find((a) => seckillStatus(a) === '进行中')
  form.value.seckillId = running?.id ?? activities.value[0]?.id ?? null
  formRef.value?.clearValidate()
}

function onEdit(row) {
  dlg.open(row)
  formRef.value?.clearValidate()
}

onMounted(async () => {
  await loadOptions()
  await load()
})
</script>

<template>
  <div class="page">
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="秒杀活动">
          <el-select
            v-model="query.seckillId"
            clearable
            placeholder="全部活动"
            style="width: 200px"
          >
            <el-option v-for="a in activities" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品">
          <el-select v-model="query.goodId" clearable placeholder="全部商品" style="width: 220px">
            <el-option v-for="g in goods" :key="g.id" :label="g.name" :value="g.id" />
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
        <span class="table-toolbar__title">秒杀商品</span>
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
            <el-icon><Plus /></el-icon> 新增秒杀商品
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="flex-center gap-8">
              <img
                class="cell-img cell-img--sm"
                :src="goodOf(row.goodId)?.pic || '/img-placeholder.svg'"
                @error="onImgError"
              />
              <div>
                <div>{{ goodOf(row.goodId)?.name || `商品 #${row.goodId}` }}</div>
                <div class="text-muted sub">原价 ¥{{ money(goodOf(row.goodId)?.price) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属活动" min-width="150">
          <template #default="{ row }">
            <div>{{ activityName(row.seckillId) }}</div>
            <el-tag
              v-if="activities.find((a) => a.id === row.seckillId)"
              :type="SECKILL_STATUS_TAG[seckillStatus(activities.find((a) => a.id === row.seckillId))]"
              size="small"
            >
              {{ seckillStatus(activities.find((a) => a.id === row.seckillId)) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="秒杀价" width="110" align="right">
          <template #default="{ row }">
            <span class="price">¥{{ money(row.seckillPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="分配库存" width="90" align="center" />
        <el-table-column prop="sold" label="已售" width="76" align="center" />
        <el-table-column label="剩余" width="80" align="center">
          <template #default="{ row }">
            <b :class="{ 'text-danger': remainOf(row) === 0 }">{{ remainOf(row) }}</b>
          </template>
        </el-table-column>
        <el-table-column prop="limitPerUser" label="限购" width="72" align="center" />
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
          <el-empty description="暂无秒杀商品" :image-size="80" />
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
      :title="isEdit ? '编辑秒杀商品' : '新增秒杀商品'"
      width="580px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="秒杀活动" prop="seckillId">
          <el-select v-model="form.seckillId" placeholder="请选择活动" style="width: 100%">
            <el-option v-for="a in activities" :key="a.id" :label="a.name" :value="a.id">
              <span>{{ a.name }}</span>
              <span class="text-muted opt-tag">{{ seckillStatus(a) }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="商品" prop="goodId">
          <el-select
            v-model="form.goodId"
            placeholder="请选择商品"
            filterable
            style="width: 100%"
          >
            <el-option v-for="g in goods" :key="g.id" :label="g.name" :value="g.id">
              <span>{{ g.name }}</span>
              <span class="text-muted opt-tag">¥{{ money(g.price) }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="秒杀价" prop="seckillPrice">
              <el-input-number
                v-model="form.seckillPrice"
                :min="0"
                :precision="2"
                :step="100"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分配库存" prop="stock">
              <el-input-number v-model="form.stock" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="每人限购">
          <el-input-number v-model="form.limitPerUser" :min="1" :max="99" />
          <span class="text-muted ml-8">件</span>
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

<style scoped>
.sub {
  font-size: 12px;
}

.opt-tag {
  float: right;
  font-size: 12px;
}

.ml-8 {
  margin-left: 8px;
}
</style>
