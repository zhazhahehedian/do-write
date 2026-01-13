'use client'

import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useRouter } from 'next/navigation'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { projectApi } from '@/lib/api/project'
import { useApiConfigCheck } from '@/hooks/use-api-config-check'
import { toast } from 'sonner'
import { Loader2 } from 'lucide-react'

// 叙事视角选项
const narrativePerspectives = [
  { value: 'first_person', label: '第一人称' },
  { value: 'third_person_limited', label: '第三人称限知视角' },
  { value: 'third_person_omniscient', label: '第三人称全知视角' },
  { value: 'multiple', label: '多视角' },
]

// 大纲模式选项
const outlineModes = [
  {
    value: 'one-to-one',
    label: '传统模式',
    description: '1个大纲节点 = 1个章节，适合短篇或已有清晰章节划分的故事'
  },
  {
    value: 'one-to-many',
    label: '细化模式',
    description: '1个大纲节点 = 多个章节，AI会将每个大纲展开为多个详细章节'
  },
]

const projectCreateSchema = z.object({
  title: z.string().min(1, '请输入项目标题').max(50, '标题不能超过50个字符'),
  description: z.string().max(500, '简介不能超过500个字符').optional(),
  genre: z.string().optional(),
  theme: z.string().max(100, '主题不能超过100个字符').optional(),
  narrativePerspective: z.string().optional(),
  targetWords: z.coerce.number().optional(),
  outlineMode: z.enum(['one-to-one', 'one-to-many']).default('one-to-one'),
})

type ProjectCreateFormValues = z.infer<typeof projectCreateSchema>

interface ProjectCreateDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess?: () => void
}

export function ProjectCreateDialog({
  open,
  onOpenChange,
  onSuccess,
}: ProjectCreateDialogProps) {
  const [isLoading, setIsLoading] = useState(false)
  const router = useRouter()
  const { checkAndRedirect } = useApiConfigCheck()

  const form = useForm({
    resolver: zodResolver(projectCreateSchema),
    defaultValues: {
      title: '',
      description: '',
      genre: '',
      theme: '',
      narrativePerspective: 'third_person_limited',
      targetWords: 100000,
      outlineMode: 'one-to-one' as const,
    },
  })

  async function onSubmit(data: ProjectCreateFormValues) {
    // 1. 检查 API 配置
    const hasConfig = checkAndRedirect()
    if (!hasConfig) return

    setIsLoading(true)
    try {
      // 2. 创建项目
      const result = await projectApi.create({
        ...data,
        targetWords: Number(data.targetWords),
      })

      toast.success('项目创建成功')
      onOpenChange(false)
      form.reset()

      // 3. 自动跳转到向导页面
      if (result) {
        router.push(`/project/wizard/${result}?step=world`)
      } else {
        onSuccess?.()
      }
    } catch (error: any) {
      console.error(error)
      // Error handled by api interceptor
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>创建新项目</DialogTitle>
          <DialogDescription>
            填写项目的基本信息以开始创作。
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="title"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>项目标题</FormLabel>
                  <FormControl>
                    <Input placeholder="输入小说标题" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="genre"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>类型</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：玄幻、都市、言情" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="theme"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>主题</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：修仙、重生、系统流" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="narrativePerspective"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>叙事视角</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="选择叙事视角" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {narrativePerspectives.map((perspective) => (
                        <SelectItem key={perspective.value} value={perspective.value}>
                          {perspective.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="outlineMode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>大纲模式</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="选择大纲模式" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {outlineModes.map((mode) => (
                        <SelectItem key={mode.value} value={mode.value}>
                          <div className="flex flex-col">
                            <span>{mode.label}</span>
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <p className="text-xs text-muted-foreground">
                    {outlineModes.find(m => m.value === field.value)?.description}
                  </p>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="targetWords"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>目标字数</FormLabel>
                  <FormControl>
                    <Input type="number" placeholder="预计总字数" {...field} value={field.value as number | string} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>简介</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="简要描述故事梗概"
                      className="resize-none"
                      maxLength={500}
                      {...field}
                    />
                  </FormControl>
                  <p className="text-xs text-muted-foreground text-right">
                    {field.value?.length || 0}/500
                  </p>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="submit" disabled={isLoading}>
                {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                创建
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
