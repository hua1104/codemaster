export const endpoints = {
  auth: {
    login: '/auth/login',
    register: '/auth/register',
    profile: '/users/me',
    updateProfile: '/users/me',
    changePassword: '/users/me/change-password'
  },
  problems: {
    listMy: '/problems/my',
    listAll: '/problems',
    detail: (id: string | number) => `/problems/${id}`,
    create: '/problems',
    update: (id: string | number) => `/problems/${id}`,
    delete: (id: string | number) => `/problems/${id}`
  },
  admin: {
    // 保留占位，实际题目相关接口请使用 problems 模块
    problems: '/admin/problems',
    // 管理员 / 老师自己的考试列表，对应后端 ExamController.getMyExams: GET /api/exams/my
    exams: '/exams/my',
    statistics: '/admin/statistics',
    users: '/admin/users',
    recentActivities: '/admin/recent-activities'
  },
  exams: {
    list: '/exams/available',
    detail: (id: string | number) => `/exams/${id}`
  },
  submissions: {
    list: '/submissions',
    detail: (id: string | number) => `/submissions/${id}`
  },
  statistics: {
    student: '/statistics/student'
  },
  teacher: {
    dashboard: '/teacher/dashboard',
    classes: '/teacher/classes',
    scores: '/teacher/scores'
  }
}
