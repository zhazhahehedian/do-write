'use client'

import { useEffect, useState, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { motion } from 'motion/react'
import { AuroraBackground } from '@/components/ui/aurora-background'
import { Loader2, CheckCircle2, XCircle } from 'lucide-react'
import { authApi } from '@/lib/api/auth'
import { useAuthStore } from '@/lib/store/auth-store'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import Link from 'next/link'

type CallbackStatus = 'loading' | 'success' | 'error'

function OAuthCallbackContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const setAuth = useAuthStore((state) => state.setAuth)
  const [status, setStatus] = useState<CallbackStatus>('loading')
  const [errorMessage, setErrorMessage] = useState<string>('')

  useEffect(() => {
    const handleCallback = async () => {
      try {
        // 从 URL 获取 token（后端回调会返回 token）
        // 注意：实际的 token 是后端处理 OAuth 回调后返回的
        // 这里我们需要调用后端的回调接口

        // 获取当前完整 URL 的查询参数
        const fullUrl = window.location.href
        const url = new URL(fullUrl)

        // 从路径中获取 provider（如果有的话）
        // URL 格式可能是 /oauth/callback?... 或者后端重定向过来的

        // 检查是否有 data 参数（后端可能直接返回 token）
        const token = searchParams.get('token')

        if (token) {
          // 直接使用 token
          localStorage.setItem('token', token)

          // 获取用户信息
          const user = await authApi.getUserInfo()
          setAuth(user, token)

          setStatus('success')
          toast.success('登录成功', {
            description: '欢迎回来！'
          })

          // 延迟跳转，让用户看到成功状态
          setTimeout(() => {
            router.push('/home')
          }, 1500)
        } else {
          // 检查是否有错误信息
          const error = searchParams.get('error')
          if (error) {
            throw new Error(decodeURIComponent(error))
          }

          // 如果没有 token，可能需要等待后端处理
          // 或者这是一个错误的回调
          throw new Error('未能获取登录凭证，请重试')
        }
      } catch (error: unknown) {
        console.error('OAuth 回调处理失败:', error)
        setStatus('error')
        const message = error instanceof Error ? error.message : '登录失败，请重试'
        setErrorMessage(message)
        toast.error('登录失败', {
          description: message,
        })
      }
    }

    handleCallback()
  }, [searchParams, router, setAuth])

  return (
    <AuroraBackground>
      <motion.div
        initial={{ opacity: 0, y: 40 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{
          delay: 0.3,
          duration: 0.8,
          ease: "easeInOut",
        }}
        className="relative z-10 w-full max-w-sm px-4"
      >
        <div className="text-center space-y-6">
          <h1 className="text-3xl font-bold tracking-tight text-foreground">
            do-write <span className="font-serif italic text-primary">Novel</span>
          </h1>

          <div className="py-8">
            {status === 'loading' && (
              <div className="flex flex-col items-center gap-4">
                <Loader2 className="h-12 w-12 animate-spin text-primary" />
                <p className="text-muted-foreground">正在处理登录...</p>
              </div>
            )}

            {status === 'success' && (
              <div className="flex flex-col items-center gap-4">
                <CheckCircle2 className="h-12 w-12 text-green-500" />
                <p className="text-foreground font-medium">登录成功！</p>
                <p className="text-muted-foreground text-sm">正在跳转...</p>
              </div>
            )}

            {status === 'error' && (
              <div className="flex flex-col items-center gap-4">
                <XCircle className="h-12 w-12 text-destructive" />
                <p className="text-foreground font-medium">登录失败</p>
                <p className="text-muted-foreground text-sm">{errorMessage}</p>
                <Button asChild className="mt-4">
                  <Link href="/login">返回登录</Link>
                </Button>
              </div>
            )}
          </div>
        </div>

        <div className="mt-8 text-center text-xs text-muted-foreground">
          &copy; {new Date().getFullYear()} do-write. All rights reserved.
        </div>
      </motion.div>
    </AuroraBackground>
  )
}

export default function OAuthCallbackPage() {
  return (
    <Suspense fallback={
      <AuroraBackground>
        <div className="flex items-center justify-center">
          <Loader2 className="h-12 w-12 animate-spin text-primary" />
        </div>
      </AuroraBackground>
    }>
      <OAuthCallbackContent />
    </Suspense>
  )
}
