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
import { authApi } from '@/lib/api/auth'
import { useAuthStore } from '@/lib/store/auth-store'
import { toast } from 'sonner'
import { Loader2, SquareArrowUpRight, User, Lock } from 'lucide-react'

const loginSchema = z.object({
  username: z.string().min(1, '请输入用户名'),
  password: z.string().min(1, '请输入密码'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export function LoginForm({ className, ...props }: React.ComponentProps<"div">) {
  const router = useRouter()
  const setAuth = useAuthStore((state) => state.setAuth)
  const [isLoading, setIsLoading] = useState(false)

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

  const handleOAuthLogin = () => {
    toast.info('功能开发中', {
      description: 'Linux DO OAuth 登录即将上线...'
    })
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

      <Button
        variant="outline"
        className="w-full h-11 rounded-full border-muted-foreground/20 bg-transparent hover:bg-primary/5 hover:text-primary hover:border-primary/30 transition-all active:scale-95 duration-300"
        onClick={handleOAuthLogin}
      >
        <SquareArrowUpRight className="mr-2 h-4 w-4" />
        使用 Linux DO 登录
      </Button>

      <div className="text-center text-sm">
        <span className="text-muted-foreground">还没有账号？</span>{' '}
        <Link href="/register" className="font-medium text-primary hover:underline underline-offset-4">
          立即注册
        </Link>
      </div>
    </div>
  )
}
