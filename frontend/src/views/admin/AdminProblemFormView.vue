<template>
  <div class="admin-problem-form-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <h2>{{ isEditMode ? `编辑题目 ID: ${problemId}` : '创建新题目' }}</h2>
          <el-button @click="router.back()" link type="primary">
            <el-icon><Back /></el-icon>
            返回列表
          </el-button>
        </div>
      </template>

      <el-form
        :model="form"
        :rules="rules"
        ref="problemFormRef"
        label-width="100px"
        class="problem-form"
      >
        <el-row :gutter="40">
          <el-col :span="10">
            <el-divider content-position="left">基础信息</el-divider>

            <el-form-item label="题目名称" prop="title">
              <el-input v-model="form.title" placeholder="请输入题目名称" />
            </el-form-item>

            <el-form-item label="题目类型" prop="type">
              <el-radio-group v-model="form.type">
                <el-radio-button label="CODING">编程题</el-radio-button>
                <el-radio-button label="CHOICE">选择题</el-radio-button>
                <el-radio-button label="FILL_BLANK">填空题</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="form.difficulty" placeholder="请选择难度" style="width: 100%;">
                <el-option label="简单 (EASY)" value="EASY" />
                <el-option label="中等 (MEDIUM)" value="MEDIUM" />
                <el-option label="困难 (HARD)" value="HARD" />
              </el-select>
            </el-form-item>

            <el-form-item label="时间限制" prop="timeLimit" v-if="form.type === 'CODING'">
              <el-input-number
                v-model="form.timeLimit"
                :min="100"
                :max="10000"
                :step="100"
                controls-position="right"
                style="width: 100%;"
              />
              <span style="margin-left: 8px;">ms</span>
            </el-form-item>

            <el-form-item label="内存限制" prop="memoryLimit" v-if="form.type === 'CODING'">
              <el-input-number
                v-model="form.memoryLimit"
                :min="32"
                :max="1024"
                :step="32"
                controls-position="right"
                style="width: 100%;"
              />
              <span style="margin-left: 8px;">MB</span>
            </el-form-item>

            <el-form-item label="是否公开" prop="isPublic">
              <el-switch
                v-model="form.isPublic"
                active-text="公开"
                inactive-text="仅自己可见"
              />
            </el-form-item>
          </el-col>

          <el-col :span="14">
            <el-divider content-position="left">题目描述</el-divider>
            <el-form-item prop="description" label-width="0">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="6"
                placeholder="在这里输入题目背景、输入输出说明等内容"
              />
            </el-form-item>

            <template v-if="form.type === 'CHOICE'">
              <el-divider content-position="left">选择题选项</el-divider>
              <div v-for="(opt, index) in choiceOptions" :key="opt.label" style="display: flex; align-items: center; margin-bottom: 8px;">
                <el-checkbox v-model="opt.correct" style="margin-right: 8px;">正确</el-checkbox>
                <span style="width: 30px; display: inline-block;">{{ opt.label }}.</span>
                <el-input
                  v-model="opt.text"
                  placeholder="请输入选项内容"
                  style="flex: 1; margin-right: 8px;"
                />
              </div>
              <div style="font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px;">
                勾选正确选项，允许多选。
              </div>
            </template>

            <template v-else-if="form.type === 'FILL_BLANK'">
              <el-divider content-position="left">填空题答案</el-divider>
              <div v-for="(ans, index) in fillAnswers" :key="index" style="display: flex; align-items: center; margin-bottom: 8px;">
                <span style="width: 60px; display: inline-block;">空 {{ index + 1 }}：</span>
                <el-input
                  v-model="fillAnswers[index]"
                  placeholder="请输入该空的参考答案"
                  style="flex: 1;"
                />
              </div>
              <div style="font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px;">
                如有多个空可填写多行答案；目前后端仅保存文本，不自动判分。
              </div>
            </template>
          </el-col>
        </el-row>

        <el-divider />
        <el-form-item class="submit-footer">
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEditMode ? '保存修改' : '创建题目' }}
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import apiClient from '@/services/apiClient'
import { endpoints } from '@/services/endpoints'

const router = useRouter()

const props = defineProps<{
  id?: string
}>()

type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
type ProblemType = 'CODING' | 'CHOICE' | 'FILL_BLANK'

interface ProblemForm {
  title: string
  description: string
  type: ProblemType
  difficulty: Difficulty
  timeLimit: number
  memoryLimit: number
  isPublic: boolean
}

