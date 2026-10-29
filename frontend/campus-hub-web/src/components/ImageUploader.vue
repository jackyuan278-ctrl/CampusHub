<template>
  <div class="image-uploader">
    <el-upload
      class="image-uploader-box"
      :class="{ 'is-round': round }"
      :show-file-list="false"
      :disabled="uploading"
      :before-upload="beforeUpload"
      :http-request="doUpload"
      accept="image/jpeg,image/png,image/webp,image/gif"
    >
      <div v-if="modelValue" class="uploader-preview">
        <img :src="modelValue" alt="已上传图片" />
        <span class="uploader-mask">{{ uploading ? '上传中…' : '点击换一张' }}</span>
      </div>
      <div v-else class="uploader-placeholder">
        <el-icon :size="20"><Plus /></el-icon>
        <span>{{ uploading ? '上传中…' : tip }}</span>
        <small>jpg / png / webp，≤ {{ maxSize }}MB</small>
      </div>
    </el-upload>
    <el-button v-if="modelValue && !uploading" link type="danger" size="small" @click="emit('update:modelValue', '')">
      移除图片
    </el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 通用图片上传：只负责「选文件 → 校验 → 调上传接口 → 回填 URL」。
 * 具体传到哪个服务由父组件通过 upload 属性决定（商品图 / 活动封面 / 头像各自一个接口）。
 */
const props = defineProps({
  modelValue: { type: String, default: '' },
  /** 上传函数，入参 File，返回 Promise<string>（图片地址） */
  upload: { type: Function, required: true },
  tip: { type: String, default: '上传图片' },
  maxSize: { type: Number, default: 5 },
  round: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const ALLOWED = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const uploading = ref(false)

function beforeUpload(file) {
  if (!ALLOWED.includes(file.type)) {
    ElMessage.error('只支持 jpg / png / webp / gif 格式的图片')
    return false
  }
  if (file.size > props.maxSize * 1024 * 1024) {
    ElMessage.error(`图片不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

async function doUpload(options) {
  uploading.value = true
  try {
    const url = await props.upload(options.file)
    emit('update:modelValue', url)
    ElMessage.success('图片上传成功')
  } catch {
    // 请求层已经弹过错误提示（含「上传未启用」这类后端文案），这里不重复弹
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.image-uploader {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.image-uploader-box :deep(.el-upload) {
  display: block;
}

.uploader-preview,
.uploader-placeholder {
  position: relative;
  width: 148px;
  height: 104px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px dashed rgba(120, 130, 155, 0.45);
  background: rgba(120, 130, 155, 0.06);
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.image-uploader-box.is-round .uploader-preview,
.image-uploader-box.is-round .uploader-placeholder {
  width: 92px;
  height: 92px;
  border-radius: 50%;
}

.uploader-preview:hover {
  border-color: var(--brand, #4f7cff);
}

.uploader-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.uploader-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #fff;
  background: rgba(20, 24, 38, 0.45);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.uploader-preview:hover .uploader-mask {
  opacity: 1;
}

.uploader-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: rgba(90, 100, 125, 0.9);
  font-size: 12px;
  text-align: center;
  padding: 0 8px;
}

.uploader-placeholder small {
  font-size: 11px;
  opacity: 0.7;
}
</style>
