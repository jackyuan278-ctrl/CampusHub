<template>
  <div class="page-container">
    <SectionHeading eyebrow="COMMUNITY" title="问答社区" description="遇到问题就来问问，校园经验在这里流动起来。"><template #actions><el-button type="primary" round @click="askQuestion"><el-icon><EditPen /></el-icon>我要提问</el-button></template></SectionHeading>
    <section class="community-toolbar"><div class="community-tabs"><button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button></div><div class="community-search"><el-icon><Search /></el-icon><input v-model="keyword" placeholder="搜索问题或话题" /></div></section>
    <div class="community-layout"><div class="question-list"><QuestionCard v-for="question in visibleQuestions" :key="question.id" :question="question" /></div><aside class="community-aside"><div class="aside-card topic-card"><span class="aside-kicker">HOT TOPICS</span><h3>大家最近在聊</h3><button v-for="topic in topics" :key="topic.name" @click="keyword = topic.name"># {{ topic.name }}<span>{{ topic.count }}</span></button></div><div class="aside-card join-card"><span class="join-icon"><el-icon><ChatDotRound /></el-icon></span><h3>分享你的校园经验</h3><p>一个回答，也许能帮到正在找答案的同学。</p><el-button round @click="askQuestion">去提问</el-button></div></aside></div>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { campusHubApi } from '@/api/campusHubApi'
import SectionHeading from '@/components/SectionHeading.vue'; import QuestionCard from '@/components/QuestionCard.vue'
const questions = ref([]); const activeTab = ref('latest'); const keyword = ref(''); const tabs = [{ key: 'latest', label: '最新' }, { key: 'hot', label: '最热' }]; const topics = [{ name: '校园周边', count: 28 }, { name: '宿舍生活', count: 21 }, { name: '课程学习', count: 17 }, { name: '活动报名', count: 12 }]
const visibleQuestions = computed(() => { let result = [...questions.value]; if (keyword.value) result = result.filter((q) => `${q.title}${q.desc}${q.tags.join('')}`.includes(keyword.value)); if (activeTab.value === 'hot') result.sort((a, b) => b.likes - a.likes); return result })
onMounted(async () => { questions.value = await campusHubApi.listQuestions() })
function askQuestion() { ElMessage.info('提问编辑器已预留，接入 campus-community-service 后即可发布') }
</script>
