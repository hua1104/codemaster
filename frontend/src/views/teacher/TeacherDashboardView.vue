<template>
  <div class="teacher-dashboard">
    <el-card class="welcome-card" shadow="hover">
      <div class="welcome-content">
        <h2 class="welcome-title">欢迎回来，任课教师</h2>
        <p class="welcome-subtitle">
          左侧导航可进入考试管理、题库管理、班级管理和成绩统计模块。
        </p>
      </div>
    </el-card>

    <el-row :gutter="20" v-loading="loading">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>考试概况</template>
          <p>已创建考试：{{ stats?.totalExamsCreated ?? 0 }}</p>
          <p>进行中：{{ stats?.ongoingExams ?? 0 }}，未开始：{{ stats?.scheduledExams ?? 0 }}</p>
          <p>已结束：{{ stats?.finishedExams ?? 0 }}</p>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>题库概况</template>
          <p>您创建的题目数量：{{ stats?.totalProblemsCreated ?? 0 }}</p>
          <p style="color: var(--el-text-color-secondary);">
            统计范围仅包含当前账号创建的题目。
          </p>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>考试参与情况</template>
          <p>收到的提交总数：{{ stats?.totalSubmissionsReceived ?? 0 }}</p>
          <p>参与学生人数：{{ stats?.distinctStudentsCount ?? 0 }}</p>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>班级概况</template>
          <p>涉及班级数量：{{ stats?.classCount ?? 0 }}</p>
          <p style="color: var(--el-text-color-secondary);">
            班级来源于参加您考试的学生的班级信息。
          </p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import apiClient from '@/services/apiClient'
import { endpoints } from '@/services/endpoints'

interface TeacherDashboardStats {
  totalExamsCreated: number
  ongoingExams: number
  scheduledExams: number
  finishedExams: number
  totalProblemsCreated: number
  totalSubmissionsReceived: number
  distinctStudentsCount: number
  classCount: number
}

const stats = ref<TeacherDashboardStats | null>(null)
const loading = ref(false)

const fetchDashboard = async () => {
  loading.value = true
  try {
    const { data } = await apiClient.get<TeacherDashboardStats>(endpoints.teacher.dashboard)
    stats.value = data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboard().catch(err => {
    console.error('加载教师仪表盘失败', err)
  })
})
</script>

<style scoped>
.teacher-dashboard {
  padding: 10px;
}

.welcome-card {
  margin-bottom: 20px;
}

.welcome-title {
  margin: 0 0 8px;
  font-size: 22px;
}

.welcome-subtitle {
  margin: 0;
  color: var(--el-text-color-secondary);
}
</style>

