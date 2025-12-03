<template>
  <div class="exam-wrapper">
    <el-header class="exam-header">
      <div class="header-left">
        <h2 class="exam-title">{{ exam.name || '考试详情' }}</h2>
        <el-tag type="warning" size="large" effect="dark" style="margin-left: 20px;">
          {{ isTaking ? '考试进行中' : '考试回顾' }}
        </el-tag>
      </div>

      <div class="header-right">
        <div class="timer-display">
          <el-icon :size="20"><Timer /></el-icon>
          <span style="font-weight: bold; font-size: 1.2em; margin-left: 5px;">{{ formattedTimeLeft }}</span>
        </div>
        
        <el-button 
          type="primary" 
          size="large" 
          :disabled="!isTaking" 
          @click="submitExam" 
          style="margin-left: 20px;"
        >
          提交试卷
        </el-button>
      </div>
    </el-header>

    <el-container class="exam-content">
      <el-aside width="250px" class="question-sidebar">
        <el-card shadow="never" class="sidebar-card">
          <template #header>
            <div class="card-header">
              <span>试题导航 ({{ currentProblemIndex + 1 }} / {{ exam.problems.length }})</span>
            </div>
          </template>
          
          <div class="problem-grid">
            <el-button
              v-for="(problem, index) in exam.problems"
              :key="problem.id"
              :type="getProblemStatusType(problem.status)"
              :plain="currentProblemIndex !== index"
              size="small"
              circle
              @click="setCurrentProblem(index)"
            >
              {{ index + 1 }}
            </el-button>
          </div>
        </el-card>
      </el-aside>

      <el-main class="problem-main">
        <el-card shadow="hover" class="problem-statement-card">
          <template #header>
            <div class="card-header">
              <span>问题 {{ currentProblemIndex + 1 }}</span>
              <el-tag size="small">{{ exam.problems[currentProblemIndex]?.score || 0 }} 分</el-tag>
            </div>
          </template>
          
          <div v-if="exam.problems.length > 0" class="problem-content">
            <div v-html="exam.problems[currentProblemIndex]?.content"></div>

            <div v-if="exam.problems[currentProblemIndex]?.type === 'CHOICE'" class="choice-options">
              <el-radio-group v-model="currentAnswer">
                <el-radio
                  v-for="opt in exam.problems[currentProblemIndex]?.options || []"
                  :key="opt.label"
                  :label="opt.label"
                >
                  {{ opt.label }}. {{ opt.text }}
                </el-radio>
              </el-radio-group>
            </div>
          </div>
          <div v-else class="problem-content" style="color: var(--el-text-color-secondary);">
            本次考试尚未配置题目，请联系老师。
          </div>
        </el-card>

        <el-card shadow="hover" class="answer-card">
          <template #header>
            作答区（答案将保存到服务器并参与判分）
          </template>
          
          <template v-if="exam.problems[currentProblemIndex]?.type === 'CHOICE'">
            <p style="margin-bottom: 8px; font-size: 13px; color: var(--el-text-color-secondary);">
              请选择一个选项作为本题答案。
            </p>
            <el-radio-group v-model="currentAnswer">
              <el-radio
                v-for="opt in exam.problems[currentProblemIndex]?.options || []"
                :key="opt.label"
                :label="opt.label"
              >
                {{ opt.label }}. {{ opt.text }}
              </el-radio>
            </el-radio-group>
          </template>
          <template v-else-if="exam.problems[currentProblemIndex]?.type === 'FILL_BLANK'">
            <el-input 
              type="textarea" 
              :rows="8" 
              placeholder="请在这里填写本题的答案（如有多个空，可依次写出）。"
              v-model="currentAnswer"
              resize="none"
            />
          </template>
          <template v-else>
            <el-input 
              type="textarea" 
              :rows="10" 
              placeholder="在这里输入您的代码..."
              v-model="currentAnswer"
              resize="none"
            />
          </template>
        </el-card>

        <div class="problem-actions">
          <el-button @click="prevProblem" :disabled="currentProblemIndex === 0">上一题</el-button>
          <el-button @click="saveDraft" type="info" :icon="Document">
            暂存当前题目
          </el-button>
          <el-button
            @click="nextProblem"
            :disabled="currentProblemIndex === exam.problems.length - 1"
            type="primary"
          >
            下一题
          </el-button>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { Timer, Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import apiClient from '@/services/apiClient'

interface ChoiceOption {
  label: string
  text: string
  correct?: boolean
}

interface Problem {
  id: number
  status: 'UNANSWERED' | 'ANSWERED' | 'FLAGGED'
  score: number
  content: string
  type: 'CODING' | 'CHOICE' | 'FILL_BLANK'
  options?: ChoiceOption[]
  answer: string
}

const route = useRoute()
const examId = Number(route.params.id)

const exam = ref({
  id: examId,
  name: '',
  duration: 0,
  timeLeft: 0,
  isTaking: true,
  problems: [] as Problem[]
})

const currentProblemIndex = ref(0)
const isTaking = computed(() => exam.value.isTaking)

let timerId: number | null = null

const startCountdown = () => {
  if (timerId !== null) return
  timerId = window.setInterval(() => {
    if (exam.value.timeLeft > 0) {
      exam.value.timeLeft -= 1
    } else {
      exam.value.timeLeft = 0
      exam.value.isTaking = false
      if (timerId !== null) {
        clearInterval(timerId)
        timerId = null
      }
    }
  }, 1000)
}

const loadExamData = async () => {
  try {
    const [examRes, problemsRes] = await Promise.all([
      apiClient.get(`/exams/${examId}`),
      apiClient.get(`/exams/${examId}/problems`)
    ])

    const examData = examRes.data
    exam.value.name = examData.title
    exam.value.duration = examData.duration ?? 0

    // 优先使用后端 remainingTime（分钟），否则用 duration 作为总时长
    const remainingMinutes = examData.remainingTime ?? examData.duration ?? 0
    exam.value.timeLeft = Math.max(0, remainingMinutes * 60)

    const problemItems = problemsRes.data as Array<{
      problemId: number
      title: string
      description: string
      score: number
      sequence: number
      type?: 'CODING' | 'CHOICE' | 'FILL_BLANK'
      options?: string
    }>

    exam.value.problems = problemItems.map((p) => {
      let parsedOptions: ChoiceOption[] | undefined
      if (p.type === 'CHOICE' && p.options) {
        try {
          const raw = JSON.parse(p.options) as Array<{ label: string; text: string; correct?: boolean }>
          parsedOptions = raw.map((o) => ({
            label: o.label,
            text: o.text,
            correct: o.correct
          }))
        } catch {
          parsedOptions = undefined
        }
      }

      return {
        id: p.problemId,
        status: 'UNANSWERED' as const,
        score: p.score ?? 0,
        content: `<h3>${p.title}</h3><p>${p.description || ''}</p>`,
        type: p.type || 'CODING',
        options: parsedOptions,
        answer: ''
      }
    })

    if (exam.value.problems.length === 0) {
      ElMessage.info('本次考试尚未配置题目，请联系老师。')
    }

    if (exam.value.timeLeft > 0) {
      startCountdown()
    } else {
      exam.value.isTaking = false
    }
  } catch (error) {
    ElMessage.error('加载考试详情失败，请稍后重试')
  }
}

const currentAnswer = computed({
  get() {
    const currentProblem = exam.value.problems[currentProblemIndex.value]
    return currentProblem ? currentProblem.answer : ''
  },
  set(val: string) {
    const currentProblem = exam.value.problems[currentProblemIndex.value]
    if (!currentProblem) {
      return
    }
    currentProblem.answer = val
    if (val.trim() && currentProblem.status === 'UNANSWERED') {
      currentProblem.status = 'ANSWERED'
    }
  }
})

const formattedTimeLeft = computed(() => {
  const seconds = exam.value.timeLeft % 60
  const minutes = Math.floor(exam.value.timeLeft / 60) % 60
  const hours = Math.floor(exam.value.timeLeft / 3600)
  
  const pad = (num: number) => num.toString().padStart(2, '0')
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
})

const getProblemStatusType = (status: Problem['status']) => {
  switch (status) {
    case 'ANSWERED': return 'success'
    case 'FLAGGED': return 'danger'
    case 'UNANSWERED': 
    default: return 'info'
  }
}

const setCurrentProblem = (index: number) => {
  if (index >= 0 && index < exam.value.problems.length) {
    currentProblemIndex.value = index
  }
}
const prevProblem = () => {
  setCurrentProblem(currentProblemIndex.value - 1)
}
const nextProblem = () => {
  setCurrentProblem(currentProblemIndex.value + 1)
}

const saveDraft = async () => {
  const currentProblem = exam.value.problems[currentProblemIndex.value]
  if (!currentProblem) {
    ElMessage.warning('当前题目不存在')
    return
  }

  if (!currentProblem.answer || !currentProblem.answer.trim()) {
    ElMessage.warning('请先填写本题的答案或代码')
    return
  }

  try {
    await apiClient.post('/submissions', {
      problemId: currentProblem.id,
      examId: exam.value.id,
      code: currentProblem.answer,
      language: 'java'
    })

    ElMessage.success('当前答案已暂存，并已保存到服务器，稍后会自动评测并算分')
  } catch (error) {
    ElMessage.error('暂存失败：服务器保存答案时出错，请稍后重试')
  }
}

const submitExam = async () => {
  const unansweredCount = exam.value.problems.filter(p => p.status === 'UNANSWERED').length

  try {
    await ElMessageBox.confirm(
      unansweredCount > 0 
        ? `您还有 ${unansweredCount} 道题未作答，确定现在提交吗？` 
        : '您已完成所有题目，确定提交试卷吗？',
      '提交确认',
      {
        confirmButtonText: '确定提交',
        cancelButtonText: '继续作答',
        type: 'warning',
      }
    )
  } catch {
    ElMessage.info('已取消提交，请继续作答。')
    return
  }

  const answeredProblems = exam.value.problems.filter(
    (p) => p.answer && p.answer.trim()
  )

  if (answeredProblems.length === 0) {
    ElMessage.warning('您尚未作答任何题目，已取消提交。')
    return
  }

  try {
    await Promise.all(
      answeredProblems.map((p) =>
        apiClient.post('/submissions', {
          problemId: p.id,
          examId: exam.value.id,
          code: p.answer,
          language: 'java'
        })
      )
    )

    ElMessage.success(`试卷提交成功，已向服务器提交 ${answeredProblems.length} 道题。系统将自动评测并计算得分，您可在“提交记录”中查看结果。`)
    exam.value.isTaking = false
    if (timerId !== null) {
      clearInterval(timerId)
      timerId = null
    }
  } catch (error) {
    ElMessage.error('提交试卷失败：发送答案到服务器时出错，请稍后重试')
  }
}

onMounted(() => {
  loadExamData().catch(() => {})
})

onBeforeUnmount(() => {
  if (timerId !== null) {
    clearInterval(timerId)
    timerId = null
  }
})
</script>

<style scoped>
.exam-wrapper {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.exam-header {
  background-color: var(--el-color-primary);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}
.header-left {
  display: flex;
  align-items: center;
}
.exam-title {
  margin: 0;
  font-size: 1.5em;
}
.header-right {
  display: flex;
  align-items: center;
}
.timer-display {
  display: flex;
  align-items: center;
  padding: 5px 10px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

.exam-content {
  flex-grow: 1;
  overflow: hidden;
  height: calc(100vh - 60px);
}

.question-sidebar {
  padding: 15px;
  background-color: #f7f9fa;
  overflow-y: auto;
}
.sidebar-card {
  height: 100%;
}
.problem-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(40px, 1fr));
  gap: 10px;
  padding-top: 10px;
}

.problem-main {
  padding: 15px;
  overflow-y: auto;
}
.problem-statement-card {
  margin-bottom: 20px;
}
.problem-content {
  line-height: 1.8;
  padding: 10px 0;
}
.answer-card {
  margin-bottom: 20px;
}
.problem-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding-bottom: 20px;
}
</style>
