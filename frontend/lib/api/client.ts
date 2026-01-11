import axios, { AxiosError, AxiosInstance, AxiosRequestConfig } from 'axios'
import { toast } from 'sonner'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || '/api'
const API_TIMEOUT = 120000 // 2分钟超时

// 创建 Axios 实例
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取 token
    const token = typeof window !== 'undefined'
      ? localStorage.getItem('token')
      : null

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    // 统一处理业务响应
    const { data } = response

    // 假设后端返回格式: { code, message, data }
    if (data.code !== undefined && data.code !== 200) {
      const error = new Error(data.message || '请求失败')
      return Promise.reject(error)
    }

    return response
  },
  (error: AxiosError<{ message?: string }>) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message

    // 错误提示映射（用于生成更友好的错误信息）
    const errorMessages: Record<number, string> = {
      400: '请求参数错误',
      401: '登录已过期，请重新登录',
      403: '没有权限访问',
      404: '请求的资源不存在',
      422: '数据验证失败',
      500: '服务器内部错误',
      503: '服务暂时不可用',
    }

    const displayMessage = status
      ? errorMessages[status] || message
      : '网络连接失败'

    // 将友好的错误信息附加到error对象上，由调用方决定是否显示toast
    // 不在这里自动显示toast，避免与组件中的onError重复提示
    const enhancedError = new Error(displayMessage) as Error & { originalError: AxiosError }
    enhancedError.originalError = error

    // 401 跳转登录（这是唯一需要在拦截器中处理的情况）
    if (status === 401 && typeof window !== 'undefined') {
      toast.error(displayMessage)
      localStorage.removeItem('token')
      window.location.href = '/login'
    }

    return Promise.reject(enhancedError)
  }
)

// 通用请求方法
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await apiClient.request<{ code: number; message: string; data: T }>(config)
  return response.data.data
}
