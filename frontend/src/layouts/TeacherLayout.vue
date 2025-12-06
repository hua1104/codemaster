<template>
  <el-container class="teacher-layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo-container">
        <el-icon :size="26" style="margin-right: 8px;"><Notebook /></el-icon>
        <span class="system-title">教师工作台</span>
      </div>

      <el-menu
        :default-active="route.path"
        class="layout-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/teacher/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>概览</span>
        </el-menu-item>

        <el-sub-menu index="teacher-exams">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>考试管理</span>
          </template>
          <el-menu-item index="/teacher/exams">考试列表</el-menu-item>
          <el-menu-item index="/teacher/exam/create">新建考试</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="teacher-problems">
          <template #title>
            <el-icon><MessageBox /></el-icon>
            <span>题库管理</span>
          </template>
          <el-menu-item index="/teacher/problems">题目列表</el-menu-item>
          <el-menu-item index="/teacher/problem/create">新增题目</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/teacher/classes">
          <el-icon><UserFilled /></el-icon>
          <span>班级管理（预留）</span>
        </el-menu-item>

        <el-menu-item index="/teacher/scores">
          <el-icon><DataAnalysis /></el-icon>
          <span>成绩总览（预留）</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-breadcrumb">
          <span>{{ currentRouteName }}</span>
        </div>

        <div class="header-right">
          <el-tag type="warning" size="large">教师端</el-tag>
          <el-button
            type="danger"
            :icon="SwitchButton"
            link
            style="margin-left: 20px;"
            @click="handleLogout"
          >
            退出登录
          </el-button>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Notebook,
  Monitor,
  Document,
  MessageBox,
  UserFilled,
  DataAnalysis,
  SwitchButton
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const currentRouteName = computed(() => {
  return (route.meta.title as string) || route.path.split('/').pop() || 'Teacher Dashboard'
})

const handleLogout = () => {
  ElMessage.success('退出成功！')
  router.push('/auth/login')
}
</script>

<style scoped>
.teacher-layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: #304156;
  height: 100%;
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #1f2d3d;
}

.layout-menu {
  height: calc(100vh - 60px);
  border-right: none;
}

.layout-header {
  background-color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  border-bottom: 1px solid var(--el-border-color-light);
  padding: 0 20px;
}

.header-breadcrumb {
  font-size: 16px;
  color: #606266;
}

.header-right {
  display: flex;
  align-items: center;
}

.layout-main {
  background-color: var(--el-bg-color-page);
  padding: 20px;
  overflow-y: auto;
}
</style>

