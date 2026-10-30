<template>
  <div class="page-container">
    <section class="page-heading">
      <div>
        <p class="eyebrow">MY CAMPUSHUB</p>
        <h1>个人中心</h1>
        <p>报名、签到、订单与收货地址，都在这里统一管理。</p>
      </div>
      <el-button round @click="router.push('/orders')">我的订单</el-button>
    </section>

    <div class="profile-layout">
      <aside class="profile-aside">
        <div class="profile-card">
          <button
            class="profile-avatar"
            type="button"
            :class="{ 'is-uploading': uploadingAvatar }"
            :title="uploadingAvatar ? '正在上传头像' : '点击更换头像'"
            @click="pickAvatar"
          >
            <el-avatar :size="72" :src="auth.user?.avatar || defaultAvatar" />
            <span class="profile-avatar-mask">
              <el-icon :size="18"><Camera /></el-icon>
              <small>{{ uploadingAvatar ? '上传中…' : '更换头像' }}</small>
            </span>
          </button>
          <input
            ref="avatarInput"
            class="avatar-input"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            @change="onAvatarChange"
          />
          <h3>{{ auth.displayName }}</h3>
          <p>@{{ auth.user?.username || 'campus' }}</p>
          <div class="profile-balance">
            <small>账户余额</small>
            <strong>¥{{ money(auth.balance) }}</strong>
          </div>
          <div class="profile-stats">
            <div><b>{{ signups.length }}</b><small>报名</small></div>
            <div><b>{{ checkin.streak || 0 }}</b><small>连续签到</small></div>
            <div><b>{{ follows.length }}</b><small>关注</small></div>
          </div>
        </div>

        <nav class="profile-nav">
          <button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="switchTab(tab.key)">
            <el-icon><component :is="tab.icon" /></el-icon>{{ tab.label }}
          </button>
        </nav>
      </aside>

      <div class="profile-content">
        <!-- 资料 -->
        <div v-if="activeTab === 'profile'" class="panel">
          <div class="panel-head">
            <h2 class="panel-title">基本资料</h2>
            <span class="panel-hint">昵称与头像会展示在社区与交易信息中</span>
          </div>
          <el-form :model="profileForm" label-position="top" class="profile-form">
            <div class="form-row">
              <el-form-item label="昵称">
                <el-input v-model="profileForm.nickname" maxlength="50" placeholder="给自己取个校园昵称" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="profileForm.phone" maxlength="11" placeholder="用于联系与通知" />
              </el-form-item>
            </div>
            <div class="form-actions">
              <el-button type="primary" round :loading="savingProfile" @click="saveProfile">保存资料</el-button>
            </div>
          </el-form>

          <div class="panel-divider"></div>

          <h2 class="panel-title">修改密码</h2>
          <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-position="top" class="profile-form">
            <div class="form-row">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" show-password placeholder="当前密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" show-password placeholder="6-20 位" />
              </el-form-item>
            </div>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" show-password placeholder="再输入一次新密码" />
            </el-form-item>
            <div class="form-actions">
              <el-button round :loading="savingPassword" @click="savePassword">更新密码</el-button>
            </div>
          </el-form>
        </div>

        <!-- 报名 -->
        <div v-else-if="activeTab === 'signups'" class="panel">
          <div class="panel-head">
            <h2 class="panel-title">我的报名</h2>
            <span class="panel-hint">报名与候补记录（/signups/mine）</span>
          </div>

          <StatePanel v-if="loadingSignups" type="loading" />
          <StatePanel v-else-if="!signups.length" type="empty" title="还没有报名任何活动" description="去活动页看看，总有一场适合你。" action-text="浏览活动" @action="router.push('/activities')" />
          <ul v-else class="signup-list">
            <li v-for="item in signups" :key="item.id">
              <div class="signup-date">{{ formatDate(item.startTime, true) || '时间待定' }}</div>
              <div class="signup-info">
                <b @click="router.push(`/activities/${item.activityId}`)">{{ item.activityTitle }}</b>
                <small><el-icon><Location /></el-icon>{{ item.location || '地点待通知' }}</small>
              </div>
              <StatusTag :text="statusMeta(SIGNUP_STATUS, item.status).text" :tone="statusMeta(SIGNUP_STATUS, item.status).tone" />
              <span v-if="item.status === 2 && item.waitPosition" class="wait-position">候补第 {{ item.waitPosition }} 位</span>
              <el-button v-if="item.status !== 3" text type="danger" size="small" @click="cancelSignup(item)">取消</el-button>
            </li>
          </ul>
        </div>

        <!-- 签到 -->
        <div v-else-if="activeTab === 'checkin'" class="panel">
          <div class="panel-head">
            <h2 class="panel-title">签到记录</h2>
            <span class="panel-hint">Redis Bitmap 按日记录</span>
          </div>
          <CheckinCard :checkin="checkin" :loading="checking" @checkin="doCheckin" />

          <div class="panel-divider"></div>
          <h3 class="panel-subtitle">本月已签到</h3>
          <div v-if="checkin.checkinDates?.length" class="checkin-history">
            <span v-for="date in checkin.checkinDates" :key="date">{{ date.slice(5) }}</span>
          </div>
          <p v-else class="panel-hint">本月还没有签到记录，今天开始也不晚。</p>
        </div>

        <!-- 地址 -->
        <div v-else-if="activeTab === 'addresses'" class="panel">
          <div class="panel-head">
            <h2 class="panel-title">收货地址</h2>
            <el-button text type="primary" @click="openAddress(null)">
              <el-icon><Plus /></el-icon>新增地址
            </el-button>
          </div>

          <StatePanel v-if="!addresses.length" type="empty" title="还没有收货地址" description="添加地址后，下单时可以直接选择。" action-text="新增地址" @action="openAddress(null)" />
          <ul v-else class="address-list">
            <li v-for="address in addresses" :key="address.id" class="address-row">
              <div>
                <b>{{ address.receiver }}</b>
                <small>{{ address.phone }}</small>
                <span v-if="address.isDefault" class="soft-tag">默认</span>
              </div>
              <p>{{ fullAddress(address) }}</p>
              <div class="address-actions">
                <el-button v-if="!address.isDefault" text size="small" @click="setDefault(address)">设为默认</el-button>
                <el-button text size="small" @click="openAddress(address)">编辑</el-button>
                <el-button text size="small" type="danger" @click="removeAddress(address)">删除</el-button>
              </div>
            </li>
          </ul>
        </div>

        <!-- 关注 -->
        <div v-else class="panel">
          <div class="panel-head">
            <h2 class="panel-title">我的关注</h2>
            <span class="panel-hint">关注社区里持续输出经验的同学</span>
          </div>

          <StatePanel v-if="!follows.length" type="empty" title="还没有关注任何人" description="在问题详情页可以关注回答者。" action-text="去社区" @action="router.push('/community')" />
          <ul v-else class="follow-list">
            <li v-for="user in follows" :key="user.id">
              <el-avatar :size="38" :src="user.avatarUrl || defaultAvatar" />
              <div>
                <b>{{ user.nickname || user.username || '校园同学' }}</b>
                <small>{{ user.nickname || user.username ? '校园同学' : '资料待同步' }}</small>
              </div>
              <el-button size="small" round @click="unfollow(user)">取消关注</el-button>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <AddressDialog v-model="addressVisible" :address="editingAddress" @saved="loadAddresses" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '@/api/user'
