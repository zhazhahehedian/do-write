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
    let completed = false
    let errored = false

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

            // 服务端声明为 error 事件时，禁止落入“正常完成”分支
            if (currentEvent === 'error') {
              errored = true
            }

            // 处理 done 事件或 [DONE] 标记
            if (currentEvent === 'done' || jsonStr === '[DONE]') {
              completed = true
              options.onComplete?.()
              return
            }

            try {
              const message: SSEMessage = JSON.parse(jsonStr)

              // 错误事件不应继续走“完成”分支（避免误提示成功）
              if (message.type === 'error') {
                errored = true
                this.handleMessage(message, options)
                return
              }

              // 兼容：服务端用 message.type=done 表达完成
              if (message.type === 'done') {
                completed = true
                this.handleMessage(message, options)
                return
              }

              this.handleMessage(message, options)
            } catch (e) {
              // 如果解析失败，可能是纯文本 chunk（兼容旧格式）
              // 直接作为 chunk 内容处理
              if (currentEvent === 'error') {
                options.onError?.(jsonStr)
                return
              }
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

      // 只有在未完成、未报错的情况下，才把“正常断流”视作完成（兼容旧后端无 done 事件）
      if (!completed && !errored) {
        options.onComplete?.()
      }
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
