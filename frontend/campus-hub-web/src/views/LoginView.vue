<template><div class="auth-page"><div class="auth-card"><div class="brand center"><div class="brand-mark">C</div><div><strong>CampusHub</strong><small>校园生活服务平台</small></div></div><h1>欢迎回来</h1><p>登录后探索更多校园生活</p><el-form @submit.prevent="login"><el-form-item><el-input v-model="username" placeholder="手机号 / 用户名" size="large"/></el-form-item><el-form-item><el-input v-model="password" placeholder="密码" show-password size="large"/></el-form-item><el-button type="primary" size="large" round class="full" @click="login">登录</el-button></el-form><el-button text class="full ghost" @click="$router.push('/home')">先去逛逛</el-button></div></div></template>
<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCampusStore } from '../stores/campus'
const router = useRouter(); const route = useRoute(); const campus = useCampusStore(); const username = ref(''); const password = ref('')
function login() { if (!username.value || !password.value) return ElMessage.warning('请输入账号和密码'); campus.setUser({ nickname: username.value }); ElMessage.success('已登录 CampusHub'); const redirect = route.query.redirect; router.push(typeof redirect === 'string' && redirect ? redirect : '/home') }
</script>
