<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { addAddress, updateAddress } from '@/api/address'
import { useRegionStore } from '@/stores/region'

const props = defineProps({
  /** 传入即为编辑，null 为新增 */
  address: { type: Object, default: null }
})

const visible = defineModel({ type: Boolean, default: false })
const emit = defineEmits(['saved'])

const regionStore = useRegionStore()
const formRef = ref()
const saving = ref(false)

const form = reactive({
  id: null,
  memberAccount: '',
  receiver: '',
  phone: '',
  /** el-cascader 的值是数组，最后一项才是区划 id */
  regionPath: [],
  addrDetail: '',
  isDefault: false
})

const rules = {
  receiver: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  regionPath: [{ required: true, message: '请选择所在地区', trigger: 'change' }],
  addrDetail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

function reset(from) {
  form.id = from?.id ?? null
  form.memberAccount = from?.memberAccount ?? ''
  form.receiver = from?.receiver ?? ''
  form.phone = from?.phone ?? ''
  form.addrDetail = from?.addrDetail ?? ''
  form.isDefault = from?.isDefault === true
  // 后端只存区划 id，回显时把祖先链补出来，否则级联选择器是空的
  form.regionPath = from?.addrId ? buildPath(from.addrId) : []
}

/** 用 nameMap 的 "省 / 市 / 区" 反推出级联的路径数组 */
function buildPath(addrId) {
  const full = regionStore.nameMap[addrId]
  if (!full) return [addrId]
  const names = full.split(' / ')
  const path = []
  let candidates = regionStore.tree
  for (const name of names) {
    const node = candidates.find((n) => n.label === name)
    if (!node) break
    path.push(node.value)
    candidates = node.children || []
  }
  return path.length ? path : [addrId]
}

watch(visible, async (v) => {
  if (!v) return
  await regionStore.ensureLoaded()
  reset(props.address)
  formRef.value?.clearValidate()
})

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  saving.value = true
  try {
    const payload = {
      id: form.id,
      memberAccount: form.memberAccount,
      receiver: form.receiver,
      phone: form.phone,
      addrId: form.regionPath[form.regionPath.length - 1],
      addrDetail: form.addrDetail,
      isDefault: form.isDefault
    }
    if (form.id) {
      await updateAddress(payload)
    } else {
      await addAddress(payload)
    }
    ElMessage.success(form.id ? '地址已更新' : '地址已新增')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" :title="form.id ? '编辑收货地址' : '新增收货地址'" width="560px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="收货人" prop="receiver">
        <el-input v-model="form.receiver" placeholder="请输入收货人姓名" />
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
      </el-form-item>

      <el-form-item label="所在地区" prop="regionPath">
        <el-cascader
          v-model="form.regionPath"
          :options="regionStore.tree"
          :loading="regionStore.loading"
          placeholder="请选择省 / 市 / 区"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="详细地址" prop="addrDetail">
        <el-input
          v-model="form.addrDetail"
          type="textarea"
          :rows="2"
          placeholder="街道、门牌号等"
        />
      </el-form-item>

      <el-form-item>
        <el-checkbox v-model="form.isDefault">设为默认收货地址</el-checkbox>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>
