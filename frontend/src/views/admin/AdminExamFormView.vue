<template>
  <div class="admin-exam-form-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <h2>{{ isEditMode ? '编辑考试' : '创建新考试' }}</h2>
          <el-button @click="router.back()" link type="primary">
            <el-icon><Back /></el-icon>
            返回列表
          </el-button>
        </div>
      </template>

      <el-form
        :model="form"
        :rules="rules"
        ref="examFormRef"
        label-width="120px"
        v-loading="loading"
        class="exam-form"
      >
        <el-divider content-position="left">基础配置</el-divider>
        <el-form-item label="考试名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：2025年软件工程期末考试" />
        </el-form-item>

        <el-form-item label="考试描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入考试的简要说明和注意事项"
          />
        </el-form-item>

        <el-divider content-position="left">时间设置</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="选择考试开始的日期和时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试时长(分)" prop="duration">
              <el-input-number
                v-model="form.duration"
                :min="10"
                :max="300"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">考试规则</el-divider>

        <el-form-item label="考试状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="DRAFT">草稿（暂不可见）</el-radio>
            <el-radio label="SCHEDULED">已排期（学生可见）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="允许中途退出">
              <el-switch
                v-model="form.allowExit"
                active-text="是"
                inactive-text="否"
              />
              <el-tooltip content="设置为“否”将强制考生停留在考试页面">
                <el-icon style="margin-left: 10px;"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示答案">
              <el-switch
                v-model="form.showAnswerAfter"
                active-text="结束后显示"
                inactive-text="不显示"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">考试题目</el-divider>

        <el-form-item>
          <el-button type="primary" @click="openProblemDialog" :disabled="isEditMode">
            从题库添加题目
          </el-button>
          <span style="margin-left: 12px; color: var(--el-text-color-secondary);">
            {{ isEditMode ? '当前暂不支持在编辑模式下修改题目列表' : '可以为本次考试选择题目并设置分数' }}
          </span>
        </el-form-item>

        <el-table
          :data="selectedProblems"
          v-loading="problemsLoading"
          class="exam-problem-table"
          style="margin-bottom: 20px;"
        >
          <el-table-column prop="id" label="题目ID" width="90" />
          <el-table-column prop="title" label="题目名称" min-width="220" show-overflow-tooltip />
          <el-table-column prop="difficulty" label="难度" width="100">
            <template #default="{ row }">
              <el-tag :type="difficultyTag(row.difficulty)" size="small">
                {{ formatDifficulty(row.difficulty) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="分数" width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.score"
                :min="1"
                :max="100"
                controls-position="right"
                :disabled="isEditMode"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button
                link
                type="danger"
                size="small"
                @click="removeSelectedProblem(row.id)"
                :disabled="isEditMode"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-form-item style="margin-top: 10px;">
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEditMode ? '保存修改' : '创建考试' }}
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="isEditMode"
        title="温馨提示"
        type="info"
        description="编辑已排期的考试时，请注意时间变更可能会影响正在进行的考试或考生的准备。当前版本暂不支持在编辑模式下修改题目列表。"
        :closable="false"
        style="margin-top: 20px;"
      />
    </el-card>

    <!-- 题库选择弹窗 -->
    <el-dialog
      v-model="problemDialogVisible"
      title="从题库选择题目"
      width="800px"
    >
      <el-table
        :data="availableProblems"
        v-loading="problemsLoading"
        style="width: 100%;"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="题目名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="difficulty" label="难度" width="100">
          <template #default="{ row }">
            <el-tag :type="difficultyTag(row.difficulty)" size="small">
              {{ formatDifficulty(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="addProblemToSelection(row)"
            >
              添加
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="problemDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Back, InfoFilled } from '@element-plus/icons-vue'
import apiClient from '@/services/apiClient'
import { endpoints } from '@/services/endpoints'

const router = useRouter()
const route = useRoute()

const props = defineProps<{
  id?: string
}>()

type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

interface ExamForm {
  name: string
  description: string
  startTime: string | null
  duration: number
  status: 'DRAFT' | 'SCHEDULED'
  allowExit: boolean
  showAnswerAfter: boolean
}

interface ProblemOption {
  id: number
  title: string
  difficulty: Difficulty
}

interface SelectedProblem extends ProblemOption {
  score: number
}

const examFormRef = ref<FormInstance>()
const loading = ref(false)
const submitLoading = ref(false)
const isEditMode = ref(false)

const initialFormState: ExamForm = {
  name: '',
  description: '',
  startTime: null,
  duration: 60,
  status: 'DRAFT',
  allowExit: false,
  showAnswerAfter: false
}

const form = reactive<ExamForm>({ ...initialFormState })

const rules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入考试名称', trigger: 'blur' },
    { min: 3, max: 100, message: '长度应在 3 到 100 个字符', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '请选择考试开始时间', trigger: 'change' }
  ],
  duration: [
    { required: true, message: '请设置考试时长', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择考试状态', trigger: 'change' }
  ]
})

// 题目选择相关状态
const problemDialogVisible = ref(false)
const problemsLoading = ref(false)
const availableProblems = ref<ProblemOption[]>([])
const selectedProblems = ref<SelectedProblem[]>([])

const difficultyTag = (difficulty: Difficulty) => {
  const map: Record<Difficulty, 'success' | 'warning' | 'danger'> = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger'
  }
  return map[difficulty]
}

