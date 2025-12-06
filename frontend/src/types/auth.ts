export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  email: string
  studentId?: string
  realName?: string
  className?: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
}

export interface UserProfile {
  id: number
  username: string
  email: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
  studentId?: string
  realName?: string
  className?: string
}

