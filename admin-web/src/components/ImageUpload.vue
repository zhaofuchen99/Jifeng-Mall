<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/upload'
import { onImgError } from '@/utils/format'

const model = defineModel({ type: String, default: '' })

const props = defineProps({
  /** 上传子目录：brand / good / member / common */
  type: { type: String, default: 'common' },
  /** 提示文案 */
  tip: { type: String, default: '' }
})

const uploading = ref(false)
const inputRef = ref()

function pick() {
  inputRef.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  // 清空 input，否则连续选同一个文件不会触发 change
  e.target.value = ''
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 10MB')
    return
  }
  doUpload(file)
}

async function doUpload(file) {
  uploading.value = true
  try {
    // 后端返回的是完整 URL；注意上传后 5 分钟内要存进业务数据，
    // 否则会被定时任务当成"没被引用"删掉
    model.value = await uploadFile(file, props.type)
    ElMessage.success('上传成功')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="upload-wrap">
    <div v-loading="uploading" class="uploader" @click="pick">
      <img v-if="model" :src="model" @error="onImgError" />
      <div v-else class="uploader__empty">
        <el-icon :size="22"><Plus /></el-icon>
        <span>上传图片</span>
      </div>
    </div>

    <div v-if="model" class="upload-actions">
      <el-button link type="primary" size="small" @click="pick">更换</el-button>
      <el-button link type="danger" size="small" @click="model = ''">移除</el-button>
    </div>
    <div v-if="tip" class="upload-tip">{{ tip }}</div>

    <input
      ref="inputRef"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
      hidden
      @change="onFileChange"
    />
  </div>
</template>

<style scoped>
.upload-wrap {
  display: inline-block;
}

.upload-actions {
  display: flex;
  gap: 8px;
}

.upload-tip {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 4px;
  max-width: 220px;
  line-height: 1.5;
}
</style>
