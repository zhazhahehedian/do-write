'use client'

import { useState } from 'react'
import { useForm } from 'react-hook-form'
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
import { projectApi } from '@/lib/api/project'
import { toast } from 'sonner'
import { Loader2 } from 'lucide-react'

const projectCreateSchema = z.object({
  title: z.string().min(1, '请输入项目标题').max(50, '标题不能超过50个字符'),
  description: z.string().max(200, '描述不能超过200个字符').optional(),
  genre: z.string().optional(),
  targetWords: z.coerce.number().optional(),
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

  const form = useForm({
    resolver: zodResolver(projectCreateSchema),
    defaultValues: {
      title: '',
      description: '',
      genre: '',
      targetWords: 100000,
    },
  })

  async function onSubmit(data: ProjectCreateFormValues) {
    setIsLoading(true)
    try {
      await projectApi.create({
        ...data,
        targetWords: Number(data.targetWords),
      })
      toast.success('项目创建成功')
      onOpenChange(false)
      form.reset()
      onSuccess?.()
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
                      {...field}
                    />
                  </FormControl>
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
