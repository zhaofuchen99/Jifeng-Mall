<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import goodApi from '@/api/good'
import brandApi from '@/api/brand'
import categoryApi from '@/api/category'
import { useCrud, useDialog } from '@/composables/useCrud'
import { datetime, money, onImgError, splitUrls } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'

/**
 * 商品管理。
 *
 * 注意几个字段语义：
 * - `isTakeDown` 上下架；`isDel` 是逻辑删除标记，**不在表单里开放**（后端 update 是条件更新，不传就不动）
 * - `detailPics` 是**逗号分隔**的多图 URL 字符串，不是数组
 * - `detail` 是富文本 HTML 字符串
 */
const {
  rows, total, loading, selection, query, pageNo, size,
  load, search, reset, remove, onPageChange, onSizeChange
} = useCrud({
  api: goodApi,
  label: '商品',
  pageSize: 10,
  query: { name: '', categoryId: null, brandId: null, isTakeDown: null, isHot: null }
})

const dlg = useDialog(() => ({
  spuNo: '',
  name: '',
  alias: '',
  summary: '',
  categoryId: null,
  brandId: null,
  markPrice: 0,
  price: 0,
  qty: 0,
  pic: '',
  pic2: '',
  detailPicsList: [],
  detail: '',
  isTakeDown: false,
  isHot: false,
  isSeckill: false,
  description: ''
}))
const { visible, isEdit, submitting, form } = dlg
const formRef = ref()

const brands = ref([])
const categories = ref([])
/** 级联选择器要求子节点字段名可配，分类实体的子节点就叫 children */
const treeProps = { value: 'id', label: 'name', children: 'children', checkStrictly: true }

