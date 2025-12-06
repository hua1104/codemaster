<template>
  <div class="login-view-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>Code Examner 登录</h2>
          <p class="subtitle">欢迎使用在线考试系统</p>
        </div>
      </template>

      <el-form
        :model="loginForm"
        :rules="loginRules"
        ref="loginFormRef"
        @keyup.enter="handleLogin"
        label-position="top"
      >
        <el-form-item label="用户名或邮箱" prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入用户名或邮箱"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            :loading="loading" 
            @click="handleLogin"
            style="width: 100%; margin-top: 10px;"
            size="large"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer-links">
        <el-button type="info" link @click="goToRegister">
          还没有账号？立即注册
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { User, Lock } from '@element-plus/icons-vue';
import { useAuthStore } from '@/store/auth';
import type { LoginPayload } from '@/types/auth';

const router = useRouter();
const authStore = useAuthStore();

// --- 状态与表单 ---
const loading = ref(false);
const loginFormRef = ref<FormInstance>();

const loginForm = reactive<LoginPayload>({
  username: '',
  password: ''
});

const loginRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' },
    { min: 3, max: 50, message: '长度应在 3 到 50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为 6 位', trigger: 'blur' }
  ]
});

// --- 登录逻辑 ---
const handleLogin = async () => {
  if (!loginFormRef.value) return;

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        // 调用实际后端登录接口，保存 token
        await authStore.login(loginForm);

        // 根据角色跳转到不同仪表盘
        if (authStore.role === 'ADMIN') {
          ElMessage.success('管理员登录成功！');
          router.push('/admin/dashboard');
        } else if (authStore.role === 'TEACHER') {
          ElMessage.success('教师登录成功！');
          router.push('/teacher/dashboard');
        } else {
          ElMessage.success('登录成功！');
          router.push('/dashboard');
        }
      } catch (error) {
        ElMessage.error('登录失败，请检查用户名或密码。');
      } finally {
        loading.value = false;
      }
    }
  });
};

// --- 路由跳转 ---
const goToRegister = () => {
  router.push('/auth/register');
};
</script>

<style scoped>
.login-view-container {
  /* 继承自 AuthLayout.vue 的居中样式 */
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
}

.login-card {
  width: 100%;
  max-width: 400px; /* 限制卡片最大宽度 */
  padding: 10px 20px 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  text-align: center;
  margin-bottom: 20px;
}

.card-header h2 {
  font-size: 24px;
  color: var(--el-color-primary);
  margin: 0;
}

.subtitle {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-top: 5px;
}

.footer-links {
  text-align: center;
  margin-top: 15px;
}
</style>
