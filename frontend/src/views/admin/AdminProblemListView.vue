<template>
  <div class="admin-problem-list-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>题库列表管理</span>
        </div>
      </template>

      <div class="filter-and-action">
        <el-input
          v-model="searchQuery"
          placeholder="搜索题目名称或描述"
          clearable
          style="width: 300px; margin-right: 15px;"
          @change="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="filter.difficulty"
          placeholder="筛选难度"
          clearable
          style="width: 120px; margin-right: 15px;"
          @change="handleSearch"
        >
          <el-option label="简单" value="EASY" />
          <el-option label="中等" value="MEDIUM" />
          <el-option label="困难" value="HARD" />
        </el-select>

        <el-button type="primary" :icon="Plus" @click="handleCreate">
          新增题目
        </el-button>
        <el-button :icon="Refresh" @click="fetchProblems" plain>
          刷新
        </el-button>
      </div>

      <el-table
        :data="problemList"
        v-loading="loading"
        :style="{ width: '100%', marginTop: '20px' }"
        border
      >
        <el-table-column prop="id" label="ID" width="80" />

        <el-table-column prop="title" label="题目名称" min-width="220" show-overflow-tooltip />

        <el-table-column label="难度" width="100">
          <template #default="scope">
            <el-tag :type="getDifficultyTag(scope.row.difficulty)" effect="dark" size="small">
              {{ formatDifficulty(scope.row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="creatorName" label="创建人" width="120" />

        <el-table-column prop="createTime" label="创建时间" min-width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="公开" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.isPublic ? 'success' : 'info'" size="small">
              {{ scope.row.isPublic ? '公开' : '私有' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="handleEdit(scope.row.id)">
              编辑
            </el-button>
            <el-button link type="info" size="small" @click="handleView(scope.row.id)">
              预览
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(scope.row.id)">
              删除
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
          :page-size="pagination.size"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'
import apiClient from '@/services/apiClient'
import { endpoints } from '@/services/endpoints'

interface Problem {
  id: number
  title: string
  description: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  creatorName: string
  createTime: string
  isPublic: boolean
}

const router = useRouter()
const route = useRoute()

const isTeacherContext = computed(() => route.path.startsWith('/teacher'))

const problemList = ref<Problem[]>([])
const loading = ref(false)
const searchQuery = ref('')
const filter = ref<{ difficulty: '' | 'EASY' | 'MEDIUM' | 'HARD' }>({
  difficulty: ''
})
const pagination = ref({
  page: 1,
  size: 10,
  total: 0
})

const fetchProblems = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: pagination.value.page - 1,
      size: pagination.value.size
    }
    if (searchQuery.value.trim()) {
      params.keyword = searchQuery.value.trim()
    }
    if (filter.value.difficulty) {
      params.difficulty = filter.value.difficulty
    }

    // 管理员/老师查看可访问的题目（管理员共享题库）
    const { data } = await apiClient.get(endpoints.problems.listAll, { params })

    problemList.value = data.content
    pagination.value.total = data.totalElements
  } catch (error) {
    ElMessage.error('加载题目列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val: number) => {
  pagination.value.size = val
  pagination.value.page = 1
  fetchProblems()
}

const handleCurrentChange = (val: number) => {
  pagination.value.page = val
  fetchProblems()
}

const handleCreate = () => {
  router.push({
    name: isTeacherContext.value ? 'TeacherProblemCreate' : 'AdminProblemCreate'
  })
}

const handleEdit = (id: number) => {
  router.push({
    name: isTeacherContext.value ? 'TeacherProblemEdit' : 'AdminProblemEdit',
    params: { id: id.toString() }
  })
}

const handleView = (id: number) => {
  ElMessage.info(`预览题目 ID: ${id}`)
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      `确定删除 ID 为 ${id} 的题目吗？`,
      '警告',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await apiClient.delete(endpoints.problems.delete(id))
    ElMessage.success('题目删除成功')
    fetchProblems()
  } catch {
    // 用户取消或请求失败时忽略
  }
}

const formatDifficulty = (difficulty: Problem['difficulty']) => {
  const map: Record<Problem['difficulty'], string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  }
  return map[difficulty] || '未知'
}

const getDifficultyTag = (difficulty: Problem['difficulty']) => {
  const map: Record<Problem['difficulty'], 'success' | 'warning' | 'danger'> = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger'
  }
  return map[difficulty] || 'info'
}

const formatDate = (value: string) => {
  if (!value) return ''
  try {
    const d = new Date(value)
    return d.toLocaleString()
  } catch {
    return value
  }
}

const handleSearch = () => {
  pagination.value.page = 1
  fetchProblems()
}

onMounted(() => {
  fetchProblems()
})
</script>

<style scoped>
.filter-and-action {
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