const problemFormRef = ref<FormInstance>()
const loading = ref(false)
const submitLoading = ref(false)

const problemId = ref(props.id)
const isEditMode = ref(!!props.id)

const form = reactive<ProblemForm>({
  title: '',
  description: '',
  type: 'CODING',
  difficulty: 'MEDIUM',
  timeLimit: 1000,
  memoryLimit: 256,
  isPublic: true
})

const choiceOptions = reactive(
  [
    { label: 'A', text: '', correct: false },
    { label: 'B', text: '', correct: false },
    { label: 'C', text: '', correct: false },
    { label: 'D', text: '', correct: false }
  ]
)

const fillAnswers = reactive<string[]>([''])

const rules = reactive<FormRules>({
  title: [{ required: true, message: '请输入题目名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择题目类型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  timeLimit: [{ required: true, message: '请设置时间限制', trigger: 'change' }],
  memoryLimit: [{ required: true, message: '请设置内存限制', trigger: 'change' }],
  description: [{ required: true, message: '请输入题目描述', trigger: 'blur' }]
})

const fetchProblemDetail = async (id: string) => {
  loading.value = true
  try {
    const { data } = await apiClient.get(endpoints.problems.detail(id))
    form.title = data.title
    form.description = data.description
     form.type = data.type || 'CODING'
    form.difficulty = data.difficulty
    form.timeLimit = data.timeLimit
    form.memoryLimit = data.memoryLimit
    form.isPublic = data.isPublic

    if (data.type === 'CHOICE' && data.options) {
      try {
        const parsed = JSON.parse(data.options) as Array<{ label: string; text: string; correct: boolean }>
        parsed.forEach((p, index) => {
          if (choiceOptions[index]) {
            choiceOptions[index].text = p.text
            choiceOptions[index].correct = !!p.correct
          }
        })
      } catch {
        // ignore parse error
      }
    } else if (data.type === 'FILL_BLANK' && data.answer) {
      try {
        const parsedAns = JSON.parse(data.answer) as string[]
        if (Array.isArray(parsedAns) && parsedAns.length > 0) {
          fillAnswers.splice(0, fillAnswers.length, ...parsedAns)
        }
      } catch {
        // 如果不是 JSON，就当作单个答案
        fillAnswers.splice(0, fillAnswers.length, data.answer)
      }
    }
  } catch (error) {
    ElMessage.error('加载题目详情失败，请稍后重试')
    router.back()
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!problemFormRef.value) return

  await problemFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('表单验证失败，请检查输入项')
      return
    }

    submitLoading.value = true
    try {
      let options: string | undefined
      let answer: string | undefined

      if (form.type === 'CHOICE') {
        const payloadOptions = choiceOptions.map((opt) => ({
          label: opt.label,
          text: opt.text,
          correct: opt.correct
        }))
        options = JSON.stringify(payloadOptions)
        answer = JSON.stringify(payloadOptions.filter((o) => o.correct).map((o) => o.label))
      } else if (form.type === 'FILL_BLANK') {
        const nonEmpty = fillAnswers.map((a) => a.trim()).filter((a) => a.length > 0)
        answer = JSON.stringify(nonEmpty)
      }

      const payload = {
        title: form.title,
        description: form.description,
        templateCode: '',
        type: form.type,
        options,
        answer,
        difficulty: form.difficulty,
        timeLimit: form.timeLimit,
        memoryLimit: form.memoryLimit,
        isPublic: form.isPublic
      }

      if (isEditMode.value && problemId.value) {
        await apiClient.put(endpoints.problems.update(problemId.value), payload)
        ElMessage.success('题目修改成功')
      } else {
        await apiClient.post(endpoints.problems.create, payload)
        ElMessage.success('新题目创建成功')
      }

      router.push({ name: 'AdminProblemList' })
    } catch (error) {
      ElMessage.error(isEditMode.value ? '保存失败，请稍后重试' : '创建失败，请稍后重试')
    } finally {
      submitLoading.value = false
    }
  })
}

const resetForm = () => {
  if (problemFormRef.value) {
    problemFormRef.value.resetFields()
  }
  form.title = ''
  form.description = ''
  form.difficulty = 'MEDIUM'
  form.timeLimit = 1000
  form.memoryLimit = 256
  form.isPublic = true
}

onMounted(() => {
  if (props.id) {
    isEditMode.value = true
    fetchProblemDetail(props.id)
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

.problem-form {
  padding-right: 20px;
}

.el-divider {
  margin-top: 25px;
  margin-bottom: 25px;
}
</style>
