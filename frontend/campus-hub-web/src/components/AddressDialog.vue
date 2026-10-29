<template>
  <el-dialog :model-value="modelValue" :title="address ? '编辑收货地址' : '新增收货地址'" width="520px" @update:model-value="$emit('update:modelValue', $event)">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="dialog-form">
      <div class="form-row">
        <el-form-item label="收货人" prop="receiver">
          <el-input v-model="form.receiver" placeholder="姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" />
        </el-form-item>
      </div>
      <div class="form-row three">
        <el-form-item label="省份">
          <el-input v-model="form.province" placeholder="江西省" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="form.city" placeholder="南昌市" />
        </el-form-item>
        <el-form-item label="区县">
          <el-input v-model="form.district" placeholder="红谷滩区" />
        </el-form-item>
      </div>
      <el-form-item label="详细地址" prop="detail">
        <el-input v-model="form.detail" type="textarea" :rows="2" placeholder="楼栋、门牌号，方便同学找到你" maxlength="255" />
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="form.isDefault">设为默认地址</el-checkbox>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存地址</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'

const props = defineProps({
  modelValue: Boolean,
  address: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref(null)
const saving = ref(false)
const form = reactive({
  receiver: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
})

const rules = {
  receiver: [{ required: true, message: '请填写收货人', trigger: 'blur' }],
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  detail: [{ required: true, message: '请填写详细地址', trigger: 'blur' }]
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    const source = props.address || {}
    Object.assign(form, {
      receiver: source.receiver || '',
      phone: source.phone || '',
      province: source.province || '',
      city: source.city || '',
      district: source.district || '',
      detail: source.detail || '',
      isDefault: Boolean(source.isDefault)
    })
    formRef.value?.clearValidate()
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form, isDefault: form.isDefault ? 1 : 0 }
    if (props.address?.id) await userApi.updateAddress(props.address.id, payload)
    else await userApi.createAddress(payload)
    ElMessage.success('地址已保存')
    emit('saved')
    emit('update:modelValue', false)
  } catch {
    // 统一错误提示
  } finally {
    saving.value = false
  }
}
</script>
