'use client'

import type { Outline } from '@/lib/types/outline'
import { Button } from '@/components/ui/button'
import { Plus, List, Loader2, MoreVertical, Pencil, Trash2, FileText } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'

interface OutlineListProps {
  outlines: Outline[]
  isLoading?: boolean
  onAdd?: () => void
  onEdit?: (outline: Outline) => void
  onDelete?: (id: string) => void
  onGenerateChapter?: (outline: Outline) => void
}

export function OutlineList({
  outlines,
  isLoading,
  onAdd,
  onEdit,
  onDelete,
  onGenerateChapter,
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
            className="hover:shadow-md transition-shadow relative group"
          >
            <CardHeader>
              <div className="flex justify-between items-start">
                <div
                  className="flex-1 cursor-pointer"
                  onClick={() => onEdit?.(outline)}
                >
                  <div className="flex items-center gap-2">
                    <Badge variant="outline" className="text-xs">
                      第{outline.orderIndex}章
                    </Badge>
                  </div>
                  <CardTitle className="line-clamp-1 mt-2">{outline.title}</CardTitle>
                </div>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <MoreVertical className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => onGenerateChapter?.(outline)}>
                      <FileText className="mr-2 h-4 w-4" />
                      生成章节
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={() => onEdit?.(outline)}>
                      <Pencil className="mr-2 h-4 w-4" />
                      编辑
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      className="text-destructive focus:text-destructive"
                      onClick={() => onDelete?.(outline.id.toString())}
                    >
                      <Trash2 className="mr-2 h-4 w-4" />
                      删除
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </CardHeader>
            <CardContent
              className="cursor-pointer"
              onClick={() => onEdit?.(outline)}
            >
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
