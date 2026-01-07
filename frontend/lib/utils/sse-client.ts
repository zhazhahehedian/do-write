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

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
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

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const jsonStr = line.slice(5).trim()
            if (jsonStr === '[DONE]') {
              options.onComplete?.()
              return
            }

            try {
              const message: SSEMessage = JSON.parse(jsonStr)
              this.handleMessage(message, options)
            } catch (e) {
              console.error('Failed to parse SSE message:', e)
            }
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
