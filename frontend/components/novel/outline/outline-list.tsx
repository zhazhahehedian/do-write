'use client'

import type { Outline } from '@/lib/types/outline'
import { Button } from '@/components/ui/button'
import { Plus, List, Loader2 } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'

interface OutlineListProps {
  outlines: Outline[]
  isLoading?: boolean
  onAdd?: () => void
  onEdit?: (outline: Outline) => void
}

export function OutlineList({ 
  outlines, 
  isLoading, 
  onAdd, 
  onEdit 
}: OutlineListProps) {
  if (isLoading) {
    return (
      <div className="flex h-[400px] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (outlines.length === 0) {
    return (
      <div className="flex h-[400px] flex-col items-center justify-center gap-4 rounded-lg border border-dashed text-center">
        <div className="rounded-full bg-muted p-4">
          <List className="h-8 w-8 text-muted-foreground" />
        </div>
        <div>
          <h3 className="text-lg font-medium">暂无大纲</h3>
          <p className="text-sm text-muted-foreground">
            创建大纲以规划您的小说结构
          </p>
        </div>
        <Button onClick={onAdd}>
          <Plus className="mr-2 h-4 w-4" />
          创建大纲
        </Button>
      </div>
    )
  }

  // Sort by orderIndex
  const sortedOutlines = [...outlines].sort((a, b) => a.orderIndex - b.orderIndex)

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-end">
        <Button onClick={onAdd}>
          <Plus className="mr-2 h-4 w-4" />
          创建大纲
        </Button>
      </div>

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {sortedOutlines.map((outline) => (
          <Card 
            key={outline.id} 
            className="cursor-pointer hover:shadow-md transition-shadow"
            onClick={() => onEdit?.(outline)}
          >
            <CardHeader>
              <div className="flex justify-between items-start">
                <CardTitle className="line-clamp-1">{outline.title}</CardTitle>
                <div className="text-xs font-medium text-muted-foreground">
                  #{outline.orderIndex}
                </div>
              </div>
            </CardHeader>
            <CardContent>
               <div className="text-sm text-muted-foreground line-clamp-3">
                 {outline.content || '暂无内容设定'}
               </div>
               <div className="mt-4 text-xs text-muted-foreground">
                 更新于: {new Date(outline.createTime).toLocaleDateString()}
               </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}
