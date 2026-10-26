<template>
  <div class="page-container">
    <SectionHeading eyebrow="CAMPUS EVENTS" title="校园活动" description="去参加，去认识，去留下属于你的校园记忆。"><template #actions><el-button type="primary" round @click="publishActivity"><el-icon><Plus /></el-icon>发布活动</el-button></template></SectionHeading>
    <section class="event-intro"><div><span class="eyebrow">THIS WEEK</span><h2>校园里，总有一件事值得出发。</h2><p>活动报名、签到和后续通知会统一沉淀在 CampusHub，先从感兴趣的活动开始吧。</p></div><div class="event-orb"><el-icon :size="34"><Calendar /></el-icon><b>{{ activities.length }}</b><small>近期活动</small></div></section>
    <div class="filter-bar"><div class="filter-tabs"><button v-for="tab in filters" :key="tab.key" :class="{ active: activeFilter === tab.key }" @click="activeFilter = tab.key">{{ tab.label }}</button></div><span class="filter-count">{{ visibleActivities.length }} 场活动</span></div>
    <div class="activity-list"><ActivityRow v-for="activity in visibleActivities" :key="activity.id" :activity="activity" @signup="viewActivity" /></div>
    <el-empty v-if="!visibleActivities.length" description="暂无符合条件的活动" />
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { campusHubApi } from '@/api/campusHubApi'
import SectionHeading from '@/components/SectionHeading.vue'; import ActivityRow from '@/components/ActivityRow.vue'
const activities = ref([]); const activeFilter = ref('all'); const filters = [{ key: 'all', label: '全部活动' }, { key: '校园活动', label: '校园活动' }, { key: '社团活动', label: '社团活动' }, { key: '讲座', label: '讲座' }]
const visibleActivities = computed(() => activeFilter.value === 'all' ? activities.value : activities.value.filter((item) => item.type === activeFilter.value))
onMounted(async () => { activities.value = await campusHubApi.listActivities() })
function viewActivity(activity) { ElMessage.success(`活动“${activity.title}”详情已准备，报名接口待后端接入`) }
function publishActivity() { ElMessage.info('发布入口已预留，接入 campus-activity-service 后即可提交') }
</script>