const formatDifficulty = (difficulty: Difficulty) => {
  const map: Record<Difficulty, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  }
  return map[difficulty] || '未知'
}

const loadAvailableProblems = async () => {
  problemsLoading.value = true
  try {
    // 使用可访问题目列表：管理员看到全部题目，教师看到公开题目 + 自己创建的题目
    const { data } = await apiClient.get(endpoints.problems.listAll, {
      params: { page: 0, size: 100 }
    })
    const items = (data.content ?? []) as any[]
    availableProblems.value = items.map((p) => ({
      id: p.id,
      title: p.title,
      difficulty: p.difficulty as Difficulty
    }))
  } catch (error) {
    ElMessage.error('加载题库失败，请稍后重试')
  } finally {
    problemsLoading.value = false
  }
}

const openProblemDialog = () => {
  if (!problemDialogVisible.value) {
    loadAvailableProblems().catch(() => {})
  }
  problemDialogVisible.value = true
}

const addProblemToSelection = (problem: ProblemOption) => {
  if (selectedProblems.value.some((p) => p.id === problem.id)) {
    ElMessage.info('该题目已在列表中')
    return
  }
  selectedProblems.value.push({
    ...problem,
    score: 10
  })
}

const removeSelectedProblem = (id: number) => {
  selectedProblems.value = selectedProblems.value.filter((p) => p.id !== id)
}

const fetchExamDetail = async (id: string) => {
  loading.value = true
  try {
    const { data } = await apiClient.get(endpoints.exams.detail(id))
    form.name = data.title
    form.description = data.description
    form.startTime = data.startTime?.replace('T', ' ').substring(0, 19) ?? null
    form.duration = data.duration ?? initialFormState.duration
    // 状态在后端由时间和状态字段决定，这里保持不变，主要用于展示
    form.status = data.status === 'SCHEDULED' ? 'SCHEDULED' : 'DRAFT'
    // allowExit / showAnswerAfter 暂无后端字段，这里保持默认
  } catch (error) {
    ElMessage.error('加载考试详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!examFormRef.value) return

  await examFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('表单验证失败，请检查输入项')
      return
    }

    if (!form.startTime) {
      ElMessage.error('请选择开始时间')
      return
    }

    submitLoading.value = true
    try {
      // 构造 ExamCreateRequest
      const startIso = form.startTime.replace(' ', 'T')
      const startDate = new Date(startIso)
      const endDate = new Date(startDate.getTime() + form.duration * 60 * 1000)
      const pad = (n: number) => n.toString().padStart(2, '0')
      const endIso = `${endDate.getFullYear()}-${pad(endDate.getMonth() + 1)}-${pad(endDate.getDate())}T${pad(endDate.getHours())}:${pad(endDate.getMinutes())}:${pad(endDate.getSeconds())}`

      const payload = {
        title: form.name,
        description: form.description,
        startTime: startIso,
        endTime: endIso,
        duration: form.duration
      }

      if (isEditMode.value && props.id) {
        await apiClient.put(`/exams/${props.id}`, payload)
        ElMessage.success(`考试 ${props.id} 保存成功！`)
      } else {
        const { data } = await apiClient.post('/exams', payload)
        const examId = data.id

        if (examId && selectedProblems.value.length > 0) {
          for (const p of selectedProblems.value) {
            await apiClient.post(
              `/exams/${examId}/problems/${p.id}`,
              null,
              { params: { score: p.score } }
            )
          }
        }

        ElMessage.success('新考试创建成功！')
      }

      router.push({
        name: route.path.startsWith('/teacher') ? 'TeacherExamList' : 'AdminExamList'
      })
    } catch (error) {
      ElMessage.error(isEditMode.value ? '保存失败，请稍后重试' : '创建失败，请稍后重试')
    } finally {
      submitLoading.value = false
    }
  })
}

const resetForm = () => {
  if (examFormRef.value) {
    examFormRef.value.resetFields()
  }
  Object.assign(form, initialFormState)
  selectedProblems.value = []
}

onMounted(() => {
  if (props.id) {
    isEditMode.value = true
    fetchExamDetail(props.id)
  } else {
    isEditMode.value = false
  }
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h2 {
  font-size: 20px;
  margin: 0;
  color: var(--el-text-color-primary);
}

.exam-form {
  max-width: 900px;
  padding-right: 20px;
}

.el-divider {
  margin-top: 30px;
  margin-bottom: 30px;
}

.el-icon {
  vertical-align: middle;
}

.exam-problem-table {
  max-width: 900px;
}
</style>
