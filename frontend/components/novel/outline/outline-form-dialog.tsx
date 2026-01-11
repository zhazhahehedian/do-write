'use client'

import { useState, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { outlineApi } from '@/lib/api/outline'
import type { Outline } from '@/lib/types/outline'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'

interface OutlineFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  projectId: string
  outline?: Outline | null
  nextOrderIndex?: number
  onSuccess?: () => void
}

interface OutlineFormData {
  orderIndex: string
  title: string
  content: string
}

const initialFormData: OutlineFormData = {
  orderIndex: '1',
  title: '',
  content: '',
}

export function OutlineFormDialog({
  open,
  onOpenChange,
  projectId,
  outline,
  nextOrderIndex = 1,
  onSuccess,
}: OutlineFormDialogProps) {
  const queryClient = useQueryClient()
  const isEditing = !!outline
  const [formData, setFormData] = useState<OutlineFormData>(initialFormData)

  useEffect(() => {
    if (open) {
      if (outline) {
        setFormData({
          orderIndex: outline.orderIndex?.toString() || '1',
          title: outline.title || '',
          content: outline.content || '',
        })
      } else {
        setFormData({
          ...initialFormData,
          orderIndex: nextOrderIndex.toString(),
        })
      }
    }
  }, [open, outline, nextOrderIndex])

  const createMutation = useMutation({
    mutationFn: (data: Partial<Outline>) => outlineApi.create(data),
    onSuccess: () => {
      toast.success('大纲创建成功')
      queryClient.invalidateQueries({ queryKey: ['outlines', projectId] })
      onOpenChange(false)
      onSuccess?.()
    },
    onError: (error: any) => {
      toast.error(error.message || '创建失败')
    },
  })

  const updateMutation = useMutation({
    mutationFn: (data: Partial<Outline>) =>
      outlineApi.update(outline!.id.toString(), data),
    onSuccess: () => {
      toast.success('大纲更新成功')
      queryClient.invalidateQueries({ queryKey: ['outlines', projectId] })
      onOpenChange(false)
      onSuccess?.()
    },
    onError: (error: any) => {
      toast.error(error.message || '更新失败')
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    if (!formData.title.trim()) {
      toast.error('请输入大纲标题')
      return
    }

    const data: Partial<Outline> = {
      projectId: Number(projectId),
      orderIndex: Number(formData.orderIndex) || 1,
      title: formData.title.trim(),
      content: formData.content.trim() || undefined,
    }

    if (isEditing) {
      updateMutation.mutate(data)
    } else {
      createMutation.mutate(data)
    }
  }

  const isPending = createMutation.isPending || updateMutation.isPending

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>{isEditing ? '编辑大纲' : '创建大纲'}</DialogTitle>
          <DialogDescription>
            {isEditing ? '修改大纲的内容' : '创建新的章节大纲'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-4 gap-4">
            <div className="space-y-2">
              <Label htmlFor="orderIndex">序号</Label>
              <Input
                id="orderIndex"
                type="number"
                value={formData.orderIndex}
                onChange={(e) => setFormData(prev => ({ ...prev, orderIndex: e.target.value }))}
                min={1}
                max={999}
              />
            </div>
            <div className="col-span-3 space-y-2">
              <Label htmlFor="title">标题 *</Label>
              <Input
                id="title"
                value={formData.title}
                onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
                placeholder="输入大纲标题"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="content">内容摘要</Label>
            <Textarea
              id="content"
              value={formData.content}
              onChange={(e) => setFormData(prev => ({ ...prev, content: e.target.value }))}
              placeholder="描述本章节的主要内容、情节发展、关键事件等"
              rows={6}
            />
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isPending}
            >
              取消
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {isEditing ? '保存' : '创建'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
