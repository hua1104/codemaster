<template>
  <div class="admin-dashboard-view">
    <el-card shadow="hover" class="welcome-card">
      <div class="welcome-content">
        <el-icon :size="50" style="margin-right: 20px;"><Monitor /></el-icon>
        <div class="text-content">
          <h1 class="welcome-title">欢迎回来，系统管理员</h1>
          <p class="welcome-subtitle">这里是系统核心概览，请留意待处理事项。</p>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" class="data-row">
      <el-col :span="6">
        <StatisticCard icon-type="Files" title="总考试数量" :value="stats.totalExams" type="primary" />
      </el-col>
      <el-col :span="6">
        <StatisticCard icon-type="CollectionTag" title="总题目数量" :value="stats.totalProblems" type="success" />
      </el-col>
      <el-col :span="6">
        <StatisticCard icon-type="UserFilled" title="用户总数" :value="stats.totalUsers" type="warning" />
      </el-col>
      <el-col :span="6">
        <StatisticCard icon-type="ChatLineRound" title="提交总数" :value="stats.totalSubmissions" type="danger" />
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-title">
              <el-icon><DataAnalysis /></el-icon>
              <span>考试状态分布</span>
            </div>
          </template>
          <div class="chart-placeholder">
            <div ref="examStatusChartRef" class="exam-status-chart"></div>
            <el-tag type="info" size="large" style="margin-top: 16px;">
              总考试数 {{ stats.totalExams }}
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-title">
              <el-icon><List /></el-icon>
              <span>最近提交 / 活动</span>
            </div>
          </template>

          <el-table :data="recentActivities" :show-header="true" stripe :style="{ width: '100%' }">
            <el-table-column prop="time" label="时间" width="150" />
            <el-table-column prop="user" label="用户" width="120" />
            <el-table-column prop="action" label="操作内容" show-overflow-tooltip />
          </el-table>

          <div v-if="recentActivities.length === 0" style="margin-top: 12px; text-align: center; color: var(--el-text-color-secondary);">
            暂无最近活动
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Monitor, Files, CollectionTag, UserFilled, ChatLineRound, DataAnalysis, List } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import StatisticCard from '@/components/common/StatisticCard.vue'
import apiClient from '@/services/apiClient'
import { endpoints } from '@/services/endpoints'

interface Activity {
  time: string
  user: string
  action: string
}

interface AdminStats {
  totalUsers: number
  totalProblems: number
  totalExams: number
  ongoingExams: number
  draftExams: number
  scheduledExams: number
  finishedExams: number
  cancelledExams: number
  totalSubmissions: number
  acceptedSubmissions: number
}

const stats = ref<AdminStats>({
  totalUsers: 0,
  totalProblems: 0,
  totalExams: 0,
  ongoingExams: 0,
  draftExams: 0,
  scheduledExams: 0,
  finishedExams: 0,
  cancelledExams: 0,
  totalSubmissions: 0,
  acceptedSubmissions: 0
})

const examStatusChartRef = ref<HTMLDivElement | null>(null)
let examStatusChart: echarts.ECharts | null = null
const recentActivities = ref<Activity[]>([])

const renderExamStatusChart = () => {
  if (!examStatusChartRef.value) return

  const data = [
    { name: '草稿', value: stats.value.draftExams },
    { name: '未开始', value: stats.value.scheduledExams },
    { name: '进行中', value: stats.value.ongoingExams },
    { name: '已结束', value: stats.value.finishedExams },
    { name: '已取消', value: stats.value.cancelledExams }
  ].filter(item => item.value && item.value > 0)

  if (!examStatusChart) {
    examStatusChart = echarts.init(examStatusChartRef.value)
  }

  examStatusChart.setOption({
    tooltip: {
      trigger: 'item'
    },
    legend: {
      bottom: 0
    },
    series: [
      {
        name: '考试状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {c} ({d}%)'
        },
        data
      }
    ]
  })
}

const fetchDashboardData = async () => {
  const { data } = await apiClient.get(endpoints.admin.statistics)

  stats.value.totalExams = data.totalExams ?? 0
  stats.value.totalProblems = data.totalProblems ?? 0
  stats.value.totalUsers = data.totalUsers ?? 0
  stats.value.totalSubmissions = data.totalSubmissions ?? 0
  stats.value.acceptedSubmissions = data.acceptedSubmissions ?? 0

  stats.value.scheduledExams = data.scheduledExams ?? 0
  stats.value.ongoingExams = data.ongoingExams ?? 0
  stats.value.finishedExams = data.finishedExams ?? 0
  stats.value.cancelledExams = data.cancelledExams ?? 0
  stats.value.draftExams = data.draftExams ?? 0

  renderExamStatusChart()

  try {
    const { data: activities } = await apiClient.get<Activity[]>(endpoints.admin.recentActivities)
    recentActivities.value = activities
  } catch {
    recentActivities.value = []
  }
}

onMounted(() => {
  fetchDashboardData().catch((err) => {
    console.error('加载管理员仪表盘数据失败', err)
  })
})

onBeforeUnmount(() => {
  if (examStatusChart) {
    examStatusChart.dispose()
    examStatusChart = null
  }
})
</script>

<style scoped>
.welcome-card {
  margin-bottom: 20px;
}
.welcome-content {
  display: flex;
  align-items: center;
}
.welcome-title {
  font-size: 24px;
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
.chart-row {
  margin-bottom: 20px;
}
.chart-card {
  height: 400px;
}
.card-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}
.card-title .el-icon {
  margin-right: 8px;
  color: var(--el-color-info);
}
.chart-placeholder {
  height: 300px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
}

.exam-status-chart {
  width: 100%;
  height: 240px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

