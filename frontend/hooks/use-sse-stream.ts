'use client'

import { useState, useCallback, useRef } from 'react'
import { SSEPostClient, type SSEOptions } from '@/lib/utils/sse-client'

interface UseSSEStreamOptions<T> extends SSEOptions<T> {
  onStart?: () => void
}

interface UseSSEStreamReturn<T> {
  isStreaming: boolean
  progress: number
  message: string
  content: string
  wordCount: number
  error: string | null
  result: T | null
  startStream: (url: string, data: Record<string, any>) => Promise<void>
  abort: () => void
  reset: () => void
}

export function useSSEStream<T = any>(
  options?: UseSSEStreamOptions<T>
): UseSSEStreamReturn<T> {
  const [isStreaming, setIsStreaming] = useState(false)
  const [progress, setProgress] = useState(0)
  const [message, setMessage] = useState('')
  const [content, setContent] = useState('')
  const [wordCount, setWordCount] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<T | null>(null)

  const clientRef = useRef<SSEPostClient | null>(null)

  const reset = useCallback(() => {
    setIsStreaming(false)
    setProgress(0)
    setMessage('')
    setContent('')
    setWordCount(0)
    setError(null)
    setResult(null)
  }, [])

  const abort = useCallback(() => {
    clientRef.current?.abort()
    setIsStreaming(false)
  }, [])

  const startStream = useCallback(async (
    url: string,
    data: Record<string, any>
  ) => {
    reset()
    setIsStreaming(true)
    options?.onStart?.()

    clientRef.current = new SSEPostClient()

    await clientRef.current.stream<T>(url, data, {
      onProgress: (msg, prog, wc) => {
        setMessage(msg)
        setProgress(prog)
        if (wc !== undefined) setWordCount(wc)
        options?.onProgress?.(msg, prog, wc)
      },
      onChunk: (chunk) => {
        setContent(prev => prev + chunk)
        options?.onChunk?.(chunk)
      },
      onResult: (data) => {
        setResult(data)
        options?.onResult?.(data)
      },
      onError: (err, code) => {
        setError(err)
        setIsStreaming(false)
        options?.onError?.(err, code)
      },
      onComplete: () => {
        setIsStreaming(false)
        setProgress(100)
        options?.onComplete?.()
      },
    })
  }, [options, reset])

  return {
    isStreaming,
    progress,
    message,
    content,
    wordCount,
    error,
    result,
    startStream,
    abort,
    reset,
  }
}