import { checkinApi, signupApi } from '@/api/activity'
import { followApi } from '@/api/community'
import { useAuthStore } from '@/stores/auth'
import { SIGNUP_STATUS, formatDate, money, statusMeta } from '@/utils/format'
import StatePanel from '@/components/StatePanel.vue'
import StatusTag from '@/components/StatusTag.vue'
import CheckinCard from '@/components/CheckinCard.vue'
import AddressDialog from '@/components/AddressDialog.vue'
import defaultAvatar from '@/assets/avatar-default.svg'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const tabs = [
  { key: 'profile', label: '基本资料', icon: 'User' },
  { key: 'signups', label: '我的报名', icon: 'Calendar' },
  { key: 'checkin', label: '签到记录', icon: 'AlarmClock' },
  { key: 'addresses', label: '收货地址', icon: 'Location' },
  { key: 'follows', label: '我的关注', icon: 'Star' }
]

const activeTab = ref('profile')
const signups = ref([])
const addresses = ref([])
const follows = ref([])
const checkin = ref({ signedToday: false, streak: 0, checkinDates: [] })
const loadingSignups = ref(false)
const checking = ref(false)
const savingProfile = ref(false)
const savingPassword = ref(false)
const addressVisible = ref(false)
const editingAddress = ref(null)
const passwordRef = ref(null)

