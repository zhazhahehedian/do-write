export interface SSEMessage {
  type: 'progress' | 'chunk' | 'result' | 'error' | 'done'
  message?: string
  progress?: number
  word_count?: number
  content?: string
  data?: any
  error?: string
  code?: number
}

export interface SSEOptions<T = any> {
  onProgress?: (message: string, progress: number, wordCount?: number) => void
  onChunk?: (content: string) => void
  onResult?: (data: T) => void
  onError?: (error: string, code?: number) => void
  onComplete?: () => void
}

/**
 * SSE POST 客户端 - 用于需要 POST 请求的流式响应
 */
export class SSEPostClient {
  private abortController: AbortController | null = null

  async stream<T = any>(
    url: string,
    data: Record<string, any>,
    options: SSEOptions<T>
  ): Promise<void> {
    this.abortController = new AbortController()

    // 从 localStorage 获取 token
    const token = typeof window !== 'undefined'
      ? localStorage.getItem('token')
      : null

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
    }

    // 添加 Bearer token 到请求头
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(data),
        signal: this.abortController.signal,
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('No response body')
      }

      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = 'message' // 默认事件类型

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          // 解析事件类型
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
            continue
          }

          if (line.startsWith('data:')) {
            const jsonStr = line.slice(5).trim()

            // 跳过空数据
            if (!jsonStr || jsonStr === '') {
              continue
            }

            // 处理 done 事件或 [DONE] 标记
            if (currentEvent === 'done' || jsonStr === '[DONE]') {
              options.onComplete?.()
              return
            }

            try {
              const message: SSEMessage = JSON.parse(jsonStr)
              this.handleMessage(message, options)
            } catch (e) {
              // 如果解析失败，可能是纯文本 chunk（兼容旧格式）
              // 直接作为 chunk 内容处理
              if (jsonStr.length > 0 && !jsonStr.startsWith('{')) {
                options.onChunk?.(jsonStr)
              } else {
                console.error('Failed to parse SSE message:', e, 'raw:', jsonStr)
              }
            }

            // 重置事件类型为默认值
            currentEvent = 'message'
          }
        }
      }

      options.onComplete?.()
    } catch (error: any) {
      if (error.name === 'AbortError') {
        console.log('SSE request aborted')
        return
      }
      options.onError?.(error.message || 'Unknown error')
    }
  }

  private handleMessage<T>(message: SSEMessage, options: SSEOptions<T>) {
    switch (message.type) {
      case 'progress':
        options.onProgress?.(
          message.message || '',
          message.progress || 0,
          message.word_count
        )
        break
      case 'chunk':
        if (message.content) {
          options.onChunk?.(message.content)
        }
        break
      case 'result':
        options.onResult?.(message.data)
        break
      case 'error':
        options.onError?.(message.error || 'Unknown error', message.code)
        break
      case 'done':
        options.onComplete?.()
        break
    }
  }

  abort() {
    this.abortController?.abort()
  }
}

/**
 * 简化的 SSE POST 函数
 */
export async function ssePost<T = any>(
  url: string,
  data: Record<string, any>,
  options: SSEOptions<T>
): Promise<void> {
  const client = new SSEPostClient()
  return client.stream(url, data, options)
}
