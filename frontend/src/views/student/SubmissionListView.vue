<template>
  <div class="submission-list-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的提交历史</span>
        </div>
      </template>

      <div class="filter-area">
        <el-input
          v-model="searchQuery"
          placeholder="搜索题目名称或提交ID"
          clearable
          style="width: 250px; margin-right: 15px;"
          @change="fetchSubmissions"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="filter.result"
          placeholder="筛选提交结果"
          clearable
          style="width: 150px; margin-right: 15px;"
          @change="fetchSubmissions"
        >
          <el-option label="通过 (AC)" value="ACCEPTED" />
          <el-option label="错误 (WA)" value="WRONG_ANSWER" />
          <el-option label="待评测" value="PENDING" />
          <el-option label="编译失败" value="COMPILE_ERROR" />
        </el-select>

        <el-button :icon="Refresh" @click="fetchSubmissions" plain>
          刷新
        </el-button>
      </div>

      <el-table
        :data="submissionList"
        v-loading="loading"
        :style="{ width: '100%', marginTop: '20px' }"
        border
      >
        <el-table-column prop="id" label="ID" width="80" />

        <el-table-column prop="problemTitle" label="题目名称" min-width="180" show-overflow-tooltip />

        <el-table-column label="结果" width="140">
          <template #default="scope">
            <el-tag :type="getResultType(scope.row.status)" effect="dark" size="small">
              {{ formatResult(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="score" label="得分" width="100">
          <template #default="scope">
            <span v-if="scope.row.score !== null && scope.row.score !== undefined">
              {{ scope.row.score }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column prop="submitTime" label="提交时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.submitTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="handleViewDetail(scope.row.id)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pagination.page"
          :page-sizes="[10, 20, 50]"
          :page-size="pagination.limit"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          background
        />
      </div>
      <el-dialog
        v-model="detailDialogVisible"
        title="提交详情"
        width="800px"
      >
        <div v-loading="detailLoading" style="min-height: 200px;">
          <div v-if="detailData">
            <h3 style="margin-bottom: 10px;">
              {{ detailData.problemTitle }}
            </h3>
            <p v-if="detailData.creatorName" style="color: var(--el-text-color-secondary); margin-bottom: 10px;">
              出题人：{{ detailData.creatorName }}
            </p>
            <p style="margin-bottom: 10px;">
              难度：{{ detailData.difficulty || '未知' }}，
              时间限制：{{ detailData.timeLimit != null ? detailData.timeLimit + ' ms' : '未设置' }}，
              内存限制：{{ detailData.memoryLimit != null ? detailData.memoryLimit + ' MB' : '未设置' }}
            </p>
            <div
              v-if="detailData.problemDescription"
              class="problem-description"
              v-html="detailData.problemDescription"
              style="margin-bottom: 15px; padding: 10px; background: #fafafa; border-radius: 4px;"
            />

            <el-divider />

            <p style="margin-bottom: 6px;">
              提交结果：
              <el-tag :type="getResultType(detailData.status)" effect="dark" size="small">
                {{ formatResult(detailData.status) }}
              </el-tag>
              <span style="margin-left: 12px;">
                得分：{{ detailData.score != null ? detailData.score : '-' }}
              </span>
            </p>
            <p style="margin-bottom: 6px;">
              提交时间：{{ formatDateTime(detailData.submitTime) }}
            </p>
            <p style="margin-bottom: 10px;">
              评测耗时：{{ detailData.timeUsed != null ? detailData.timeUsed + ' ms' : '未知' }}，
              内存：{{ detailData.memoryUsed != null ? detailData.memoryUsed + ' KB' : '未知' }}
            </p>

            <el-input
              v-model="detailData.code"
              type="textarea"
              :rows="12"
              readonly
              placeholder="本次提交的代码内容"
            />
          </div>
          <div v-else-if="!detailLoading">
            暂无详情数据
          </div>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import apiClient from '@/services/apiClient'

interface SubmissionRow {
  id: number
  problemId: number
  problemTitle: string
  status: string
  score: number | null
  submitTime: string
}

const loading = ref(false)
const searchQuery = ref('')
const submissionList = ref<SubmissionRow[]>([])
const filter = ref({
  result: ''
})
const pagination = ref({
  page: 1,
  limit: 10,
  total: 0
})

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<any | null>(null)

const formatDateTime = (value: string | null | undefined) => {
  if (!value) return ''
  try {
    const d = new Date(value)
    return d.toLocaleString()
  } catch {
    return value
  }
}

const fetchSubmissions = async () => {
  loading.value = true
  try {
    const { data: pageData } = await apiClient.get('/submissions/my', {
      params: {
        page: pagination.value.page - 1,
        size: pagination.value.limit
      }
    })

    const items = (pageData.content ?? []) as any[]
    let rows: SubmissionRow[] = items.map((s) => ({
      id: s.id,
      problemId: s.problemId,
      problemTitle: s.problemTitle,
      status: s.status,
      score: s.score ?? null,
      submitTime: s.submitTime
    }))

    if (searchQuery.value) {
      const keyword = searchQuery.value.trim()
      rows = rows.filter(
        (r) =>
          r.problemTitle.includes(keyword) ||
          r.id.toString().includes(keyword)
      )
    }

    if (filter.value.result) {
      rows = rows.filter((r) => r.status === filter.value.result)
    }

    submissionList.value = rows
    pagination.value.total = pageData.totalElements ?? rows.length
  } catch (error) {
    console.error('Fetch Submissions Error:', error)
    ElMessage.error('加载提交记录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val: number) => {
  pagination.value.limit = val
  pagination.value.page = 1
  fetchSubmissions()
}

const handleCurrentChange = (val: number) => {
  pagination.value.page = val
  fetchSubmissions()
}

const handleViewDetail = async (id: number) => {
  detailDialogVisible.value = true
  detailLoading.value = true
  detailData.value = null

  try {
    const { data: submission } = await apiClient.get(`/submissions/${id}`)

    let problemDetail: any = null
    try {
      const { data } = await apiClient.get(`/problems/${submission.problemId}`)
      problemDetail = data
    } catch {
      problemDetail = null
    }

    detailData.value = {
      id: submission.id,
      problemId: submission.problemId,
      problemTitle: submission.problemTitle,
      status: submission.status,
      score: submission.score,
      timeUsed: submission.timeUsed,
      memoryUsed: submission.memoryUsed,
      submitTime: submission.submitTime,
      code: submission.code,
      problemDescription: problemDetail?.description ?? '',
      difficulty: problemDetail?.difficulty ?? '',
      timeLimit: problemDetail?.timeLimit ?? null,
      memoryLimit: problemDetail?.memoryLimit ?? null,
      creatorName: problemDetail?.creatorName ?? ''
    }
  } catch (error) {
    console.error('Load Submission Detail Error:', error)
    detailDialogVisible.value = false
    ElMessage.error('加载提交详情失败，请稍后重试')
  } finally {
    detailLoading.value = false
  }
}

const formatResult = (status: string) => {
  const map: Record<string, string> = {
    ACCEPTED: '通过 (AC)',
    WRONG_ANSWER: '错误 (WA)',
    PENDING: '待评测',
    JUDGING: '评测中',
    COMPILATION_ERROR: '编译失败',
    RUNTIME_ERROR: '运行时错误',
    TIME_LIMIT_EXCEEDED: '超时',
    MEMORY_LIMIT_EXCEEDED: '内存超限'
  }
  return map[status] || status || '未知'
}

const getResultType = (status: string) => {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    ACCEPTED: 'success',
    WRONG_ANSWER: 'danger',
    PENDING: 'warning',
    JUDGING: 'warning',
    COMPILATION_ERROR: 'info',
    RUNTIME_ERROR: 'info',
    TIME_LIMIT_EXCEEDED: 'info',
    MEMORY_LIMIT_EXCEEDED: 'info'
  }
  return map[status] || 'info'
}

onMounted(() => {
  fetchSubmissions()
})
</script>

<style scoped>
.filter-area {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
