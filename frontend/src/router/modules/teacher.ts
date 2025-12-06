import type { RouteRecordRaw } from 'vue-router'
import TeacherLayout from '@/layouts/TeacherLayout.vue'

const teacherRoutes: RouteRecordRaw[] = [
  {
    path: '/teacher',
    component: TeacherLayout,
    redirect: '/teacher/dashboard',
    meta: { requiresAuth: true, role: 'TEACHER' },
    children: [
      {
        path: 'dashboard',
        name: 'TeacherDashboard',
        component: () => import('@/views/teacher/TeacherDashboardView.vue'),
        meta: { title: '教师概览' }
      },
      {
        path: 'exams',
        name: 'TeacherExamList',
        component: () => import('@/views/admin/AdminExamListView.vue'),
        meta: { title: '考试列表' }
      },
      {
        path: 'exam/create',
        name: 'TeacherExamCreate',
        component: () => import('@/views/admin/AdminExamFormView.vue'),
        meta: { title: '创建考试' }
      },
      {
        path: 'exam/:id',
        name: 'TeacherExamEdit',
        component: () => import('@/views/admin/AdminExamFormView.vue'),
        props: true,
        meta: { title: '编辑考试' }
      },
      {
        path: 'problems',
        name: 'TeacherProblemList',
        component: () => import('@/views/admin/AdminProblemListView.vue'),
        meta: { title: '题目列表' }
      },
      {
        path: 'problem/create',
        name: 'TeacherProblemCreate',
        component: () => import('@/views/admin/AdminProblemFormView.vue'),
        meta: { title: '创建题目' }
      },
      {
        path: 'problem/:id',
        name: 'TeacherProblemEdit',
        component: () => import('@/views/admin/AdminProblemFormView.vue'),
        props: true,
        meta: { title: '编辑题目' }
      },
      {
        path: 'classes',
        name: 'TeacherClassList',
        component: () => import('@/views/teacher/TeacherClassListView.vue'),
        meta: { title: '班级管理' }
      },
      {
        path: 'scores',
        name: 'TeacherScoreOverview',
        component: () => import('@/views/teacher/TeacherScoreOverviewView.vue'),
        meta: { title: '成绩总览' }
      }
    ]
  }
]

export default teacherRoutes