const rules = {
  spuNo: [{ required: true, message: '请输入商品编号', trigger: 'blur' }],
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { max: 100, message: '不超过 100 个字符', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  brandId: [{ required: true, message: '请选择品牌', trigger: 'change' }],
  price: [
    { required: true, message: '请输入售价', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (Number(v) > 0 ? cb() : cb(new Error('售价必须大于 0'))),
      trigger: 'blur'
    }
  ],
  qty: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const onSelectionChange = (v) => (selection.value = v)

const onTakeDownSwitch = async (row, val) => {
  await goodApi.update({ id: row.id, isTakeDown: val })
  ElMessage.success(val ? '商品已下架' : '商品已上架')
}

const onHotSwitch = async (row, val) => {
  await goodApi.update({ id: row.id, isHot: val })
  ElMessage.success(val ? '已设为热销' : '已取消热销')
}

async function loadOptions() {
  const [brandPage, catTree] = await Promise.all([
    brandApi.list({ pageNo: 1, pageSize: 0 }),
    categoryApi.tree()
  ])
  brands.value = brandPage?.list || []
  categories.value = catTree || []
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    // detailPics 后端存的是逗号分隔字符串，表单里用数组更好操作，提交时拼回去
    const payload = { ...form.value }
    payload.detailPics = (payload.detailPicsList || []).filter(Boolean).join(',')
    delete payload.detailPicsList
    delete payload.brand
    delete payload.category

    if (!payload.pic) payload.pic = null
    if (!payload.pic2) payload.pic2 = null

    if (isEdit.value) {
      await goodApi.update(payload)
      ElMessage.success('商品已更新')
    } else {
      await goodApi.save(payload)
      ElMessage.success('商品已新增')
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
  // 把逗号分隔的多图字符串还原成数组给表单用
  form.value.detailPicsList = splitUrls(row.detailPics)
  formRef.value?.clearValidate()
}

function addDetailPic() {
  form.value.detailPicsList.push('')
}

function removeDetailPic(i) {
  form.value.detailPicsList.splice(i, 1)
}

const categoryName = computed(() => {
  // 商品列表带 full=true 才有 category 对象；没带就只在编辑弹窗里靠 id 反查
  const map = {}
  const walk = (nodes) => {
    nodes.forEach((n) => {
      map[n.id] = n.name
      if (n.children?.length) walk(n.children)
    })
  }
  walk(categories.value)
  return map
})

onMounted(async () => {
  await loadOptions()
  await load()
})
</script>

<template>
  <div class="page">
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="商品名称">
          <el-input
            v-model="query.name"
            placeholder="支持模糊匹配"
            clearable
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="query.categoryId"
            :data="categories"
            :props="treeProps"
            node-key="id"
            check-strictly
            clearable
            placeholder="全部分类"
            style="width: 170px"
          />
        </el-form-item>
        <el-form-item label="品牌">
          <el-select v-model="query.brandId" clearable placeholder="全部品牌" style="width: 150px">
            <el-option v-for="b in brands" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.isTakeDown" clearable placeholder="全部" style="width: 120px">
            <el-option label="已上架" :value="false" />
            <el-option label="已下架" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item label="热销">
          <el-select v-model="query.isHot" clearable placeholder="全部" style="width: 110px">
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
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
        <span class="table-toolbar__title">商品列表</span>
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
            <el-icon><Plus /></el-icon> 新增商品
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column label="主图" width="74" align="center">
          <template #default="{ row }">
            <img class="cell-img" :src="row.pic || '/img-placeholder.svg'" @error="onImgError" />
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="good-name">{{ row.name }}</div>
            <div class="text-muted good-sub">{{ row.spuNo }} · {{ row.alias || '无别名' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="110">
          <template #default="{ row }">
            {{ row.category?.name || categoryName[row.categoryId] || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="品牌" width="100">
          <template #default="{ row }">{{ row.brand?.name || '—' }}</template>
        </el-table-column>
        <el-table-column label="价格" width="140" align="right">
          <template #default="{ row }">
            <div class="price">¥{{ money(row.price) }}</div>
            <div class="text-muted good-sub">原价 ¥{{ money(row.markPrice) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="qty" label="库存" width="80" align="center" />
        <el-table-column label="热销" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.isHot" size="small" @change="(v) => onHotSwitch(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="上架" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.isTakeDown" size="small" @change="(v) => onTakeDownSwitch(row, v)" />
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
          <el-empty description="没有符合条件的商品" :image-size="80" />
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
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="880px"
      top="6vh"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品编号" prop="spuNo">
              <el-input v-model="form.spuNo" placeholder="唯一 SPU 编号，如 SPU1001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="别名">
              <el-input v-model="form.alias" placeholder="用于搜索的别名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="完整商品名称" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categories"
                :props="treeProps"
                node-key="id"
                check-strictly
                placeholder="请选择分类"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌" prop="brandId">
              <el-select v-model="form.brandId" placeholder="请选择品牌" style="width: 100%">
                <el-option v-for="b in brands" :key="b.id" :label="b.name" :value="b.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="售价" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0"
                :precision="2"
                :step="100"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="标价">
              <el-input-number
                v-model="form.markPrice"
                :min="0"
                :precision="2"
                :step="100"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存" prop="qty">
              <el-input-number v-model="form.qty" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="摘要">
          <el-input v-model="form.summary" maxlength="100" show-word-limit placeholder="一句话卖点" />
        </el-form-item>

        <el-form-item label="主图">
          <div class="img-row">
            <div>
              <ImageUpload v-model="form.pic" type="good" tip="列表与详情页的主图" />
            </div>
            <div>
              <ImageUpload v-model="form.pic2" type="good" tip="详情页第二张图（可选）" />
            </div>
          </div>
        </el-form-item>

        <el-form-item label="详情图">
          <div class="detail-pics">
            <div v-for="(_, i) in form.detailPicsList" :key="i" class="detail-pics__item">
              <ImageUpload v-model="form.detailPicsList[i]" type="good" />
              <el-button link type="danger" size="small" @click="removeDetailPic(i)">移除</el-button>
            </div>
            <div class="uploader detail-pics__add" @click="addDetailPic">
              <div class="uploader__empty">
                <el-icon :size="20"><Plus /></el-icon>
                <span>加一张</span>
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="详情">
          <el-input
            v-model="form.detail"
            type="textarea"
            :rows="4"
            placeholder="支持 HTML（详情页会原样渲染）"
          />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="标记">
          <el-checkbox v-model="form.isHot">热销</el-checkbox>
          <el-checkbox v-model="form.isSeckill">参与秒杀</el-checkbox>
          <el-checkbox v-model="form.isTakeDown">下架</el-checkbox>
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
.good-name {
  font-weight: 500;
  line-height: 1.3;
}

.good-sub {
  font-size: 12px;
  margin-top: 2px;
}

.img-row {
  display: flex;
  gap: 20px;
}

.detail-pics {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.detail-pics__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.detail-pics__add {
  height: 104px;
}
</style>
