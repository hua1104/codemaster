<template>
  <div class="teacher-class-list-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>班级管理</span>
        </div>
      </template>

      <el-table
        :data="classList"
        v-loading="loading"
        style="width: 100%;"
        stripe
      >
        <el-table-column prop="className" label="班级名称" min-width="150" />
        <el-table-column prop="studentCount" label="参与学生数" width="120" />
        <el-table-column prop="submissionCount" label="提交次数" width="120" />
        <el-table-column label="平均得分" width="120">
          <template #default="{ row }">
            <span v-if="row.averageScore !== null">
              {{ row.averageScore.toFixed(1) }}
            </span>
            <span v-else>暂无数据</span>
          </template>
        </el-table-column>
      </el-table>

      <p v-if="!loading && classList.length === 0" class="empty-tip">
        暂无班级数据。只有当学生参加您创建的考试并提交答案后，
        才会在这里按班级聚合统计。
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import apiClient from '@/services/apiClient'
import { endpoints } from '@/services/endpoints'

interface TeacherClassSummary {
  className: string
  studentCount: number
  submissionCount: number
  averageScore: number | null
}

const classList = ref<TeacherClassSummary[]>([])
const loading = ref(false)

const fetchClasses = async () => {
  loading.value = true
  try {
    const { data } = await apiClient.get<TeacherClassSummary[]>(endpoints.teacher.classes)
    classList.value = data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchClasses().catch(err => {
    console.error('加载教师班级数据失败', err)
  })
})
</script>

<style scoped>
.teacher-class-list-view {
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
