<template>
  <div class="admin-exam-list-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <h2>考试列表</h2>
          <el-button
            type="primary"
            :icon="Plus"
            @click="router.push({ name: 'AdminExamCreate' })"
          >
            创建考试
          </el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-input
          v-model="queryForm.keyword"
          placeholder="搜索考试名称"
          clearable
          style="width: 250px; margin-right: 15px;"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table
        :data="examList"
        v-loading="loading"
        class="exam-table"
      >
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="title" label="考试名称" min-width="180" />
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分钟)" width="120" />
        <el-table-column prop="problemCount" label="题目数量" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row.id)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          @current-change="handlePageChange"
          :current-page="queryForm.page"
          :page-size="queryForm.size"
          :total="total"
          layout="total, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import apiClient from '@/services/apiClient'
import { endpoints } from '@/services/endpoints'

const router = useRouter()

type ExamStatus = 'DRAFT' | 'SCHEDULED' | 'ONGOING' | 'FINISHED' | 'CANCELLED'

interface Exam {
  id: number
  title: string
  startTime: string
  endTime: string
  duration: number
  problemCount: number
  status: ExamStatus
}

interface QueryForm {
  page: number
  size: number
  keyword: string
}

type TagType = 'info' | 'success' | 'danger' | 'warning' | 'primary'

const examList = ref<Exam[]>([])
const total = ref(0)
const loading = ref(false)

const queryForm = reactive<QueryForm>({
  page: 1,
  size: 10,
  keyword: ''
})

const fetchExamList = async () => {
  loading.value = true
  try {
    const response = await apiClient.get(endpoints.admin.exams, {
      params: {
        page: queryForm.page - 1,
        size: queryForm.size,
        keyword: queryForm.keyword
      }
    })

    const pageData = response.data
    examList.value = pageData.content ?? []
    total.value = pageData.totalElements ?? 0
    if (typeof pageData.number === 'number') {
      queryForm.page = pageData.number + 1
    }
  } catch (error) {
    console.error('Fetch Exam List Error:', error)
    ElMessage.error('加载考试列表失败，请检查后端接口')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  fetchExamList()
}

const handlePageChange = (newPage: number) => {
  queryForm.page = newPage
  fetchExamList()
}

const handleEdit = (id: number) => {
  router.push({ name: 'AdminExamEdit', params: { id: id.toString() } })
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm(
    '此操作将永久删除该考试及其相关数据，是否继续？',
    '警告',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await apiClient.delete(`/exams/${id}`)
      ElMessage.success('删除成功')
      fetchExamList()
    } catch (error) {
      console.error('Delete Exam Error:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }).catch(() => {
    // 用户取消
  })
}

const getStatusText = (status: ExamStatus) => {
  const map: Record<ExamStatus, string> = {
    DRAFT: '草稿',
    SCHEDULED: '未开始',
    ONGOING: '进行中',
    FINISHED: '已结束',
    CANCELLED: '已取消'
  }
  return map[status] ?? '未知'
}

const getTagType = (status: ExamStatus): TagType => {
  const map: Record<ExamStatus, TagType> = {
    DRAFT: 'info',
    SCHEDULED: 'primary',
    ONGOING: 'success',
    FINISHED: 'danger',
    CANCELLED: 'warning'
  }
  return map[status] ?? 'info'
}

const formatDateTime = (value: string) => {
  if (!value) return ''
  try {
    const d = new Date(value)
    return d.toLocaleString()
  } catch {
    return value
  }
}

onMounted(() => {
  fetchExamList()
})
</script>

<style scoped>
.exam-table {
  width: 100%;
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  font-size: 20px;
  margin: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  padding-bottom: 10px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>

