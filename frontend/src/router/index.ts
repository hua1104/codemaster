import { createRouter, createWebHistory } from 'vue-router'
import authRoutes from './modules/auth'
import adminRoutes from './modules/admin'
import studentRoutes from './modules/student'
import teacherRoutes from './modules/teacher'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/auth/login'
    },
    ...authRoutes,
    ...adminRoutes,
    ...teacherRoutes,
    ...studentRoutes,
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/common/NotFoundView.vue')
    }
  ]
})

export default router

