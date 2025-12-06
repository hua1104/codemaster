<template>
  <div class="teacher-score-overview-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>考试成绩总览</span>
        </div>
      </template>

      <el-table
        :data="examScores"
        v-loading="loading"
        style="width: 100%;"
        stripe
      >
        <el-table-column prop="title" label="考试名称" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            {{ formatStatus(row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="submissionCount" label="提交次数" width="110" />
        <el-table-column prop="acceptedCount" label="通过提交数" width="120" />
        <el-table-column label="平均得分" width="120">
          <template #default="{ row }">
            <span v-if="row.averageScore !== null">
              {{ row.averageScore.toFixed(1) }}
            </span>
            <span v-else>暂无数据</span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="160">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" min-width="160">
          <template #default="{ row }">
            {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
      </el-table>

      <p v-if="!loading && examScores.length === 0" class="empty-tip">
        暂无考试成绩数据。请先创建考试并让学生提交答案。
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import apiClient from '@/services/apiClient'
import { endpoints } from '@/services/endpoints'

type ExamStatus = 'DRAFT' | 'SCHEDULED' | 'ONGOING' | 'FINISHED' | 'CANCELLED'

interface TeacherExamScoreSummary {
  examId: number
  title: string
  status: ExamStatus
  startTime: string | null
  endTime: string | null
  submissionCount: number
  acceptedCount: number
  averageScore: number | null
}

const examScores = ref<TeacherExamScoreSummary[]>([])
const loading = ref(false)

const fetchScores = async () => {
  loading.value = true
  try {
    const { data } = await apiClient.get<TeacherExamScoreSummary[]>(endpoints.teacher.scores)
    examScores.value = data
  } finally {
    loading.value = false
  }
}

const formatTime = (value: string | null) => {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD HH:mm')
}

const formatStatus = (status: ExamStatus) => {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'SCHEDULED': return '未开始'
    case 'ONGOING': return '进行中'
    case 'FINISHED': return '已结束'
    case 'CANCELLED': return '已取消'
    default: return status
  }
}

onMounted(() => {
  fetchScores().catch(err => {
    console.error('加载教师考试成绩总览失败', err)
  })
})
</script>

<style scoped>
.teacher-score-overview-view {
  padding: 10px;
}

.card-header {
  font-size: 16px;
  font-weight: 500;
}

.empty-tip {
  margin-top: 12px;
  color: var(--el-text-color-secondary);
}
</style>
