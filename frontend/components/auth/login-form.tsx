'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { authApi, oauthApi } from '@/lib/api/auth'
import { useAuthStore } from '@/lib/store/auth-store'
import { toast } from 'sonner'
import { Loader2, User, Lock } from 'lucide-react'
import type { OAuthProvider } from '@/lib/types/auth'

// Linux.do Logo SVG
const LinuxDoIcon = () => (
  <svg viewBox="0 0 24 24" className="h-4 w-4 mr-2" fill="currentColor">
    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
  </svg>
)

// FishPi Logo SVG
const FishPiIcon = () => (
  <svg viewBox="0 0 24 24" className="h-4 w-4 mr-2" fill="currentColor">
    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zM8.65 14.35a.5.5 0 01-.7-.7l4-4a.5.5 0 01.7 0l4 4a.5.5 0 01-.7.7L12 10.41l-3.35 3.94z"/>
  </svg>
)

const loginSchema = z.object({
  username: z.string().min(1, '请输入用户名'),
  password: z.string().min(1, '请输入密码'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export function LoginForm({ className, ...props }: React.ComponentProps<"div">) {
  const router = useRouter()
  const setAuth = useAuthStore((state) => state.setAuth)
  const [isLoading, setIsLoading] = useState(false)

  const [oauthLoading, setOauthLoading] = useState<OAuthProvider | null>(null)

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: '',
      password: '',
    },
  })

  async function onSubmit(data: LoginFormValues) {
    setIsLoading(true)
    try {
      const response = await authApi.login(data)
      const token = response.token
      
      localStorage.setItem('token', token)

      const user = await authApi.getUserInfo()
      setAuth(user, token)

      toast.success('登录成功', {
        description: '欢迎回来！'
      })
      router.push('/home')
    } catch (error: unknown) {
      console.error(error)
      localStorage.removeItem('token')

      // 友好的错误提示
      const message = error instanceof Error ? error.message : '登录失败'
      const isNetworkError = message.includes('Network') || message.includes('网络')

      toast.error('登录失败', {
        description: isNetworkError
          ? '网络连接失败，请检查网络或确认后端服务已启动'
          : message,
        duration: 5000,
      })
    } finally {
      setIsLoading(false)
    }
  }

  const handleOAuthLogin = async (provider: OAuthProvider) => {
    setOauthLoading(provider)
    try {
      const response = await oauthApi.getAuthorizeUrl(provider)
      // 跳转到授权页面
      window.location.href = response.authorizeUrl
    } catch (error: unknown) {
      console.error('获取授权 URL 失败:', error)
      const message = error instanceof Error ? error.message : '获取授权链接失败'
      toast.error('OAuth 登录失败', {
        description: message,
      })
      setOauthLoading(null)
    }
  }

  return (
    <div className={cn("grid gap-6", className)} {...props}>
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          <FormField
            control={form.control}
            name="username"
            render={({ field }) => (
              <FormItem>
                <FormLabel className="sr-only">用户名</FormLabel>
                <FormControl>
                  <div className="relative group">
                    <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground transition-colors group-focus-within:text-primary z-10 pointer-events-none" />
                    <Input
                      placeholder="用户名"
                      className="h-11 pl-10 rounded-xl bg-background/50 backdrop-blur-sm border-muted-foreground/20 focus:border-primary/50 focus:bg-background/80 transition-all duration-300"
                      {...field}
                    />
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="password"
            render={({ field }) => (
              <FormItem>
                <FormLabel className="sr-only">密码</FormLabel>
                <FormControl>
                  <div className="relative group">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground transition-colors group-focus-within:text-primary z-10 pointer-events-none" />
                    <Input
                      type="password"
                      placeholder="密码"
                      className="h-11 pl-10 rounded-xl bg-background/50 backdrop-blur-sm border-muted-foreground/20 focus:border-primary/50 focus:bg-background/80 transition-all duration-300"
                      {...field}
                    />
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <Button
            type="submit"
            className="w-full h-11 rounded-full tracking-wide bg-primary hover:bg-primary/90 text-primary-foreground text-sm font-bold shadow-lg shadow-primary/20 hover:shadow-primary/30 transition-all active:scale-95 duration-300"
            disabled={isLoading}
          >
            {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {isLoading ? '登录中...' : '登录'}
          </Button>
        </form>
      </Form>

      <div className="flex items-center gap-3 text-xs text-muted-foreground uppercase">
        <div className="h-[1px] flex-1 bg-border/40" />
        <span>或</span>
        <div className="h-[1px] flex-1 bg-border/40" />
      </div>

      <div className="space-y-3">
        <Button
          variant="outline"
          className="w-full h-11 rounded-full border-muted-foreground/20 bg-transparent hover:bg-primary/5 hover:text-primary hover:border-primary/30 transition-all active:scale-95 duration-300"
          onClick={() => handleOAuthLogin('linuxdo')}
          disabled={oauthLoading !== null}
        >
          {oauthLoading === 'linuxdo' ? (
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          ) : (
            <LinuxDoIcon />
          )}
          使用 Linux DO 登录
        </Button>

        <Button
          variant="outline"
          className="w-full h-11 rounded-full border-muted-foreground/20 bg-transparent hover:bg-orange-500/5 hover:text-orange-600 hover:border-orange-500/30 transition-all active:scale-95 duration-300"
          onClick={() => handleOAuthLogin('fishpi')}
          disabled={oauthLoading !== null}
        >
          {oauthLoading === 'fishpi' ? (
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          ) : (
            <FishPiIcon />
          )}
          使用摸鱼派登录
        </Button>
      </div>

      <div className="text-center text-sm">
        <span className="text-muted-foreground">还没有账号？</span>{' '}
        <Link href="/register" className="font-medium text-primary hover:underline underline-offset-4">
          立即注册
        </Link>
      </div>
    </div>
  )
}
