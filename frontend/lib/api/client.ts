import axios, { AxiosError, AxiosInstance, AxiosRequestConfig } from 'axios'
import { toast } from 'sonner'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || '/api'
const API_TIMEOUT = 120000 // 2鍒嗛挓瓒呮椂

// 鍒涘缓 Axios 瀹炰緥
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
    // 浠?localStorage 鑾峰彇 token
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
    // 缁熶竴澶勭悊涓氬姟鍝嶅簲
    const { data } = response

    // 鍋囪鍚庣杩斿洖鏍煎紡: { code, message, data }
    if (data.code !== undefined && data.code !== 200) {
      const error = new Error(data.message || '璇锋眰澶辫触')
      return Promise.reject(error)
    }

    return response
  },
  (error: AxiosError<{ message?: string }>) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message

    // 閿欒鎻愮ず鏄犲皠锛堢敤浜庣敓鎴愭洿鍙嬪ソ鐨勯敊璇俊鎭級
    const errorMessages: Record<number, string> = {
      400: '请求参数错误',
      401: '登录已过期，请重新登录',
      403: '没有权限访问',
      404: '请求的资源不存在',
      422: '数据校验失败',
      500: '服务器内部错误',
      503: '服务暂时不可用',
    }

    const displayMessage = status
      ? errorMessages[status] || message
      : '缃戠粶杩炴帴澶辫触'

    // 灏嗗弸濂界殑閿欒淇℃伅闄勫姞鍒癳rror瀵硅薄涓婏紝鐢辫皟鐢ㄦ柟鍐冲畾鏄惁鏄剧ずtoast
    // 涓嶅湪杩欓噷鑷姩鏄剧ずtoast锛岄伩鍏嶄笌缁勪欢涓殑onError閲嶅鎻愮ず
    const enhancedError = new Error(displayMessage) as Error & { originalError: AxiosError }
    enhancedError.originalError = error

    // 401 璺宠浆鐧诲綍锛堣繖鏄敮涓€闇€瑕佸湪鎷︽埅鍣ㄤ腑澶勭悊鐨勬儏鍐碉級
    if (status === 401 && typeof window !== 'undefined') {
      toast.error(displayMessage)
      localStorage.removeItem('token')
      window.location.href = '/login'
    }

    return Promise.reject(enhancedError)
  }
)

// 閫氱敤璇锋眰鏂规硶
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await apiClient.request<{ code: number; message: string; data: T }>(config)
  return response.data.data
}


