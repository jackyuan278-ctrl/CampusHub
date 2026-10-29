<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand center">
        <div class="brand-mark">C</div>
        <div>
          <strong>CampusHub</strong>
          <small>校园生活服务平台</small>
        </div>
      </div>

      <div class="auth-tabs">
        <button :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
        <button :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
      </div>

      <h1>{{ mode === 'login' ? '欢迎回来' : '加入 CampusHub' }}</h1>
      <p>{{ mode === 'login' ? '登录后继续你的校园生活' : '注册后即可发帖、报名与下单' }}</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" size="large" placeholder="用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="phone">
          <el-input v-model="form.phone" size="large" placeholder="手机号" maxlength="11" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" size="large" show-password placeholder="密码（6-20 位）" autocomplete="current-password" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" size="large" show-password placeholder="确认密码" />
        </el-form-item>
        <el-button type="primary" size="large" round class="full" :loading="submitting" @click="submit">
          {{ mode === 'login' ? '登录' : '注册并登录' }}
        </el-button>
      </el-form>

      <el-button text class="full ghost" @click="router.push('/home')">先去逛逛</el-button>
      <p class="auth-tip">演示账号可在后端 tb_user 中注册，注册接口已对接 /users/register。</p>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const mode = ref('login')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ username: '', phone: '', password: '', confirmPassword: '' })

const rules = computed(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: mode.value === 'register'
    ? [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
    : [],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: mode.value === 'register'
    ? [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        {
          validator: (_rule, value, callback) => {
            if (value !== form.password) callback(new Error('两次输入的密码不一致'))
            else callback()
          },
          trigger: 'blur'
        }
      ]
    : []
}))

function switchMode(next) {
  mode.value = next
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (mode.value === 'login') {
      await auth.login({ username: form.username, password: form.password })
      ElMessage.success(`欢迎回来，${auth.displayName}`)
    } else {
      await auth.register({ username: form.username, phone: form.phone, password: form.password })
      ElMessage.success('注册成功，已自动登录')
    }
    await cart.load()
    const redirect = route.query.redirect
    router.push(typeof redirect === 'string' && redirect ? redirect : '/home')
  } catch {
    // 统一错误提示
  } finally {
    submitting.value = false
  }
}
</script>
