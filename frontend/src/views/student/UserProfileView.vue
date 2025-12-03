<template>
  <div class="user-profile-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>个人资料与设置</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="profile-tabs">
        <el-tab-pane label="基本信息" name="info">
          <div class="info-content" v-if="profile">
            <el-form label-width="100px" class="user-info-form">
              <el-form-item label="头像">
                <el-avatar :size="80" :icon="UserFilled" />
              </el-form-item>

              <el-form-item label="用户名">
                <el-input :model-value="profile.username" disabled />
                <span class="info-tip">用户名不可修改。</span>
              </el-form-item>

              <el-form-item label="邮箱">
                <el-input :model-value="profile.email" disabled />
                <span class="info-tip">邮箱用于找回密码，目前不可修改。</span>
              </el-form-item>

              <template v-if="profile.role === 'STUDENT'">
                <el-form-item label="学号">
                  <el-input v-model="studentProfileForm.studentId" placeholder="请输入学号" />
                </el-form-item>

                <el-form-item label="姓名">
                  <el-input v-model="studentProfileForm.realName" placeholder="请输入姓名" />
                </el-form-item>

                <el-form-item label="班级">
                  <el-input v-model="studentProfileForm.className" placeholder="请输入班级" />
                </el-form-item>

                <el-form-item>
                  <el-button
                    type="primary"
                    :loading="saveProfileLoading"
                    @click="handleSaveProfile"
                  >
                    保存个人信息
                  </el-button>
                </el-form-item>
              </template>
            </el-form>
          </div>
          <div v-else style="padding: 20px; color: var(--el-text-color-secondary);">
            正在加载个人信息...
          </div>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <div class="password-content">
            <el-form
              :model="passwordForm"
              :rules="passwordRules"
              ref="passwordFormRef"
              label-width="120px"
              class="password-form"
            >
              <el-form-item label="旧密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password />
              </el-form-item>

              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password />
              </el-form-item>

              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
              </el-form-item>

              <el-form-item>
                <el-button
                  type="danger"
                  :loading="passwordLoading"
                  @click="handleChangePassword"
                >
                  确认修改（示例）
                </el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import apiClient from '@/services/apiClient'
import { endpoints } from '@/services/endpoints'

const activeTab = ref('info')
const passwordLoading = ref(false)
const saveProfileLoading = ref(false)

const authStore = useAuthStore()
const profile = computed(() => authStore.profile)

const studentProfileForm = reactive({
  studentId: '',
  realName: '',
  className: ''
})

const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = reactive<FormRules>({
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度至少为 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
})

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      passwordLoading.value = true
      try {
        await apiClient.post(endpoints.auth.changePassword, {
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
        ElMessage.success('密码修改成功，请使用新密码重新登录')
        resetPasswordForm()
      } finally {
        passwordLoading.value = false
      }
    }
  })
}

const resetPasswordForm = () => {
  if (passwordFormRef.value) {
    passwordFormRef.value.resetFields()
  }
}

const syncStudentProfileForm = () => {
  if (profile.value && profile.value.role === 'STUDENT') {
    studentProfileForm.studentId = profile.value.studentId || ''
    studentProfileForm.realName = profile.value.realName || ''
    studentProfileForm.className = profile.value.className || ''
  }
}

const handleSaveProfile = async () => {
  if (!profile.value || profile.value.role !== 'STUDENT') {
    return
  }

  if (!studentProfileForm.realName.trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }

  saveProfileLoading.value = true
  try {
    await apiClient.put(endpoints.auth.updateProfile, {
      studentId: studentProfileForm.studentId,
      realName: studentProfileForm.realName,
      className: studentProfileForm.className
    })

    await authStore.fetchProfile()
    syncStudentProfileForm()
    ElMessage.success('个人信息已保存')
  } catch (error) {
    ElMessage.error('保存个人信息失败，请稍后重试')
  } finally {
    saveProfileLoading.value = false
  }
}

onMounted(async () => {
  if (!authStore.profile) {
    await authStore.fetchProfile()
  }
  syncStudentProfileForm()
})
</script>

<style scoped>
.profile-tabs {
  margin-top: 10px;
}

.user-info-form {
  max-width: 500px;
  padding: 10px 0;
}

.password-form {
  max-width: 450px;
  padding: 10px 0;
}

.info-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-left: 10px;
}
</style>
