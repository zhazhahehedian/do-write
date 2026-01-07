'use client'

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Progress } from '@/components/ui/progress'
import { Button } from '@/components/ui/button'
import { Loader2, X } from 'lucide-react'

interface SSEProgressModalProps {
  open: boolean
  title: string
  message: string
  progress: number
  wordCount?: number
  onCancel?: () => void
  showCancel?: boolean
}

export function SSEProgressModal({
  open,
  title,
  message,
  progress,
  wordCount,
  onCancel,
  showCancel = true,
}: SSEProgressModalProps) {
  return (
    <Dialog open={open}>
      <DialogContent className="sm:max-w-md" showCloseButton={showCancel}>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Loader2 className="h-5 w-5 animate-spin text-primary" />
            {title}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* 进度条 */}
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">{message}</span>
              <span className="font-medium">{Math.round(progress)}%</span>
            </div>
            <Progress value={progress} className="h-2" />
          </div>

          {/* 字数统计 */}
          {wordCount !== undefined && wordCount > 0 && (
            <div className="text-sm text-muted-foreground">
              已生成 <span className="font-medium text-foreground">{wordCount.toLocaleString()}</span> 字
            </div>
          )}

          {/* 取消按钮 */}
          {showCancel && onCancel && (
            <div className="flex justify-end">
              <Button variant="outline" size="sm" onClick={onCancel}>
                <X className="mr-2 h-4 w-4" />
                取消
              </Button>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
