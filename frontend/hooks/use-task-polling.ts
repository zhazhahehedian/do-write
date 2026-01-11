'use client'

import { useState, useEffect, useCallback, useRef } from 'react'
import { wizardApi } from '@/lib/api/wizard'
import type { GenerationTask, TaskStatus } from '@/lib/types/wizard'

interface UseTaskPollingOptions {
  /** 轮询间隔（毫秒），默认 2000ms */
  interval?: number
  /** 任务完成时的回调 */
  onComplete?: (task: GenerationTask) => void
  /** 任务失败时的回调 */
  onError?: (task: GenerationTask) => void
  /** 进度更新时的回调 */
  onProgress?: (progress: number, currentStep: string) => void
}

interface UseTaskPollingResult {
  /** 当前任务信息 */
  task: GenerationTask | null
  /** 是否正在轮询 */
  isPolling: boolean
  /** 开始轮询任务 */
  startPolling: (taskId: string) => void
  /** 停止轮询 */
  stopPolling: () => void
  /** 错误信息 */
  error: string | null
}

const TERMINAL_STATUSES: TaskStatus[] = ['completed', 'failed', 'cancelled']

export function useTaskPolling(options: UseTaskPollingOptions = {}): UseTaskPollingResult {
  const {
    interval = 2000,
    onComplete,
    onError,
    onProgress,
  } = options

  const [task, setTask] = useState<GenerationTask | null>(null)
  const [isPolling, setIsPolling] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const taskIdRef = useRef<string | null>(null)
  const intervalRef = useRef<NodeJS.Timeout | null>(null)

  const stopPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current)
      intervalRef.current = null
    }
    setIsPolling(false)
    taskIdRef.current = null
  }, [])

  const pollTask = useCallback(async (taskId: string) => {
    try {
      const taskData = await wizardApi.getTask(taskId)
      setTask(taskData)
      setError(null)

      // 触发进度回调
      onProgress?.(taskData.progress, taskData.currentStep)

      // 检查是否是终止状态
      if (TERMINAL_STATUSES.includes(taskData.status)) {
        stopPolling()

        if (taskData.status === 'completed') {
          onComplete?.(taskData)
        } else if (taskData.status === 'failed') {
          onError?.(taskData)
        }
      }
    } catch (err: any) {
      setError(err.message || '获取任务状态失败')
      // 网络错误不停止轮询，继续尝试
    }
  }, [onComplete, onError, onProgress, stopPolling])

  const startPolling = useCallback((taskId: string) => {
    // 如果已经在轮询同一个任务，不重复启动
    if (taskIdRef.current === taskId && isPolling) {
      return
    }

    // 停止之前的轮询
    stopPolling()

    taskIdRef.current = taskId
    setIsPolling(true)
    setError(null)

    // 立即执行一次
    pollTask(taskId)

    // 设置定时轮询
    intervalRef.current = setInterval(() => {
      if (taskIdRef.current) {
        pollTask(taskIdRef.current)
      }
    }, interval)
  }, [interval, isPolling, pollTask, stopPolling])

  // 组件卸载时清理
  useEffect(() => {
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
      }
    }
  }, [])

  return {
    task,
    isPolling,
    startPolling,
    stopPolling,
    error,
  }
}
