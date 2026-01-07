'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { authApi } from '@/lib/api/auth'
import { toast } from 'sonner'
import { Loader2, User, Mail, Lock } from 'lucide-react'
import { motion } from 'motion/react'
import { AuroraBackground } from '@/components/ui/aurora-background'

const registerSchema = z.object({
  username: z.string().min(2, '用户名至少 2 个字符'),
  email: z.string().email('请输入有效的邮箱地址').optional().or(z.literal('')),
  password: z.string().min(6, '密码至少 6 个字符'),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: "两次输入的密码不一致",
  path: ["confirmPassword"],
})

type RegisterFormValues = z.infer<typeof registerSchema>

export default function RegisterPage() {
  const router = useRouter()
  const [isLoading, setIsLoading] = useState(false)

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  })

  async function onSubmit(data: RegisterFormValues) {
    setIsLoading(true)
    try {
      await authApi.register({
        username: data.username,
        email: data.email || undefined,
        password: data.password,
      })
      toast.success('注册成功，请登录')
      router.push('/login')
    } catch (error: any) {
      console.error(error)
    } finally {
      setIsLoading(false)
    }
  }

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
        <div className="text-center mb-8 space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-foreground">
            创建账号
          </h1>
          <p className="text-sm text-muted-foreground font-light">
            开启您的创作之旅
          </p>
        </div>

        <div className="w-full">
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
                        <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground transition-colors group-focus-within:text-primary" />
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
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="sr-only">邮箱 (选填)</FormLabel>
                    <FormControl>
                      <div className="relative group">
                        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground transition-colors group-focus-within:text-primary" />
                        <Input 
                          type="email" 
                          placeholder="邮箱 (选填)" 
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
                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground transition-colors group-focus-within:text-primary" />
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
              <FormField
                control={form.control}
                name="confirmPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="sr-only">确认密码</FormLabel>
                    <FormControl>
                      <div className="relative group">
                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground transition-colors group-focus-within:text-primary" />
                        <Input 
                          type="password" 
                          placeholder="确认密码" 
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
                注册
              </Button>
            </form>
          </Form>
          
          <div className="mt-6 text-center text-sm">
            <span className="text-muted-foreground">已有账号？</span>{' '}
            <Link href="/login" className="font-medium text-primary hover:underline underline-offset-4">
              立即登录
            </Link>
          </div>
        </div>

        <div className="mt-8 text-center text-xs text-muted-foreground">
          &copy; {new Date().getFullYear()} do-write. All rights reserved.
        </div>
      </motion.div>
    </AuroraBackground>
  )
}
