<template>
  <div class="student-exam-list-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的考试</span>
        </div>
      </template>

      <div class="filter-area">
        <el-input
          v-model="searchQuery"
          placeholder="搜索考试名称或ID"
          clearable
          style="width: 300px; margin-right: 15px;"
          @change="fetchExams"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-button :icon="Refresh" @click="fetchExams" plain>
          刷新列表
        </el-button>
      </div>

      <el-tabs v-model="activeTab" class="exam-tabs" @tab-change="fetchExams">
        <el-tab-pane label="进行中 / 待开始" name="active">
          <el-table
            :data="activeExams"
            v-loading="loading"
            :style="{ width: '100%' }"
            stripe
          >
            <el-table-column prop="name" label="考试名称" min-width="200" show-overflow-tooltip />

            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)" effect="light" size="small">
                  {{ formatStatus(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="startTime" label="开始时间" width="180" />
            <el-table-column prop="duration" label="时长(分钟)" width="120" />

            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button
                  link
                  :type="scope.row.status === 'ONGOING' ? 'primary' : 'info'"
                  size="small"
                  @click="handleViewExam(scope.row.id)"
                >
                  {{ scope.row.status === 'ONGOING' ? '进入考试' : '查看详情' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="已结束" name="finished">
          <el-table
            :data="finishedExams"
            v-loading="loading"
            :style="{ width: '100%' }"
            stripe
          >
            <el-table-column prop="name" label="考试名称" min-width="200" show-overflow-tooltip />

            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)" effect="light" size="small">
                  {{ formatStatus(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="startTime" label="开始时间" width="180" />
            <el-table-column prop="duration" label="时长(分钟)" width="120" />

            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button
                  link
                  type="info"
                  size="small"
                  @click="handleViewExam(scope.row.id)"
                >
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pagination.page"
          :page-size="pagination.limit"
          :total="pagination.total"
          layout="total, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import apiClient from '@/services/apiClient'
import { endpoints } from '@/services/endpoints'

const router = useRouter()

type ExamStatus = 'DRAFT' | 'SCHEDULED' | 'ONGOING' | 'FINISHED' | 'CANCELLED'

interface Exam {
  id: number
  name: string
  status: ExamStatus
  startTime: string
  duration: number
}

const activeTab = ref<'active' | 'finished'>('active')
const loading = ref(false)
const searchQuery = ref('')
const activeExams = ref<Exam[]>([])
const finishedExams = ref<Exam[]>([])
const pagination = ref({
  page: 1,
  limit: 10,
  total: 0
})

const formatDateTime = (value: string | null | undefined) => {
  if (!value) return ''
  try {
    const d = new Date(value)
    return d.toLocaleString()
  } catch {
    return value
  }
}

const getStatusType = (status: ExamStatus) => {
  switch (status) {
    case 'ONGOING': return 'success'
    case 'SCHEDULED': return 'warning'
    case 'FINISHED': return 'info'
    case 'CANCELLED': return 'danger'
    case 'DRAFT':
    default: return 'info'
  }
}

const formatStatus = (status: ExamStatus) => {
  const map: Record<ExamStatus, string> = {
    ONGOING: '进行中',
    SCHEDULED: '待开始',
    FINISHED: '已结束',
    CANCELLED: '已取消',
    DRAFT: '草稿'
  }
  return map[status] || '未知'
}

const fetchExams = async () => {
  loading.value = true
  try {
    const { data: pageData } = await apiClient.get(endpoints.exams.list, {
      params: {
        page: pagination.value.page - 1,
        size: pagination.value.limit
      }
    })

    const exams = (pageData.content ?? []) as any[]
    const mapped: Exam[] = exams.map((e) => ({
      id: e.id,
      name: e.title,
      status: e.status as ExamStatus,
      startTime: formatDateTime(e.startTime),
      duration: e.duration ?? 0
    }))

    const filtered = mapped.filter(exam =>
      !searchQuery.value ||
      exam.name.includes(searchQuery.value) ||
      exam.id.toString().includes(searchQuery.value)
    )

    activeExams.value = filtered.filter(e => e.status === 'SCHEDULED' || e.status === 'ONGOING')
    finishedExams.value = filtered.filter(e => e.status === 'FINISHED' || e.status === 'CANCELLED')

    pagination.value.total = activeTab.value === 'active' ? activeExams.value.length : finishedExams.value.length
  } catch (error) {
    console.error('Fetch Exam List Error:', error)
    ElMessage.error('加载考试列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val: number) => {
  pagination.value.limit = val
  pagination.value.page = 1
  fetchExams()
}

const handleCurrentChange = (val: number) => {
  pagination.value.page = val
  fetchExams()
}

const handleViewExam = (id: number) => {
  router.push({ name: 'StudentExamDetail', params: { id: id.toString() } })
}

onMounted(() => {
  fetchExams()
})
</script>

<style scoped>
.filter-area {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.exam-tabs {
  margin-top: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

