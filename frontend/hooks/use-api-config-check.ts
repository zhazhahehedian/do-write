'use client'

import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { userConfigApi } from '@/lib/api/user-config'
import { useAuthStore } from '@/lib/store/auth-store'
import { toast } from 'sonner'
import { useCallback } from 'react'

/**
 * API 配置检查 Hook
 * 用于检查用户是否配置了默认的 AI API 配置
 * 在创建项目、进入向导等需要 AI 功能的场景使用
 */
export function useApiConfigCheck() {
  const router = useRouter()
  const { user } = useAuthStore()

  const { data: configs, isLoading, refetch } = useQuery({
    queryKey: ['api-configs', user?.id],
    queryFn: () => userConfigApi.list(String(user?.id || '')),
    enabled: !!user?.id,
    staleTime: 5 * 60 * 1000, // 5分钟内不重新请求
  })

  // 检查是否有默认配置
  const hasDefaultConfig = configs?.some(c => c.isDefault === 1) ?? false

  // 检查是否有任何配置
  const hasAnyConfig = (configs?.length ?? 0) > 0

  /**
   * 检查 API 配置，如果没有配置则提示并跳转
   * @param onSuccess 检查通过后的回调
   * @returns 是否有默认配置
   */
  const checkAndRedirect = useCallback((onSuccess?: () => void): boolean => {
    if (isLoading) return false

    if (!hasDefaultConfig) {
      toast.error('请先配置 AI 模型', {
        description: hasAnyConfig
          ? '请将一个 API 配置设置为默认配置'
          : '使用 AI 功能前需要先配置至少一个 API 配置',
        action: {
          label: '去配置',
          onClick: () => router.push('/settings/api-config'),
        },
        duration: 5000,
      })
      return false
    }

    onSuccess?.()
    return true
  }, [isLoading, hasDefaultConfig, hasAnyConfig, router])

  /**
   * 跳转到 API 配置页面
   */
  const goToApiConfig = useCallback(() => {
    router.push('/settings/api-config')
  }, [router])

  return {
    configs,
    hasDefaultConfig,
    hasAnyConfig,
    isLoading,
    checkAndRedirect,
    goToApiConfig,
    refetch,
  }
}