/** 头像单独走上传接口、上传即写库，不参与「保存资料」，所以不放进 profileForm */
const profileForm = reactive({ nickname: '', phone: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const tabsKeys = computed(() => tabs.map((tab) => tab.key))

function switchTab(key) {
  activeTab.value = key
  router.replace({ path: '/profile', query: key === 'profile' ? {} : { tab: key } })
}

function fullAddress(address) {
  return `${address.province || ''}${address.city || ''}${address.district || ''}${address.detail || ''}`
}

function syncProfileForm() {
  Object.assign(profileForm, {
    nickname: auth.user?.nickname || '',
    phone: auth.user?.phone || ''
  })
}

async function loadSignups() {
  loadingSignups.value = true
  try {
    const data = await signupApi.mine({ silent: true })
    signups.value = Array.isArray(data) ? data : []
  } catch {
    signups.value = []
  } finally {
    loadingSignups.value = false
  }
}

async function loadAddresses() {
  try {
    const data = await userApi.addresses({ silent: true })
    addresses.value = Array.isArray(data) ? data : []
  } catch {
    addresses.value = []
  }
}

async function loadFollows() {
  try {
    const data = await followApi.mine({ silent: true })
    follows.value = Array.isArray(data) ? data : []
  } catch {
    follows.value = []
  }
}

async function loadCheckin() {
  try {
    checkin.value = await checkinApi.mine({ silent: true })
  } catch {
    checkin.value = { signedToday: false, streak: 0, checkinDates: [] }
  }
}

const avatarInput = ref(null)
const uploadingAvatar = ref(false)
const AVATAR_MAX_SIZE = 5
const AVATAR_ALLOWED = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

/** 点头像本身触发文件选择，不再单独占一个表单项 */
function pickAvatar() {
  if (uploadingAvatar.value) return
  avatarInput.value?.click()
}

/** 头像上传接口自己会写库，上传完直接刷新登录态，右上角和本页头像一起变 */
async function onAvatarChange(event) {
  const file = event.target.files?.[0]
  // 清空 value，否则连续选同一张图不会再触发 change
  event.target.value = ''
  if (!file) return
  if (!AVATAR_ALLOWED.includes(file.type)) {
    return ElMessage.error('头像只支持 jpg / png / webp / gif')
  }
  if (file.size > AVATAR_MAX_SIZE * 1024 * 1024) {
    return ElMessage.error(`头像不能超过 ${AVATAR_MAX_SIZE}MB`)
  }
  uploadingAvatar.value = true
  try {
    const url = await userApi.uploadAvatar(file)
    // 只替换头像字段，不整体 loadMe：上面的 watch 会把还没保存的昵称、手机号一起冲掉
    if (url) auth.cacheUser({ ...auth.user, avatar: url })
    else await auth.loadMe()
    ElMessage.success('头像已更新')
  } catch {
    // 请求层已弹过提示（含「上传未启用」这类后端文案）
  } finally {
    uploadingAvatar.value = false
  }
}

async function saveProfile() {
  if (profileForm.phone && !/^1[3-9]\d{9}$/.test(profileForm.phone)) return ElMessage.warning('手机号格式不正确')
  savingProfile.value = true
  try {
    await userApi.updateMe({ ...profileForm })
    await auth.loadMe()
    ElMessage.success('资料已更新')
  } catch {
    // 统一错误提示
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  const valid = await passwordRef.value?.validate().catch(() => false)
  if (!valid) return
  savingPassword.value = true
  try {
    await userApi.changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    passwordRef.value?.clearValidate()
    ElMessage.success('密码已更新，下次登录请使用新密码')
  } catch {
    // 统一错误提示
  } finally {
    savingPassword.value = false
  }
}

async function cancelSignup(item) {
  try {
    await ElMessageBox.confirm(`确定取消「${item.activityTitle}」的报名吗？`, '取消报名', {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await signupApi.cancel(item.activityId)
    ElMessage.success('已取消')
    await loadSignups()
  } catch {
    // 统一错误提示
  }
}

async function doCheckin() {
  checking.value = true
  try {
    checkin.value = await checkinApi.checkin()
    ElMessage.success('签到成功，今天也要好好生活')
  } catch {
    // 统一错误提示
  } finally {
    checking.value = false
  }
}

function openAddress(address) {
  editingAddress.value = address
  addressVisible.value = true
}

async function setDefault(address) {
  try {
    await userApi.setDefaultAddress(address.id)
    ElMessage.success('已设为默认地址')
    await loadAddresses()
  } catch {
    // 统一错误提示
  }
}

async function removeAddress(address) {
  try {
    await ElMessageBox.confirm('删除后下单时无法选择该地址，确定删除吗？', '删除地址', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await userApi.removeAddress(address.id)
    ElMessage.success('地址已删除')
    await loadAddresses()
  } catch {
    // 统一错误提示
  }
}

async function unfollow(user) {
  try {
    await followApi.unfollow(user.id)
    ElMessage.success('已取消关注')
    await loadFollows()
  } catch {
    // 统一错误提示
  }
}

watch(
  () => route.query.tab,
  (value) => {
    activeTab.value = tabsKeys.value.includes(value) ? value : 'profile'
  }
)

watch(
  () => auth.user,
  syncProfileForm,
  { immediate: true, deep: true }
)

onMounted(async () => {
  if (tabsKeys.value.includes(route.query.tab)) activeTab.value = route.query.tab
  await auth.loadMe()
  syncProfileForm()
  await Promise.all([loadSignups(), loadAddresses(), loadFollows(), loadCheckin()])
})
</script>
