<template>
  <div class="student-dashboard-view">
    <el-card shadow="hover" class="welcome-card">
      <div class="welcome-content">
        <el-avatar :size="60" :icon="UserFilled" style="margin-right: 20px;" />
        <div class="text-content">
          <h1 class="welcome-title">你好，{{ userName }}</h1>
          <p class="welcome-subtitle">
            欢迎回到 Code Examner 在线考试系统，请查看你的待办事项。
          </p>
        </div>
        <el-button type="primary" plain :icon="Setting" @click="goToProfile">
          前往个人设置
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="20" class="data-row">
      <el-col :span="6">
        <StatisticCard icon-type="Clock" title="待参加考试数量" :value="stats.pendingExams" type="warning" />
      </el-col>
      <el-col :span="6">
        <StatisticCard icon-type="DocumentChecked" title="最近一周提交次数" :value="stats.recentSubmissions" type="success" />
      </el-col>
      <el-col :span="6">
        <StatisticCard icon-type="MessageBox" title="总提交次数" :value="stats.totalSubmissions" type="info" />
      </el-col>
      <el-col :span="6">
        <StatisticCard icon-type="TrophyBase" title="通过率" :value="stats.acceptanceRate" type="primary" :is-percent="true" />
      </el-col>
    </el-row>

    <el-row :gutter="20" class="list-row">
      <el-col :span="12">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="card-title">
              <el-icon><Calendar /></el-icon>
              <span>可参加 / 即将开始的考试 ({{ upcomingExams.length }})</span>
              <el-button link type="primary" @click="goToExams">更多</el-button>
            </div>
          </template>

          <el-table :data="upcomingExams" :show-header="false" stripe :style="{ width: '100%' }">
            <el-table-column prop="name" label="考试名称" show-overflow-tooltip min-width="150" />
            <el-table-column prop="startTime" label="开始时间" width="180" />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="scope">
                <el-button link type="warning" size="small" @click="handleViewExam(scope.row.id)">查看</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <div style="padding: 20px; color: var(--el-text-color-secondary);">
                暂无可参加的考试
              </div>
            </template>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="card-title">
              <el-icon><Finished /></el-icon>
              <span>最近的提交记录 ({{ recentSubmissions.length }})</span>
              <el-button link type="primary" @click="goToSubmissions">更多</el-button>
            </div>
          </template>

          <el-table :data="recentSubmissions" :show-header="false" stripe :style="{ width: '100%' }">
            <el-table-column prop="problemTitle" label="题目" show-overflow-tooltip min-width="180" />
            <el-table-column prop="statusText" label="结果" width="100" />
            <el-table-column prop="score" label="得分" width="80" />
            <template #empty>
              <div style="padding: 20px; color: var(--el-text-color-secondary);">
                暂无提交记录
              </div>
            </template>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatisticCard from '@/components/common/StatisticCard.vue'
import { UserFilled, Setting, Calendar, Finished, Clock, DocumentChecked, MessageBox, TrophyBase } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import apiClient from '@/services/apiClient'
import { endpoints } from '@/services/endpoints'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

const userName = ref('学生')

interface UpcomingExam {
  id: number
  name: string
  startTime: string
}
interface RecentSubmission {
  id: number
  problemTitle: string
  score: number | null
  statusText: string
}

const stats = reactive({
  pendingExams: 0,
  recentSubmissions: 0,
  totalSubmissions: 0,
  acceptanceRate: 0
})

const upcomingExams = ref<UpcomingExam[]>([])
const recentSubmissions = ref<RecentSubmission[]>([])

const goToProfile = () => {
  router.push({ name: 'StudentProfile' })
}

const goToExams = () => {
  router.push({ name: 'StudentExamList' })
}

const goToSubmissions = () => {
  router.push({ name: 'StudentSubmissionList' })
}

const handleViewExam = (id: number) => {
  router.push({ name: 'StudentExamDetail', params: { id: id.toString() } })
}

const formatDateTime = (value: string | null | undefined) => {
  if (!value) return ''
  try {
    const d = new Date(value)
    return d.toLocaleString()
  } catch {
    return value
  }
}

const fetchDashboardData = async () => {
  try {
    if (!authStore.profile) {
      await authStore.fetchProfile()
    }
    if (authStore.profile) {
      userName.value = authStore.profile.realName || authStore.profile.username

      // 用户统计（提交次数、通过率等）
      const userId = authStore.profile.id
      if (userId) {
        const { data: userStats } = await apiClient.get(`/statistics/user/${userId}`)
        stats.totalSubmissions = userStats.totalSubmissions ?? 0
        stats.recentSubmissions = userStats.recentSubmissions ?? 0
        stats.acceptanceRate = userStats.acceptanceRate ?? 0
      }
    }

    // 可参加 / 即将开始的考试（取第一页少量数据）
    const { data: examPage } = await apiClient.get(endpoints.exams.list, {
      params: { page: 0, size: 5 }
    })
    const exams = (examPage.content ?? []) as any[]
    upcomingExams.value = exams.map((e) => ({
      id: e.id,
      name: e.title,
      startTime: formatDateTime(e.startTime)
    }))
    stats.pendingExams = upcomingExams.value.length

    // 最近提交记录
    const { data: submissionPage } = await apiClient.get('/submissions/my', {
      params: { page: 0, size: 5 }
    })
    const subs = (submissionPage.content ?? []) as any[]
    recentSubmissions.value = subs.map((s) => ({
      id: s.id,
      problemTitle: s.problemTitle,
      score: s.score ?? null,
      statusText: s.status ?? 'UNKNOWN'
    }))
  } catch (error) {
    ElMessage.error('加载仪表盘数据失败，请稍后重试')
  }
}

onMounted(() => {
  fetchDashboardData().catch(() => {})
})
</script>

<style scoped>
.welcome-card {
  margin-bottom: 20px;
}
.welcome-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.welcome-title {
  font-size: 22px;
  margin: 0;
  color: var(--el-color-primary);
}
.welcome-subtitle {
  color: var(--el-text-color-secondary);
  margin-top: 5px;
}

.data-row {
  margin-bottom: 20px;
}

.list-row {
  margin-bottom: 20px;
}
.list-card {
  height: 100%;
}
.card-title {
  display: flex;
  align-items: center;
  font-size: 16px;
}
.card-title .el-icon {
  margin-right: 8px;
  color: var(--el-color-primary);
}
.card-title span {
  flex-grow: 1;
  font-weight: bold;
}
</style>

