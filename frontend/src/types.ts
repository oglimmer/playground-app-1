export type Role = 'USER' | 'ADMIN'
export type UserStatus = 'PENDING' | 'APPROVED'

export interface User {
  id: string
  email: string
  displayName: string
  role: Role
  status: UserStatus
  createdAt: string
}

export interface PageSummary {
  slug: string
  title: string
  tags: string[]
  updatedAt: string
  updatedBy: string
}

export interface Page {
  slug: string
  title: string
  content: string
  tags: string[]
  createdAt: string
  updatedAt: string
  updatedBy: string
}

export interface SavePageRequest {
  title: string
  content: string
  tags: string[]
}
