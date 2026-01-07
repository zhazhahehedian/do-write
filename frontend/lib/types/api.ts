// 分页请求参数
export interface PageRequest {
  pageNum: number
  pageSize: number
  keyword?: string
  orderBy?: string
  order?: 'asc' | 'desc'
}

// 分页结果
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  list: T[]
}

// 统一响应格式
export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}
